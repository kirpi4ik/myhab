package org.myhab.services.dashboard

import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.myhab.domain.ui.DashboardScreen
import org.myhab.domain.ui.DashboardScreenBackground

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * One-shot importer for legacy /wui SVG screens (the hand-authored files with
 * an embedded base64 floor-plan image and peripheral bindings encoded in
 * element ids: {@code asset-<category>-id-<peripheralId>-<index>}).
 *
 * <p>Extracts the embedded raster into a {@link DashboardScreenBackground}
 * and seeds {@code layoutJson} widgets from the asset elements (bbox centers
 * for markers/text, bbox corners for heat zones). This is a seeding aid, not
 * a faithful conversion — imported screens are created {@code enabled:false}
 * so positions can be reviewed/adjusted in the editor before going live.</p>
 */
@Slf4j
@Transactional
class DashboardScreenImportService {

    /** Legacy binding id. NOTE [a-z_]: door_lock contains an underscore. */
    private static final Pattern ASSET_ID = Pattern.compile(/^asset-([a-z_]+)-id-(\d+)-(\d+)$/, Pattern.CASE_INSENSITIVE)
    private static final Pattern DATA_URI = Pattern.compile(/^data:(image\/[a-z+]+);base64,(.*)$/, Pattern.DOTALL)
    /** Numeric pairs inside a path "d" attribute — seeding-grade bbox extraction. */
    private static final Pattern NUMBER = Pattern.compile(/-?\d+(?:\.\d+)?/)

    /**
     * Parse a legacy SVG and create a disabled DashboardScreen with background
     * bytes and seeded widgets. Throws on structural problems (no viewBox, no
     * embedded image).
     */
    DashboardScreen importLegacySvg(String name, byte[] svgBytes) {
        def svg = new XmlSlurper().parseText(new String(svgBytes, 'UTF-8'))

        // --- viewBox ---
        def viewBox = (svg.@viewBox?.text() ?: '').trim().split(/[\s,]+/)
        if (viewBox.size() != 4) {
            throw new IllegalArgumentException('SVG has no usable root viewBox')
        }

        // --- embedded background image ---
        def imageNode = svg.depthFirst().find { node ->
            node.name() == 'image' && dataUriOf(node) != null
        }
        if (imageNode == null) {
            throw new IllegalArgumentException('SVG contains no embedded base64 <image> background')
        }
        Matcher uriMatcher = DATA_URI.matcher(dataUriOf(imageNode))
        uriMatcher.matches()
        String contentType = uriMatcher.group(1)
        byte[] imageBytes = Base64.decoder.decode(uriMatcher.group(2).replaceAll(/\s/, ''))
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes))
        if (image == null) {
            throw new IllegalArgumentException("Embedded background image is not decodable (${contentType})")
        }

        // Legacy image placement in viewBox space -> mapping into natural px space
        double imgX = attrD(imageNode, 'x', 0d)
        double imgY = attrD(imageNode, 'y', 0d)
        double imgW = attrD(imageNode, 'width', viewBox[2] as double)
        double imgH = attrD(imageNode, 'height', viewBox[3] as double)
        int naturalW = image.width
        int naturalH = image.height

        // --- seed widgets from asset-* elements ---
        // Widget kind is derived from the SHAPE, not the category: legacy
        // screens used circles as icons (heat markers, bulbs), simple paths/
        // polygons as areas (LED strips, room outlines) and complex curvy
        // paths as hand-drawn icons.
        List<Map> widgets = []
        Closure<List<Double>> toPx = { double x, double y ->
            [(x - imgX) / imgW * naturalW, (y - imgY) / imgH * naturalH]
        }
        double scaleX = naturalW / imgW

        svg.depthFirst().each { node ->
            String id = node.@id?.text()
            if (!id) return
            Matcher assetMatcher = ASSET_ID.matcher(id)
            if (!assetMatcher.matches()) return

            String category = assetMatcher.group(1).toLowerCase()
            long peripheralId = assetMatcher.group(2) as long
            double[] bbox = bboxOf(node) // [minX, minY, maxX, maxY] in viewBox space
            if (bbox == null) return

            double cx = (bbox[0] + bbox[2]) / 2d
            double cy = (bbox[1] + bbox[3]) / 2d

            Map widget = [id: UUID.randomUUID().toString(), peripheralId: peripheralId]
            String tag = node.name()

            Closure markerAtCenter = {
                widget.kind = 'marker'
                def (mx, my) = toPx(cx, cy)
                widget.x = round1(mx); widget.y = round1(my)
                // preserve the legacy icon footprint where derivable
                double extent = Math.max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * scaleX
                widget.size = round1(Math.min(Math.max(extent, 16d), 60d))
                widget.icon = null
            }
            Closure zoneWithPoints = { List<List<Double>> pts ->
                widget.kind = 'zone'
                widget.points = pts.collect { p ->
                    def mapped = toPx(p[0], p[1])
                    [round1(mapped[0]), round1(mapped[1])]
                }
            }
            Closure zoneFromBbox = {
                zoneWithPoints([
                        [bbox[0], bbox[1]], [bbox[2], bbox[1]],
                        [bbox[2], bbox[3]], [bbox[0], bbox[3]],
                ])
            }

            if (category in ['temp', 'luminosity']) {
                widget.kind = 'text'
                def (tx, ty) = toPx(cx, cy)
                widget.x = round1(tx); widget.y = round1(ty)
                widget.fontSize = 20
            } else if (tag in ['circle', 'ellipse'] || tag == 'g') {
                // circles/ellipses were icon markers (heat, bulbs); groups are
                // composed icons (door lock)
                markerAtCenter()
            } else if (tag in ['polygon', 'polyline']) {
                List<List<Double>> pts = numberPairs(node.@points?.text())
                pts.size() >= 3 ? zoneWithPoints(pts) : markerAtCenter()
            } else if (tag == 'rect') {
                zoneFromBbox()
            } else if (tag == 'path') {
                // simple paths (few coordinate pairs: LED strips, room
                // outlines) keep their vertices as a zone; complex curvy paths
                // are hand-drawn icons -> marker
                List<List<Double>> pts = numberPairs(node.@d?.text())
                if (pts.size() >= 3 && pts.size() <= 24) {
                    zoneWithPoints(pts)
                } else {
                    markerAtCenter()
                }
            } else {
                markerAtCenter()
            }
            widgets << widget
        }

        // --- persist ---
        Integer nextOrdinal = ((DashboardScreen.createCriteria().get {
            projections { max('ordinal') }
        } ?: -1) as Integer) + 1

        DashboardScreen screen = new DashboardScreen(
                name: name,
                ordinal: nextOrdinal,
                enabled: false, // review in the editor before showing on /wui
                layoutJson: JsonOutput.toJson([version: 1, widgets: widgets]),
                backgroundContentType: contentType,
                backgroundWidth: naturalW,
                backgroundHeight: naturalH,
        ).save(flush: true, failOnError: true)
        new DashboardScreenBackground(screen: screen, data: imageBytes).save(flush: true, failOnError: true)

        log.info("Imported legacy SVG '${name}': ${widgets.size()} widgets seeded, background ${naturalW}x${naturalH} ${contentType}")
        return screen
    }

    /** href / xlink:href data-URI of an <image> node, or null. */
    private static String dataUriOf(node) {
        String href = node.@href?.text() ?: node.'@xlink:href'?.text()
        (href?.startsWith('data:image/')) ? href : null
    }

    private static double attrD(node, String attr, double fallback) {
        String value = node."@${attr}"?.text()
        value ? (value as double) : fallback
    }

    private static double round1(double value) {
        Math.round(value * 10d) / 10d
    }

    /**
     * Approximate bounding box [minX, minY, maxX, maxY] of a shape element in
     * its local coordinates. Nested transforms are ignored on purpose —
     * seeding-grade accuracy, positions get adjusted in the editor.
     */
    private static double[] bboxOf(node) {
        switch (node.name()) {
            case 'rect':
                double x = attrD(node, 'x', 0d), y = attrD(node, 'y', 0d)
                return [x, y, x + attrD(node, 'width', 0d), y + attrD(node, 'height', 0d)] as double[]
            case 'circle':
                double cx = attrD(node, 'cx', 0d), cy = attrD(node, 'cy', 0d), r = attrD(node, 'r', 0d)
                return [cx - r, cy - r, cx + r, cy + r] as double[]
            case 'ellipse':
                double ecx = attrD(node, 'cx', 0d), ecy = attrD(node, 'cy', 0d)
                double rx = attrD(node, 'rx', 0d), ry = attrD(node, 'ry', 0d)
                return [ecx - rx, ecy - ry, ecx + rx, ecy + ry] as double[]
            case 'text':
                double tx = attrD(node, 'x', 0d), ty = attrD(node, 'y', 0d)
                return [tx, ty, tx, ty] as double[]
            case 'polygon':
            case 'polyline':
                return bboxOfNumbers(node.@points?.text())
            case 'path':
                return bboxOfNumbers(node.@d?.text())
            case 'g':
                // union of children bboxes
                double[] union = null
                node.children().each { child ->
                    double[] childBox = bboxOf(child)
                    if (childBox != null) {
                        union = union == null ? childBox : [
                                Math.min(union[0], childBox[0]), Math.min(union[1], childBox[1]),
                                Math.max(union[2], childBox[2]), Math.max(union[3], childBox[3]),
                        ] as double[]
                    }
                }
                return union
            default:
                return null
        }
    }

    /** All numbers in a points/d attribute string, grouped into [x,y] pairs. */
    private static List<List<Double>> numberPairs(String raw) {
        if (!raw) return []
        List<Double> nums = []
        Matcher m = NUMBER.matcher(raw)
        while (m.find()) {
            nums << (m.group() as double)
        }
        List<List<Double>> pairs = []
        for (int i = 0; i + 1 < nums.size(); i += 2) {
            pairs << [nums[i], nums[i + 1]]
        }
        return pairs
    }

    /** Min/max over all numbers-as-pairs found in a points/d attribute string. */
    private static double[] bboxOfNumbers(String raw) {
        List<List<Double>> pairs = numberPairs(raw)
        if (pairs.size() < 2) return null
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE
        pairs.each { p ->
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0])
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1])
        }
        [minX, minY, maxX, maxY] as double[]
    }
}
