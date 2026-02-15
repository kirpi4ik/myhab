package org.myhab.services

import grails.gorm.transactions.Transactional
import org.myhab.domain.device.Cable

import javax.imageio.ImageIO
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Service for generating printable labels for cables and other entities.
 * Optimized for Brother PT-P710BT label printer.
 * 
 * Common Brother label tape widths:
 * - 6mm (TZe-211)
 * - 9mm (TZe-221) 
 * - 12mm (TZe-231)
 * - 18mm (TZe-241)
 * - 24mm (TZe-251)
 */
@Transactional(readOnly = true)
class LabelService {

    // Label template configurations
    static final Map<String, Map> TEMPLATES = [
        'small': [
            width: 200,
            height: 60,
            fontSize: 12,
            titleSize: 14,
            padding: 5,
            fields: ['code']
        ],
        'medium': [
            width: 300,
            height: 80,
            fontSize: 14,
            titleSize: 14,
            padding: 8,
            fields: ['code', 'description']
        ],
        'large': [
            width: 400,
            height: 100,
            fontSize: 10,
            titleSize: 12,
            padding: 10,
            fields: ['id', 'code', 'description', 'category']
        ],
        'brother_12mm': [
            width: 280,
            height: 64,  // ~12mm at 180 DPI
            fontSize: 11,
            titleSize: 14,
            padding: 4,
            fields: ['code', 'description']
        ],
        'brother_18mm': [
            width: 350,
            height: 96,  // ~18mm at 180 DPI
            fontSize: 16,
            titleSize: 16,
            padding: 6,
            fields: ['code', 'description', 'category']
        ],
        'brother_24mm': [
            width: 400,
            height: 128, // ~24mm at 180 DPI
            fontSize: 12,
            titleSize: 16,
            padding: 8,
            fields: ['id', 'code', 'description', 'category']
        ]
    ]

    /**
     * Generate a label image for a cable
     * 
     * @param cableId The cable ID
     * @param templateName The template to use (small, medium, large, brother_12mm, etc.)
     * @param customFields Optional list of fields to override template defaults
     * @param customWidth Optional custom width
     * @param customHeight Optional custom height
     * @return byte array of PNG image
     */
    byte[] generateCableLabel(Long cableId, String templateName = 'medium', 
                               List<String> customFields = null,
                               Integer customWidth = null, 
                               Integer customHeight = null) {
        
        Cable cable = Cable.get(cableId)
        if (!cable) {
            throw new IllegalArgumentException("Cable not found with id: ${cableId}")
        }

        // Get template config
        Map template = TEMPLATES[templateName] ?: TEMPLATES['medium']
        
        // Apply custom overrides
        int width = customWidth ?: template.width
        int height = customHeight ?: template.height
        List<String> fields = customFields ?: template.fields

        // Build field values map
        Map<String, String> fieldValues = buildCableFieldValues(cable, fields)

        return generateLabelImage(fieldValues, width, height, template)
    }

    /**
     * Build a map of field names to display values for a cable
     */
    private Map<String, String> buildCableFieldValues(Cable cable, List<String> fields) {
        Map<String, String> values = [:]
        
        fields.each { field ->
            switch (field) {
                case 'id':
                    values['ID'] = cable.id?.toString() ?: '-'
                    break
                case 'code':
                    values['Code'] = cable.code ?: '-'
                    break
                case 'codeNew':
                    values['New Code'] = cable.codeNew ?: '-'
                    break
                case 'codeOld':
                    values['Old Code'] = cable.codeOld ?: '-'
                    break
                case 'description':
                    values['Desc'] = truncate(cable.description, 40) ?: '-'
                    break
                case 'category':
                    values['Cat'] = cable.category?.name ?: '-'
                    break
                case 'rack':
                    values['Rack'] = cable.rack?.name ?: '-'
                    break
                case 'patchPanel':
                    if (cable.patchPanel) {
                        values['Panel'] = "${cable.patchPanel.name}:${cable.patchPanelPort ?: '?'}"
                    }
                    break
                case 'nrWires':
                    values['Wires'] = cable.nrWires?.toString() ?: '-'
                    break
            }
        }
        
        return values
    }

    /**
     * Generate the actual label image
     */
    private byte[] generateLabelImage(Map<String, String> fieldValues, int width, int height, Map template) {
        // Create image with white background
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        Graphics2D g2d = image.createGraphics()

        // Enable anti-aliasing for better text quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Fill white background
        g2d.setColor(Color.WHITE)
        g2d.fillRect(0, 0, width, height)

        // Draw border
        g2d.setColor(Color.BLACK)
        g2d.setStroke(new BasicStroke(2))
        g2d.drawRect(1, 1, width - 3, height - 3)

        int padding = template.padding ?: 8
        int fontSize = template.fontSize ?: 11
        int titleSize = template.titleSize ?: 14
        
        Font titleFont = new Font("SansSerif", Font.BOLD, titleSize)
        Font labelFont = new Font("SansSerif", Font.PLAIN, fontSize)
        Font valueFont = new Font("SansSerif", Font.BOLD, fontSize)

        int y = padding + titleSize
        int x = padding

        // Draw the main code as title (first field, usually 'code')
        String mainValue = fieldValues.values().find { it } ?: 'NO DATA'
        g2d.setFont(titleFont)
        g2d.setColor(Color.BLACK)
        
        FontMetrics titleMetrics = g2d.getFontMetrics()
        
        // Draw values only (no field labels)
        if (fieldValues.size() == 1) {
            // Single field - center it
            int textWidth = titleMetrics.stringWidth(mainValue)
            g2d.drawString(mainValue, (width - textWidth) / 2, y)
        } else {
            // Multiple fields - first field centered, rest left-aligned
            boolean isFirst = true
            fieldValues.each { label, value ->
                if (y + fontSize > height - padding) return // Don't overflow
                
                if (isFirst) {
                    // First field as title, centered and bold
                    g2d.setFont(titleFont)
                    g2d.setColor(Color.BLACK)
                    int textWidth = titleMetrics.stringWidth(value)
                    g2d.drawString(value, (width - textWidth) / 2, y)
                    y += titleSize + 4
                    isFirst = false
                } else {
                    // Subsequent fields - just the value, left-aligned
                    g2d.setFont(valueFont)
                    g2d.setColor(Color.BLACK)
                    g2d.drawString(value, x, y)
                    y += fontSize + 3
                }
            }
        }

        g2d.dispose()

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return baos.toByteArray()
    }

    /**
     * Get available template names
     */
    List<String> getAvailableTemplates() {
        return TEMPLATES.keySet().toList()
    }

    /**
     * Get template configuration
     */
    Map getTemplateConfig(String templateName) {
        return TEMPLATES[templateName]
    }

    /**
     * Get available fields for cables
     */
    List<String> getAvailableCableFields() {
        return ['id', 'code', 'codeNew', 'codeOld', 'description', 'category', 'rack', 'patchPanel', 'nrWires']
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (!str) return str
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + '...' : str
    }
}
