package org.myhab.controller

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.services.LabelService

/**
 * REST controller for printable label PNG generation.
 * Kept as REST (not GraphQL) because GraphQL doesn't ergonomically serve binary streams —
 * the PNG endpoint is consumed via direct fetch + browser download from labelService.
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class LabelController {

    LabelService labelService

    static responseFormats = ['json', 'png']
    static allowedMethods = [
        generateCableLabel     : 'GET',
        generateDeviceLabel    : 'GET',
        generatePeripheralLabel: 'GET'
    ]

    /**
     * Generate a label image for a cable. GET /api/labels/cable/{id}
     *
     * Query parameters (shared by all label endpoints):
     * - template: Template name (small, medium, large, brother_12mm, brother_18mm, brother_24mm)
     * - fields: Comma-separated list of fields to include
     * - width / height: Custom dimensions in pixels
     * - download: If true, sets Content-Disposition to attachment
     * - qr: 'true'/'false' to force-enable/disable the QR code, overriding the
     *       global feature.qr.enabled toggle (used by the settings-page preview)
     */
    def generateCableLabel() {
        renderLabel('cable') { Long id, String tpl, List<String> fields, Integer w, Integer h, Boolean qr ->
            labelService.generateCableLabel(id, tpl, fields, w, h, qr)
        }
    }

    /** Generate a label image for a device. GET /api/labels/device/{id} */
    def generateDeviceLabel() {
        renderLabel('device') { Long id, String tpl, List<String> fields, Integer w, Integer h, Boolean qr ->
            labelService.generateDeviceLabel(id, tpl, fields, w, h, qr)
        }
    }

    /** Generate a label image for a peripheral. GET /api/labels/peripheral/{id} */
    def generatePeripheralLabel() {
        renderLabel('peripheral') { Long id, String tpl, List<String> fields, Integer w, Integer h, Boolean qr ->
            labelService.generatePeripheralLabel(id, tpl, fields, w, h, qr)
        }
    }

    /**
     * Shared param parsing + PNG streaming. `kind` is the entity slug used in
     * the download filename; `generator` produces the PNG bytes for the parsed
     * id + options.
     */
    private void renderLabel(String kind, Closure<byte[]> generator) {
        Long id = params.id as Long
        if (!id) {
            response.status = 400
            render([error: "${kind.capitalize()} ID is required"] as JSON)
            return
        }

        String templateName = params.template ?: 'brother_18mm'
        String fieldsParam = params.fields as String
        List<String> fields = fieldsParam ? fieldsParam.split(',').collect { it.trim() } : null
        Integer customWidth = params.width ? params.width as Integer : null
        Integer customHeight = params.height ? params.height as Integer : null
        boolean download = params.download == 'true'
        Boolean qrOverride = params.qr != null ? (params.qr == 'true') : null

        try {
            byte[] imageData = generator(id, templateName, fields, customWidth, customHeight, qrOverride)

            response.contentType = 'image/png'
            response.contentLength = imageData.length
            String disposition = download ? 'attachment' : 'inline'
            response.setHeader('Content-Disposition', "${disposition}; filename=\"${kind}-${id}-label.png\"")

            response.outputStream << imageData
            response.outputStream.flush()

        } catch (IllegalArgumentException e) {
            response.status = 404
            render([error: e.message] as JSON)
        } catch (Exception e) {
            log.error("Error generating ${kind} label", e)
            response.status = 500
            render([error: 'Failed to generate label: ' + e.message] as JSON)
        }
    }
}
