package org.myhab.services

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import com.hazelcast.core.HazelcastInstance
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.init.cache.CacheMap

import javax.sql.DataSource
import java.sql.Connection
import java.util.concurrent.atomic.AtomicLong

/**
 * Restores the public demo's sandbox to its seeded state.
 *
 * All visitors share one dataset, so "your changes disappear" is implemented by
 * resetting: on idle, nightly, and on demand from the demo banner. Three things make
 * this cheap enough to do without restarting anything:
 *
 * <ul>
 *   <li>Hibernate's second-level cache is off (application.yml), so a restore done at
 *       the database level is immediately visible to the running application.</li>
 *   <li>The seeded rows are kept in a parallel {@code seed} schema, so the restore is
 *       a truncate-and-copy that needs no knowledge of the schema and does not have to
 *       be updated when a table is added.</li>
 *   <li>The simulator republishes retained MQTT state on request, so device state
 *       lines up with the restored rows.</li>
 * </ul>
 *
 * Safety: this truncates application tables, so it refuses to run anywhere that is not
 * the demo environment AND does not have a {@code seed} schema. Production has neither.
 */
@Slf4j
class DemoService {

    static transactional = false

    // Typed on purpose: these autowire BY TYPE. `def wSocketsService` in particular
    // silently stays null - Grails names that bean `WSocketsService` (a leading
    // two-capital class keeps its case), so by-name injection never matches.
    DataSource dataSource
    HazelcastInstance hazelcastInstance
    WSocketsService wSocketsService
    MqttTopicService mqttTopicService
    def grailsApplication

    /** Epoch millis of the last mutating request; 0 means untouched since the last reset. */
    private final AtomicLong lastMutationAt = new AtomicLong(0L)
    private final AtomicLong lastResetAt = new AtomicLong(System.currentTimeMillis())

    boolean isDemo() {
        return Environment.current.name == 'demo'
    }

    /**
     * True when the environment is the demo AND the seed schema exists. Both are
     * required: the flag alone must never be enough to truncate a real database.
     */
    boolean isResetAvailable() {
        if (!isDemo()) return false
        try {
            withSql { Sql sql ->
                sql.firstRow("SELECT 1 FROM information_schema.schemata WHERE schema_name = 'seed'") != null
            }
        } catch (Exception e) {
            log.error("Could not determine whether the seed schema exists: ${e.message}", e)
            return false
        }
    }

    void recordMutation() {
        if (isDemo()) lastMutationAt.set(System.currentTimeMillis())
    }

    boolean isDirty() {
        return lastMutationAt.get() > 0
    }

    /** Minutes since the last mutating request, or -1 if the sandbox is untouched. */
    long idleMinutes() {
        long last = lastMutationAt.get()
        if (last == 0L) return -1
        return (System.currentTimeMillis() - last) / 60000L
    }

    long lastReset() {
        return lastResetAt.get()
    }

    /**
     * Restore the sandbox. Returns true if a reset actually ran.
     */
    boolean reset(String reason) {
        if (!isResetAvailable()) {
            log.warn("Refusing to reset: not the demo environment, or no seed schema present")
            return false
        }

        long started = System.currentTimeMillis()
        log.info("Resetting demo sandbox (${reason})")

        try {
            restoreFromSeed()
            clearCaches()
            republishDeviceState()

            lastMutationAt.set(0L)
            lastResetAt.set(System.currentTimeMillis())

            // Open tabs are showing rows that no longer exist; tell them to refetch
            // rather than leaving stale ids on screen until the next navigation.
            notifyClients()

            log.info("Demo sandbox reset in ${System.currentTimeMillis() - started}ms")
            return true
        } catch (Exception e) {
            log.error("Demo reset failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Truncate every table that the seed schema mirrors and copy the seeded rows back.
     *
     * Deliberately generic: it iterates whatever is in the seed schema rather than
     * naming tables, so adding a domain class does not silently leave a table
     * un-restored. Foreign keys are deferred via session_replication_role instead of
     * ordering the truncates.
     *
     * History timestamps are rebased by (now - seed built_at) so the charts always end
     * "now" and the seed never has to be rebuilt to stay plausible.
     */
    private void restoreFromSeed() {
        withSql { Sql sql ->
            sql.execute("SET session_replication_role = replica" as String)
            try {
                sql.execute("""
                    DO \$\$
                    DECLARE
                        t text;
                        tables text;
                    BEGIN
                        -- Truncate everything in one statement, with CASCADE.
                        -- session_replication_role only defers FK *triggers*; TRUNCATE
                        -- still refuses to touch a table that something references
                        -- unless the referencing tables go with it. CASCADE here only
                        -- reaches partition children, which are restored through their
                        -- parent anyway.
                        SELECT string_agg(format('public.%I', tablename), ', ')
                          INTO tables
                          FROM pg_tables
                         WHERE schemaname = 'seed'
                           AND tablename <> 'seed_meta';

                        IF tables IS NULL THEN
                            RAISE EXCEPTION 'seed schema is empty - refusing to wipe the database';
                        END IF;

                        EXECUTE format('TRUNCATE %s CASCADE', tables);

                        FOR t IN
                            SELECT tablename FROM pg_tables
                             WHERE schemaname = 'seed'
                               AND tablename <> 'seed_meta'
                        LOOP
                            EXECUTE format('INSERT INTO public.%I SELECT * FROM seed.%I', t, t);
                        END LOOP;
                    END
                    \$\$;
                """ as String)

                rebaseHistory(sql)
                resyncSequences(sql)
            } finally {
                sql.execute("SET session_replication_role = DEFAULT" as String)
            }
        }
    }

    /**
     * Shift every seeded timestamp forward by the age of the seed, so a dataset built
     * months ago still renders a full, current window in the solar and heating charts.
     */
    private void rebaseHistory(Sql sql) {
        def meta = sql.firstRow("SELECT built_at FROM seed.seed_meta LIMIT 1")
        if (!meta?.built_at) {
            log.warn("seed.seed_meta has no built_at - skipping timestamp rebasing")
            return
        }

        sql.execute("""
            DO \$\$
            DECLARE
                shift interval;
                r record;
            BEGIN
                SELECT now() - built_at INTO shift FROM seed.seed_meta LIMIT 1;
                IF shift IS NULL OR shift <= interval '0' THEN
                    RETURN;
                END IF;
                FOR r IN
                -- Partition children must be excluded: they appear in
                -- information_schema.columns alongside their parent, so shifting both
                -- would apply the offset twice to every partitioned row. Updating the
                -- parent already reaches them, and re-routes rows across partitions
                -- when the partition key itself moves.
                    SELECT c.relname AS table_name, a.attname AS column_name
                      FROM pg_class c
                      JOIN pg_namespace n ON n.oid = c.relnamespace
                      JOIN pg_attribute a ON a.attrelid = c.oid
                      JOIN pg_type ty ON ty.oid = a.atttypid
                     WHERE n.nspname = 'public'
                       AND c.relkind IN ('r', 'p')
                       AND NOT c.relispartition
                       AND a.attnum > 0
                       AND NOT a.attisdropped
                       AND ty.typname IN ('timestamp', 'timestamptz')
                       -- Explicit ESCAPE rather than the default backslash: this SQL
                       -- lives in a Groovy GString, which rejects that escape.
                       AND c.relname NOT LIKE 'qrtz@_%' ESCAPE '@'
                LOOP
                    EXECUTE format('UPDATE public.%I SET %I = %I + %L WHERE %I IS NOT NULL',
                                   r.table_name, r.column_name, r.column_name, shift, r.column_name);
                END LOOP;
            END
            \$\$;
        """ as String)
    }

    /**
     * Push every sequence past the restored rows. Without this the first insert after
     * a reset collides with a seeded id.
     */
    private void resyncSequences(Sql sql) {
        sql.execute("""
            DO \$\$
            DECLARE
                maxid bigint;
            BEGIN
                SELECT GREATEST(
                    COALESCE((SELECT MAX(id) FROM public.zones), 0),
                    COALESCE((SELECT MAX(id) FROM public.device_controllers), 0),
                    COALESCE((SELECT MAX(id) FROM public.device_ports), 0),
                    COALESCE((SELECT MAX(id) FROM public.device_peripherals), 0),
                    COALESCE((SELECT MAX(id) FROM public.device_peripherals_categories), 0),
                    COALESCE((SELECT MAX(id) FROM public.configurations), 0),
                    COALESCE((SELECT MAX(id) FROM public.users), 0),
                    10000
                ) INTO maxid;
                PERFORM setval('hibernate_sequence', maxid + 1, false);
            END
            \$\$;
        """ as String)
    }

    /**
     * Drop the short-lived correlation state. These key off port ids that have just
     * been replaced, so leaving them would apply a pending action or an auto-off
     * deadline to a row the visitor never touched.
     */
    private void clearCaches() {
        [CacheMap.PENDING_ACTION, CacheMap.EXPIRE].each { CacheMap map ->
            try {
                hazelcastInstance?.getMap(map.name)?.clear()
            } catch (Exception e) {
                log.warn("Could not clear cache '${map.name}': ${e.message}")
            }
        }
    }

    /**
     * Ask the simulator to republish retained state and device status. Without this the
     * broker still holds whatever the visitor last switched, and the next reconnect
     * would reinstate it over the restored rows.
     */
    private void republishDeviceState() {
        try {
            String prefix = grailsApplication.config.getProperty('mqtt.topic.prefix', String, 'myhab')
            mqttTopicService.publishRaw("${prefix}/_demo/reset" as String, 'reset')
        } catch (Exception e) {
            log.warn("Could not signal the simulator to republish state: ${e.message}")
        }
    }

    private void notifyClients() {
        try {
            wSocketsService?.broadcastDemoReset()
        } catch (Exception e) {
            log.warn("Could not broadcast the demo reset: ${e.message}")
        }
    }

    /** Runs against the app's own datasource; the reset is pure SQL, not GORM. */
    private <T> T withSql(Closure<T> body) {
        Connection connection = dataSource.connection
        Sql sql = new Sql(connection)
        try {
            return body(sql)
        } finally {
            sql.close()
        }
    }
}
