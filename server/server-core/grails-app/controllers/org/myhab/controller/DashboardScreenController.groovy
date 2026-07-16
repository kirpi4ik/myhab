package org.myhab.controller

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.domain.ui.DashboardScreen
import org.myhab.domain.ui.DashboardScreenBackground
import org.myhab.services.dashboard.DashboardScreenImportService

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * REST endpoints for dashboard screen binary content (kept as REST — GraphQL
 * doesn't serve binary streams; same split as avatars and label PNGs):
 * <ul>
 *   <li>GET  /api/screens/$id/background — stream the floor-plan image</li>
 *   <li>PUT  /api/screens/$id/background — upload/replace it (admin)</li>
 *   <li>POST /api/screens/import-svg     — import a legacy /wui SVG (admin)</li>
 * </ul>
 * Screen metadata and layout JSON go through GraphQL (see DashboardScreen).
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class DashboardScreenController {

    static allowedMethods = [
            show            : 'GET',
            update          : 'PUT',
            resizeBackground: 'POST',
            importSvg       : 'POST',
    ]

    private static final List<String> ALLOWED_CONTENT_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
    private static final int MAX_SVG_SIZE = 8 * 1024 * 1024

    /**
     * Fixed canvas presets so screens target a device class regardless of the
     * uploaded image's original size/ratio. The image is stretched to exactly
     * the preset size (no letterbox borders — see fitToCanvas).
     */
    private static final Map<String, int[]> SIZE_PRESETS = [
            mobile: [780, 500] as int[],
            tablet: [1280, 800] as int[],
    ]

    DashboardScreenImportService dashboardScreenImportService

    /**
     * GET /api/screens/$id/background
     * Streams the background image; 204 when the screen has none. Immutable
     * cache — the client busts with ?v=<tsUpdated>.
     */
    def show() {
        DashboardScreen screen = DashboardScreen.get(params.long('id'))
        if (!screen) {
            response.status = 404
            render([error: 'Screen not found'] as JSON)
            return
        }
        DashboardScreenBackground background = DashboardScreenBackground.findByScreen(screen)
        if (!background?.data) {
            render(status: 204)
            return
        }
        response.setHeader('Cache-Control', 'private, max-age=31536000, immutable')
        render(file: new ByteArrayInputStream(background.data),
                contentType: screen.backgroundContentType ?: 'image/jpeg',
                fileName: 'background')
    }

    /**
     * PUT /api/screens/$id/background?preset=mobile|tablet
     * Multipart upload (field "file"). Without a preset the image keeps its
     * natural dimensions; with one it is stretched to a fixed canvas
     * (see SIZE_PRESETS). If the screen's canvas dimensions change, existing
     * widget coordinates are rescaled proportionally so the layout survives
     * background replacement.
     */
    @Secured(['ROLE_ADMIN'])
    @Transactional
    def update() {
        DashboardScreen screen = DashboardScreen.get(params.long('id'))
        if (!screen) {
            response.status = 404
            render([error: 'Screen not found'] as JSON)
            return
        }
        def file = request.getFile('file')
        if (!file || file.empty) {
            response.status = 400
            render([error: 'No file uploaded. Use multipart form field "file".'] as JSON)
            return
        }
        if (file.size > DashboardScreenBackground.MAX_SIZE) {
            response.status = 400
            render([error: "Background must not exceed ${DashboardScreenBackground.MAX_SIZE / (1024 * 1024)}MB"] as JSON)
            return
        }
        String contentType = file.contentType?.toLowerCase()
        if (!(contentType in ALLOWED_CONTENT_TYPES)) {
            response.status = 400
            render([error: "Allowed types: ${ALLOWED_CONTENT_TYPES.join(', ')}"] as JSON)
            return
        }
        String preset = params.preset?.toLowerCase()
        if (preset && !SIZE_PRESETS.containsKey(preset)) {
            response.status = 400
            render([error: "Unknown preset '${preset}'. Allowed: ${SIZE_PRESETS.keySet().join(', ')}"] as JSON)
            return
        }
        byte[] bytes = file.bytes
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes))
        if (image == null) {
            response.status = 400
            render([error: 'File is not a decodable image'] as JSON)
            return
        }

        String storedContentType = contentType == 'image/jpg' ? 'image/jpeg' : contentType
        if (preset) {
            int[] canvas = SIZE_PRESETS[preset]
            image = fitToCanvas(image, canvas[0], canvas[1])
            // ImageIO cannot encode webp — normalized canvases are stored as jpeg
            storedContentType = 'image/jpeg'
            def out = new ByteArrayOutputStream()
            ImageIO.write(image, 'jpg', out)
            bytes = out.toByteArray()
        }

        // Keep existing widget placements proportionally valid when the
        // canvas dimensions change (e.g. new image size or preset switch).
        if (screen.backgroundWidth && screen.backgroundHeight && screen.layoutJson
                && (screen.backgroundWidth != image.width || screen.backgroundHeight != image.height)) {
            screen.layoutJson = rescaleLayout(screen.layoutJson,
                    image.width / (double) screen.backgroundWidth,
                    image.height / (double) screen.backgroundHeight)
        }

        screen.backgroundContentType = storedContentType
        screen.backgroundWidth = image.width
        screen.backgroundHeight = image.height
        screen.save(flush: true, failOnError: true)

        DashboardScreenBackground background = DashboardScreenBackground.findByScreen(screen)
                ?: new DashboardScreenBackground(screen: screen)
        background.data = bytes
        background.save(flush: true, failOnError: true)

        render([success: true, width: image.width, height: image.height] as JSON)
    }

    /**
     * POST /api/screens/$id/background/resize?preset=mobile|tablet
     * Stretches the already-stored background to a preset canvas (no re-upload)
     * and rescales the saved layout proportionally — same processing as a
     * preset upload, but starting from the stored bytes.
     */
    @Secured(['ROLE_ADMIN'])
    @Transactional
    def resizeBackground() {
        DashboardScreen screen = DashboardScreen.get(params.long('id'))
        if (!screen) {
            response.status = 404
            render([error: 'Screen not found'] as JSON)
            return
        }
        DashboardScreenBackground background = DashboardScreenBackground.findByScreen(screen)
        if (!background?.data) {
            response.status = 400
            render([error: 'Screen has no background to resize'] as JSON)
            return
        }
        String preset = params.preset?.toLowerCase()
        if (!SIZE_PRESETS.containsKey(preset)) {
            response.status = 400
            render([error: "Param 'preset' is required. Allowed: ${SIZE_PRESETS.keySet().join(', ')}"] as JSON)
            return
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(background.data))
        if (image == null) {
            response.status = 400
            render([error: 'Stored background is not a decodable image'] as JSON)
            return
        }
        int[] canvas = SIZE_PRESETS[preset]
        if (image.width == canvas[0] && image.height == canvas[1]) {
            render([success: true, width: image.width, height: image.height] as JSON)
            return
        }
        image = fitToCanvas(image, canvas[0], canvas[1])
        if (screen.backgroundWidth && screen.backgroundHeight && screen.layoutJson) {
            screen.layoutJson = rescaleLayout(screen.layoutJson,
                    image.width / (double) screen.backgroundWidth,
                    image.height / (double) screen.backgroundHeight)
        }
        def out = new ByteArrayOutputStream()
        ImageIO.write(image, 'jpg', out)

        screen.backgroundContentType = 'image/jpeg'
        screen.backgroundWidth = image.width
        screen.backgroundHeight = image.height
        screen.save(flush: true, failOnError: true)

        background.data = out.toByteArray()
        background.save(flush: true, failOnError: true)

        render([success: true, width: image.width, height: image.height] as JSON)
    }

    /**
     * Stretch an image to exactly the given canvas size. No aspect-preserving
     * letterboxing: white borders waste screen space and break the per-axis
     * widget rescale in rescaleLayout (which assumes the image fills the
     * canvas). Floor plans are usually close to the target ratio, so the
     * distortion is negligible.
     */
    private static BufferedImage fitToCanvas(BufferedImage source, int canvasWidth, int canvasHeight) {
        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB)
        def graphics = canvas.createGraphics()
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(source, 0, 0, canvasWidth, canvasHeight, null)
        } finally {
            graphics.dispose()
        }
        return canvas
    }

    /** Scale all widget coordinates/sizes in a layout document by (sx, sy). */
    private static String rescaleLayout(String layoutJson, double sx, double sy) {
        def round1 = { double v -> Math.round(v * 10d) / 10d }
        def layout = new groovy.json.JsonSlurper().parseText(layoutJson)
        double sizeScale = Math.min(sx, sy)
        layout.widgets?.each { w ->
            if (w.x != null) w.x = round1((w.x as double) * sx)
            if (w.y != null) w.y = round1((w.y as double) * sy)
            if (w.points) w.points = w.points.collect { p -> [round1((p[0] as double) * sx), round1((p[1] as double) * sy)] }
            if (w.size != null) w.size = round1((w.size as double) * sizeScale)
            if (w.fontSize != null) w.fontSize = round1((w.fontSize as double) * sizeScale)
        }
        return groovy.json.JsonOutput.toJson(layout)
    }

    /**
     * POST /api/screens/import-svg
     * Multipart upload (field "file") of a legacy /wui SVG + "name" param.
     * Creates a disabled screen seeded from the SVG's asset-* elements.
     */
    @Secured(['ROLE_ADMIN'])
    def importSvg() {
        def file = request.getFile('file')
        String name = params.name?.trim()
        if (!file || file.empty || !name) {
            response.status = 400
            render([error: 'Multipart field "file" (.svg) and param "name" are required'] as JSON)
            return
        }
        if (file.size > MAX_SVG_SIZE) {
            response.status = 400
            render([error: "SVG must not exceed ${MAX_SVG_SIZE / (1024 * 1024)}MB"] as JSON)
            return
        }
        if (DashboardScreen.findByName(name)) {
            response.status = 409
            render([error: "A screen named '${name}' already exists"] as JSON)
            return
        }
        try {
            DashboardScreen screen = dashboardScreenImportService.importLegacySvg(name, file.bytes)
            def layout = new groovy.json.JsonSlurper().parseText(screen.layoutJson)
            render([success: true, id: screen.id, widgets: layout.widgets.size()] as JSON)
        } catch (IllegalArgumentException e) {
            response.status = 400
            render([error: e.message] as JSON)
        }
    }
}
