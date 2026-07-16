package org.myhab.services.dashboard

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import org.myhab.domain.ui.DashboardScreen
import org.myhab.domain.ui.DashboardScreenBackground
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class DashboardScreenImportServiceSpec extends Specification
        implements ServiceUnitTest<DashboardScreenImportService>, DataTest {

    private static final int IMG_W = 200
    private static final int IMG_H = 100

    void setupSpec() {
        mockDomains(DashboardScreen, DashboardScreenBackground)
    }

    /**
     * Fixture mimicking the legacy screen-1.svg structure: viewBox 0 0 780 500,
     * embedded base64 image placed at (-40,-80) sized 820x630 in viewBox space,
     * asset-* overlay shapes with the legacy id convention (incl. door_lock
     * with underscore), plus a nav button that must be ignored.
     */
    private static String fixtureSvg() {
        BufferedImage img = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_INT_RGB)
        def out = new ByteArrayOutputStream()
        ImageIO.write(img, 'png', out)
        String b64 = Base64.encoder.encodeToString(out.toByteArray())
        return """<svg viewBox="0 0 780 500" id="screen-t" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
            <image id="image-1" x="-40" y="-80" width="820" height="630" xlink:href="data:image/png;base64,${b64}"></image>
            <rect id="asset-light-id-2524-1" x="100" y="120" width="20" height="20"/>
            <circle id="asset-light-id-2524-2" cx="200" cy="200" r="10"/>
            <path id="asset-heat-id-257551-1" d="M 300 100 L 400 100 L 400 180 L 300 180 Z"/>
            <text id="asset-temp-id-6872-1" x="500" y="50">21C</text>
            <g id="asset-door_lock-id-3599655-1">
                <circle cx="600" cy="300" r="15"/>
                <path d="M 590 290 L 610 310"/>
            </g>
            <polyline id="asset-light-id-9999-1" points="10,10 50,10 50,14"/>
            <circle id="nav-home-1" cx="39" cy="20" r="31"/>
        </svg>"""
    }

    void "imports a legacy SVG into a disabled screen with seeded widgets"() {
        when:
            DashboardScreen screen = service.importLegacySvg('Test screen', fixtureSvg().bytes)

        then: 'screen metadata extracted from the embedded image'
            screen.id != null
            !screen.enabled
            screen.backgroundContentType == 'image/png'
            screen.backgroundWidth == IMG_W
            screen.backgroundHeight == IMG_H

        and: 'background bytes stored separately and decodable'
            def background = DashboardScreenBackground.findByScreen(screen)
            background != null
            ImageIO.read(new ByteArrayInputStream(background.data)).width == IMG_W

        and: 'one widget per legacy asset shape, nav ignored'
            def layout = new JsonSlurper().parseText(screen.layoutJson)
            layout.version == 1
            layout.widgets.size() == 6

        and: 'kinds derived from SHAPE: circle+g -> marker, rect/simple path/polyline -> zone, temp -> text'
            layout.widgets.count { it.kind == 'marker' } == 2 // circle light + door_lock group (underscore id!)
            layout.widgets.count { it.kind == 'zone' } == 3   // rect light + simple heat path + LED-strip polyline
            layout.widgets.count { it.kind == 'text' } == 1   // temp

        and: 'peripheral ids parsed from ids'
            layout.widgets.collect { it.peripheralId as long }.sort().unique() == [2524L, 6872L, 9999L, 257551L, 3599655L].sort()

        and: 'LED-strip polyline keeps its exact 3 vertices'
            layout.widgets.find { (it.peripheralId as long) == 9999L }.points.size() == 3

        and: 'circle light becomes a marker at its center, sized from its radius'
            // circle light: viewBox center (200,200), r=10; image rect (-40,-80,820,630)
            // -> px: (200+40)/820*200 = 58.5 ; (200+80)/630*100 = 44.4 ; size clamped to >= 16
            def circleMarker = layout.widgets.find { it.kind == 'marker' && (it.peripheralId as long) == 2524L }
            Math.abs((circleMarker.x as double) - 58.5d) < 0.11d
            Math.abs((circleMarker.y as double) - 44.4d) < 0.11d
            (circleMarker.size as double) >= 16d

        and: 'rect light becomes a 4-corner zone in px space'
            def rectZone = layout.widgets.find { it.kind == 'zone' && (it.peripheralId as long) == 2524L }
            rectZone.points.size() == 4
            // rect corner (100,120) -> px: (100+40)/820*200 = 34.1 ; (120+80)/630*100 = 31.7
            Math.abs((rectZone.points[0][0] as double) - 34.1d) < 0.11d
            Math.abs((rectZone.points[0][1] as double) - 31.7d) < 0.11d

        and: 'simple heat path keeps its vertices as a zone (not a bbox)'
            def heatZone = layout.widgets.find { it.kind == 'zone' && (it.peripheralId as long) == 257551L }
            heatZone.points.size() >= 3

        and: 'produced layout passes the domain validator'
            DashboardScreen.validateLayoutJson(screen.layoutJson) == null
    }

    void "deleting a screen cascades to its background row"() {
        given:
            DashboardScreen screen = service.importLegacySvg('To delete', fixtureSvg().bytes)
            assert DashboardScreenBackground.findByScreen(screen) != null

        when:
            screen.delete(flush: true)

        then:
            DashboardScreen.get(screen.id) == null
            DashboardScreenBackground.count() == old(DashboardScreenBackground.count()) - 1
    }

    void "rejects an SVG without an embedded image"() {
        when:
            service.importLegacySvg('Broken', '<svg viewBox="0 0 10 10"><rect id="asset-light-id-1-1" x="1" y="1" width="2" height="2"/></svg>'.bytes)

        then:
            thrown(IllegalArgumentException)
    }

    void "layout validator rejects malformed documents"() {
        expect:
            DashboardScreen.validateLayoutJson(json) != null

        where:
            json << [
                    'not json',
                    '{"version":2,"widgets":[]}',
                    '{"version":1}',
                    '{"version":1,"widgets":[{"kind":"marker","peripheralId":1}]}',           // missing x/y
                    '{"version":1,"widgets":[{"kind":"zone","peripheralId":1,"points":[[1,2],[3,4]]}]}', // <3 points
                    '{"version":1,"widgets":[{"kind":"blob","peripheralId":1,"x":1,"y":2}]}', // unknown kind
                    '{"version":1,"widgets":[{"kind":"link","x":1,"y":2}]}',                  // link without href
                    '{"version":1,"widgets":[{"kind":"link","x":1,"y":2,"href":"  "}]}',      // link with blank href
                    '{"version":1,"widgets":[{"kind":"link","href":"/"}]}',                   // link without x/y
            ]
    }

    void "layout validator accepts a valid document"() {
        expect:
            DashboardScreen.validateLayoutJson(
                    '{"version":1,"widgets":[' +
                            '{"id":"a","kind":"marker","peripheralId":1,"x":10,"y":20,"size":28},' +
                            '{"id":"b","kind":"zone","peripheralId":2,"points":[[0,0],[10,0],[10,10]]},' +
                            '{"id":"c","kind":"text","peripheralId":3,"x":5,"y":6,"fontSize":20},' +
                            '{"id":"d","kind":"link","x":40,"y":40,"size":36,"icon":"mdi-monitor","label":"Web UI","href":"/"}]}'
            ) == null
    }
}
