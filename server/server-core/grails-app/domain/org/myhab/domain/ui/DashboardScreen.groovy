package org.myhab.domain.ui

import grails.gorm.DetachedCriteria
import graphql.schema.DataFetchingEnvironment
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher
import org.myhab.domain.common.BaseEntity

/**
 * A dashboard screen for the mobile/wall UI (/wui): an uploaded floor-plan
 * background image plus a JSON list of peripheral widgets (markers, polygon
 * zones, text readouts) placed over it in the in-app editor.
 *
 * <p>Replaces the legacy convention of hand-drawn SVG files with peripheral
 * bindings encoded in element ids ({@code asset-<category>-id-<id>-<n>}).</p>
 *
 * <p>Background image bytes deliberately live in {@link DashboardScreenBackground}
 * (FK on that side) so list/get queries never hydrate megabytes; they are
 * served via REST ({@code /api/screens/$id/background}), not GraphQL.</p>
 *
 * <p>{@code layoutJson} schema (version 1):</p>
 * <pre>
 * { "version": 1, "widgets": [
 *   {"id":"<uuid>","kind":"marker","peripheralId":2524,"x":431.5,"y":210.2,"size":28,"icon":null},
 *   {"id":"<uuid>","kind":"zone","peripheralId":257551,"points":[[100,120],[260,120],[260,300]]},
 *   {"id":"<uuid>","kind":"text","peripheralId":6872,"x":300,"y":80,"fontSize":20},
 *   {"id":"<uuid>","kind":"link","x":40,"y":40,"size":36,"icon":"mdi-monitor","label":"Web UI","href":"/"}
 * ]}
 * </pre>
 * {@code link} widgets are navigation controls (no peripheral binding): the
 * viewer routes to {@code href} (in-app path or absolute URL) on tap.
 * Coordinates are in background-image natural pixel space (viewBox =
 * {@code 0 0 backgroundWidth backgroundHeight}). Widget category (and thus
 * visual/action) is derived at runtime from the peripheral — never stored.
 */
@Slf4j
class DashboardScreen extends BaseEntity {

    static final List<String> WIDGET_KINDS = ['marker', 'zone', 'text', 'link'].asImmutable()

    String name
    Integer ordinal = 0
    Boolean enabled = true
    String layoutJson
    /** Metadata of the uploaded background (bytes live in DashboardScreenBackground). */
    String backgroundContentType
    Integer backgroundWidth
    Integer backgroundHeight

    /**
     * Image bytes as a hasOne association: FK stays on the background table
     * and the association is lazy, so list queries never hydrate the bytes —
     * while deletes cascade from the screen. Excluded from GraphQL below.
     */
    static hasOne = [background: DashboardScreenBackground]

    transient wSocketsService
    static transients = ['wSocketsService']

    static constraints = {
        name blank: false, unique: true
        layoutJson nullable: true
        backgroundContentType nullable: true
        backgroundWidth nullable: true
        backgroundHeight nullable: true
        background nullable: true
    }

    static mapping = {
        table '`dashboard_screens`'
        layoutJson type: 'text'
        sort ordinal: 'asc'
        autowire true
    }

    def afterInsert() { broadcastChanged() }

    def afterUpdate() { broadcastChanged() }

    def afterDelete() { broadcastChanged() }

    private void broadcastChanged() {
        // Direct broadcast (same style as WSocketsService.broadcastRawMqtt) so
        // every write path — auto CRUD, saveLayout, reorder, REST background
        // upload (touches metadata) — notifies open viewers to refetch.
        try {
            wSocketsService?.broadcastDashboardScreenChanged(id)
        } catch (Exception e) {
            log.warn("Failed to broadcast dashboard screen change for ${id}: ${e.message}")
        }
    }

    /**
     * Validate a layoutJson document. Returns null when valid, otherwise a
     * human-readable error string.
     */
    static String validateLayoutJson(String json) {
        def doc
        try {
            doc = new JsonSlurper().parseText(json)
        } catch (Exception e) {
            return "Invalid JSON: ${e.message}"
        }
        if (!(doc instanceof Map)) return 'Layout must be a JSON object'
        if (doc.version != 1) return "Unsupported layout version: ${doc.version}"
        if (!(doc.widgets instanceof List)) return 'Layout must contain a "widgets" array'
        for (def w : doc.widgets) {
            if (!(w instanceof Map)) return 'Each widget must be an object'
            if (!(w.kind in WIDGET_KINDS)) return "Unknown widget kind: ${w.kind}"
            if (w.kind == 'link') {
                // navigation control — no peripheral binding, needs a target
                if (!(w.x instanceof Number) || !(w.y instanceof Number)) {
                    return "Widget ${w.id}: x/y must be numeric"
                }
                if (!(w.href instanceof String) || !w.href.trim()) {
                    return "Widget ${w.id}: link needs a non-empty href"
                }
                continue
            }
            if (!(w.peripheralId instanceof Number)) return "Widget ${w.id}: peripheralId must be numeric"
            if (w.kind in ['marker', 'text']) {
                if (!(w.x instanceof Number) || !(w.y instanceof Number)) {
                    return "Widget ${w.id}: x/y must be numeric"
                }
            } else { // zone
                if (!(w.points instanceof List) || w.points.size() < 3
                        || !w.points.every { it instanceof List && it.size() == 2 && it.every { c -> c instanceof Number } }) {
                    return "Widget ${w.id}: zone needs >= 3 [x,y] points"
                }
            }
        }
        return null
    }

    static graphql = GraphQLMapping.lazy {
        // Never expose the image bytes through GraphQL — they are streamed by
        // DashboardScreenController (/api/screens/$id/background).
        exclude('background')

        // Ordered list for both the viewer (enabledOnly:true) and the editor.
        query('dashboardScreens', [DashboardScreen]) {
            argument('enabledOnly', Boolean) { nullable true }
            dataFetcher(new EntityDataFetcher<DashboardScreen>(DashboardScreen.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    def criteria = environment.getArgument('enabledOnly')
                            ? DashboardScreen.where { enabled == true }
                            : DashboardScreen.where { }
                    criteria.order('ordinal', 'asc')
                }
            })
        }

        // Whole-document layout save from the editor, with server-side validation.
        mutation('dashboardScreenSaveLayout', DashboardScreen) {
            argument('id', Long)
            argument('layoutJson', String)
            returns DashboardScreen
            dataFetcher { DataFetchingEnvironment env ->
                DashboardScreen screen = DashboardScreen.get(env.getArgument('id') as Long)
                if (!screen) {
                    throw new RuntimeException("Dashboard screen not found: ${env.getArgument('id')}")
                }
                String json = env.getArgument('layoutJson') as String
                String error = validateLayoutJson(json)
                if (error) {
                    throw new RuntimeException("Invalid layout: ${error}")
                }
                DashboardScreen.withTransaction {
                    screen.layoutJson = json
                    screen.save(flush: true, failOnError: true)
                }
                return screen
            }
        }

        // Rewrite ordinals to match the given id order (editor up/down arrows).
        mutation('dashboardScreenReorder', [DashboardScreen]) {
            argument('ids', [Long])
            dataFetcher { DataFetchingEnvironment env ->
                List<Long> ids = (env.getArgument('ids') as List).collect { it as Long }
                DashboardScreen.withTransaction {
                    ids.eachWithIndex { Long screenId, int index ->
                        DashboardScreen screen = DashboardScreen.get(screenId)
                        if (screen && screen.ordinal != index) {
                            screen.ordinal = index
                            screen.save(flush: true, failOnError: true)
                        }
                    }
                }
                return DashboardScreen.list(sort: 'ordinal', order: 'asc')
            }
        }
    }
}
