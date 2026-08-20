-- ---------------------------------------------------------------------------
-- Snapshot the freshly-populated `public` schema into a parallel `seed` schema.
--
-- DemoService restores the sandbox by truncating each public table and copying its
-- rows back from here. Keeping the pristine copy in the same database is what makes
-- a reset a fast, in-process operation with no restart and no external dump file.
--
-- Run this AFTER demo-entities.sql and after any history import, so `seed` captures
-- the complete starting state.
--
-- Two exclusions:
--   qrtz_*   Quartz owns these while the scheduler is running; truncating them out
--            from under a live scheduler corrupts its state.
--   partition children (port_values_2024, event_log_p*, ...) are skipped: copying a
--            partitioned parent already captures every row, and restoring through the
--            parent routes them back into the right partition. Including both would
--            duplicate every partitioned row on restore.
-- ---------------------------------------------------------------------------

\set ON_ERROR_STOP on

DROP SCHEMA IF EXISTS seed CASCADE;
CREATE SCHEMA seed;

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT c.relname
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE n.nspname = 'public'
           AND c.relkind IN ('r', 'p')     -- ordinary + partitioned parents
           AND NOT c.relispartition        -- but never a partition child
           AND c.relname NOT LIKE 'qrtz\_%'
    LOOP
        EXECUTE format('CREATE TABLE seed.%I AS SELECT * FROM public.%I', t, t);
    END LOOP;
END
$$;

-- The age of this snapshot. DemoService shifts every restored timestamp by
-- (now() - built_at), so a seed built months ago still produces charts that end
-- "now" instead of trailing off into a flat line.
CREATE TABLE seed.seed_meta (built_at timestamp without time zone NOT NULL);
INSERT INTO seed.seed_meta (built_at) VALUES (now());

-- A restore that finds no seed tables would silently empty the demo, so fail loudly
-- here instead - this is the point where the mistake is still cheap.
DO $$
DECLARE
    n int;
BEGIN
    SELECT count(*) INTO n FROM pg_tables WHERE schemaname = 'seed';
    IF n < 5 THEN
        RAISE EXCEPTION 'seed schema has only % table(s) - demo-entities.sql probably did not run', n;
    END IF;
    RAISE NOTICE 'seed schema captured % tables', n;
END
$$;
