package org.myhab.controller

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.services.LabelService

/**
 * REST Controller for generating printable labels.
 * Supports various label sizes optimized for Brother PT-P710BT printer.
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class LabelController {

    LabelService labelService

    static responseFormats = ['json', 'png']
    static allowedMethods = [
        generateCableLabel: 'GET',
        templates: 'GET',
        fields: 'GET'
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
        List<String> fields = params.fields ? params.fields.split(',').collect { it.trim() } : null
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

    /**
     * Get available label templates.
     * 
     * GET /api/labels/templates
     * 
     * @return JSON list of template configurations
     */
    def templates() {
        def templates = labelService.availableTemplates.collect { name ->
            def config = labelService.getTemplateConfig(name)
            [
                name: name,
                width: config.width,
                height: config.height,
                fields: config.fields,
                description: getTemplateDescription(name)
            ]
        }
        render templates as JSON
    }

    /**
     * Get available fields for cable labels.
     * 
     * GET /api/labels/fields/cable
     * 
     * @return JSON list of available field names
     */
    def fields() {
        String entityType = params.type ?: 'cable'
        
        def fields
        switch (entityType) {
            case 'cable':
                fields = labelService.availableCableFields
                break
            default:
                fields = labelService.availableCableFields
        }
        
        render([
            entityType: entityType,
            fields: fields
        ] as JSON)
    }

    /**
     * Get human-readable description for templates
     */
    private String getTemplateDescription(String name) {
        switch (name) {
            case 'small': return 'Compact label with code only'
            case 'medium': return 'Standard label with code and description'
            case 'large': return 'Full label with all details'
            case 'brother_12mm': return 'Brother 12mm tape (TZe-231)'
            case 'brother_18mm': return 'Brother 18mm tape (TZe-241)'
            case 'brother_24mm': return 'Brother 24mm tape (TZe-251)'
            default: return name
        }
    }
}
