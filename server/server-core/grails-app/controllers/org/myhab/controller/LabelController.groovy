package org.myhab.controller

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.services.LabelService

/**
 * REST controller for printable label PNG generation.
 * Kept as REST (not GraphQL) because GraphQL doesn't ergonomically serve binary streams —
 * the PNG endpoint is consumed via direct fetch + browser download from labelService.downloadCableLabel.
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class LabelController {

    LabelService labelService

    static responseFormats = ['json', 'png']
    static allowedMethods = [
        generateCableLabel: 'GET'
    ]

    /**
     * Generate a label image for a cable.
     *
     * GET /api/labels/cable/{id}
     *
     * Query parameters:
     * - template: Template name (small, medium, large, brother_12mm, brother_18mm, brother_24mm)
     * - fields: Comma-separated list of fields to include (id, code, description, category, rack, patchPanel)
     * - width: Custom width in pixels
     * - height: Custom height in pixels
     * - download: If true, sets Content-Disposition to attachment
     *
     * @return PNG image
     */
    def generateCableLabel() {
        Long cableId = params.id as Long

        if (!cableId) {
            response.status = 400
            render([error: 'Cable ID is required'] as JSON)
            return
        }

        String templateName = params.template ?: 'brother_18mm'
        String fieldsParam = params.fields as String
        List<String> fields = fieldsParam ? fieldsParam.split(',').collect { it.trim() } : null
        Integer customWidth = params.width ? params.width as Integer : null
        Integer customHeight = params.height ? params.height as Integer : null
        boolean download = params.download == 'true'

        try {
            byte[] imageData = labelService.generateCableLabel(
                cableId,
                templateName,
                fields,
                customWidth,
                customHeight
            )

            response.contentType = 'image/png'
            response.contentLength = imageData.length

            if (download) {
                response.setHeader('Content-Disposition', "attachment; filename=\"cable-${cableId}-label.png\"")
            } else {
                response.setHeader('Content-Disposition', "inline; filename=\"cable-${cableId}-label.png\"")
            }

            response.outputStream << imageData
            response.outputStream.flush()

        } catch (IllegalArgumentException e) {
            response.status = 404
            render([error: e.message] as JSON)
        } catch (Exception e) {
            log.error("Error generating cable label", e)
            response.status = 500
            render([error: 'Failed to generate label: ' + e.message] as JSON)
        }
    }
}
