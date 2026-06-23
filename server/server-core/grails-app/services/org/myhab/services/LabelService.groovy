package org.myhab.services

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import grails.gorm.transactions.Transactional
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.device.Cable
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral

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

    ConfigProvider configProvider

    // Default QR content when feature.qr.content.template is not configured.
    // Must keep the leading `myhab://{entityType}/{id}` token so the in-app
    // scanner can parse the entity type + id back out (see Phase 2 scanner).
    static final String DEFAULT_QR_TEMPLATE = 'myhab://{entityType}/{id}'

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
                               Integer customHeight = null,
                               Boolean includeQrOverride = null) {

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

        return composeLabel(fieldValues, width, height, template, 'CABLE', cableQrVars(cable), includeQrOverride)
    }

    /**
     * Generate a label image for a device.
     *
     * @param deviceId The device ID
     * @param templateName Template to use (defaults to brother_18mm via controller)
     * @param customFields Optional field list overriding the device default
     * @param customWidth/customHeight Optional canvas overrides
     * @param includeQrOverride Force QR on/off, overriding the global toggle
     * @return byte array of PNG image
     */
    byte[] generateDeviceLabel(Long deviceId, String templateName = 'brother_18mm',
                                List<String> customFields = null,
                                Integer customWidth = null,
                                Integer customHeight = null,
                                Boolean includeQrOverride = null) {

        Device device = Device.get(deviceId)
        if (!device) {
            throw new IllegalArgumentException("Device not found with id: ${deviceId}")
        }

        Map template = TEMPLATES[templateName] ?: TEMPLATES['medium']
        int width = customWidth ?: template.width
        int height = customHeight ?: template.height
        List<String> fields = customFields ?: ['code', 'name', 'model']

        Map<String, String> fieldValues = buildDeviceFieldValues(device, fields)

        return composeLabel(fieldValues, width, height, template, 'DEVICE', deviceQrVars(device), includeQrOverride)
    }

    /**
     * Generate a label image for a peripheral.
     *
     * @param peripheralId The peripheral ID
     * @param templateName Template to use (defaults to brother_18mm via controller)
     * @param customFields Optional field list overriding the peripheral default
     * @param customWidth/customHeight Optional canvas overrides
     * @param includeQrOverride Force QR on/off, overriding the global toggle
     * @return byte array of PNG image
     */
    byte[] generatePeripheralLabel(Long peripheralId, String templateName = 'brother_18mm',
                                    List<String> customFields = null,
                                    Integer customWidth = null,
                                    Integer customHeight = null,
                                    Boolean includeQrOverride = null) {

        DevicePeripheral peripheral = DevicePeripheral.get(peripheralId)
        if (!peripheral) {
            throw new IllegalArgumentException("Peripheral not found with id: ${peripheralId}")
        }

        Map template = TEMPLATES[templateName] ?: TEMPLATES['medium']
        int width = customWidth ?: template.width
        int height = customHeight ?: template.height
        List<String> fields = customFields ?: ['name', 'model', 'category']

        Map<String, String> fieldValues = buildPeripheralFieldValues(peripheral, fields)

        return composeLabel(fieldValues, width, height, template, 'PERIPHERAL', peripheralQrVars(peripheral), includeQrOverride)
    }

    /**
     * Shared label assembly: optionally renders + composites a QR (driven by the
     * global feature.qr.enabled toggle, with an explicit override winning), then
     * delegates to generateLabelImage.
     */
    private byte[] composeLabel(Map<String, String> fieldValues, int width, int height, Map template,
                                String entityType, Map<String, String> qrVars, Boolean includeQrOverride) {
        BufferedImage qrImage = null
        String qrPosition = 'right'
        boolean qrEnabled = includeQrOverride != null ? includeQrOverride : isQrEnabled()
        if (qrEnabled) {
            int padding = (template.padding ?: 8) as int
            int qrSize = resolveQrSize(height, padding)
            String content = buildQrContent(entityType, qrVars, getQrTemplate())
            qrImage = renderQr(content, qrSize)
            qrPosition = getQrPosition()
        }

        return generateLabelImage(fieldValues, width, height, template, qrImage, qrPosition)
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
     * Build a map of field names to display values for a device.
     */
    private Map<String, String> buildDeviceFieldValues(Device device, List<String> fields) {
        Map<String, String> values = [:]
        fields.each { field ->
            switch (field) {
                case 'id':
                    values['ID'] = device.id?.toString() ?: '-'
                    break
                case 'code':
                    values['Code'] = device.code ?: '-'
                    break
                case 'name':
                    values['Name'] = device.name ?: '-'
                    break
                case 'model':
                    values['Model'] = device.model?.toString() ?: '-'
                    break
                case 'type':
                    values['Type'] = device.type?.name ?: '-'
                    break
                case 'description':
                    values['Desc'] = truncate(device.description, 40) ?: '-'
                    break
                case 'rack':
                    values['Rack'] = device.rack?.name ?: '-'
                    break
            }
        }
        return values
    }

    /**
     * Build a map of field names to display values for a peripheral.
     */
    private Map<String, String> buildPeripheralFieldValues(DevicePeripheral peripheral, List<String> fields) {
        Map<String, String> values = [:]
        fields.each { field ->
            switch (field) {
                case 'id':
                    values['ID'] = peripheral.id?.toString() ?: '-'
                    break
                case 'name':
                    values['Name'] = peripheral.name ?: '-'
                    break
                case 'model':
                    values['Model'] = peripheral.model ?: '-'
                    break
                case 'category':
                    values['Cat'] = peripheral.category?.name ?: '-'
                    break
                case 'description':
                    values['Desc'] = truncate(peripheral.description, 40) ?: '-'
                    break
            }
        }
        return values
    }

    /**
     * Generate the actual label image.
     *
     * When a QR image is supplied the canvas is widened by the QR width + padding
     * and the QR is drawn on the configured side; the text region keeps its
     * original width so existing layout/centering math is unchanged. When qrImage
     * is null the output is byte-for-byte identical to the previous behavior.
     */
    private byte[] generateLabelImage(Map<String, String> fieldValues, int width, int height, Map template,
                                       BufferedImage qrImage = null, String qrPosition = 'right') {
        int padding = template.padding ?: 8
        int fontSize = template.fontSize ?: 11
        int titleSize = template.titleSize ?: 14

        // Reserve room for the QR on the configured side, if present.
        int qrDim = qrImage ? qrImage.width : 0
        int qrGutter = qrImage ? qrDim + padding : 0
        boolean qrLeft = qrImage && qrPosition?.equalsIgnoreCase('left')
        int totalWidth = width + qrGutter
        int textXOffset = qrLeft ? qrGutter : 0

        // Create image with white background
        BufferedImage image = new BufferedImage(totalWidth, height, BufferedImage.TYPE_INT_RGB)
        Graphics2D g2d = image.createGraphics()

        // Enable anti-aliasing for better text quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Fill white background
        g2d.setColor(Color.WHITE)
        g2d.fillRect(0, 0, totalWidth, height)

        // Draw the QR (vertically centered) in its gutter before shifting the text origin.
        if (qrImage) {
            int qrX = qrLeft ? padding : (totalWidth - qrDim - padding)
            int qrY = ((height - qrDim) / 2) as int
            g2d.drawImage(qrImage, qrX, qrY, null)
        }

        // Shift the origin so the text region renders in its own width-sized area.
        if (textXOffset) {
            g2d.translate(textXOffset, 0)
        }

        // Draw border
        g2d.setColor(Color.BLACK)
        g2d.setStroke(new BasicStroke(2))
        g2d.drawRect(1, 1, width - 3, height - 3)

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

    // ----- QR code support -------------------------------------------------

    /** Whether the global QR feature toggle is on (default: off). */
    boolean isQrEnabled() {
        return cfg(Boolean, CfgKey.QR.QR_ENABLED.key(), false)
    }

    /** Configured QR content template, falling back to the stable default. */
    String getQrTemplate() {
        String tpl = cfg(String, CfgKey.QR.QR_CONTENT_TEMPLATE.key(), DEFAULT_QR_TEMPLATE)
        return tpl?.trim() ? tpl.trim() : DEFAULT_QR_TEMPLATE
    }

    /** Side of the label the QR is drawn on: 'left' or 'right' (default: right). */
    String getQrPosition() {
        String pos = cfg(String, CfgKey.QR.QR_POSITION.key(), 'right')
        return pos?.equalsIgnoreCase('left') ? 'left' : 'right'
    }

    /** Resolve the QR square size in px: configured value, or fit to label height. */
    private int resolveQrSize(int height, int padding) {
        int configured = cfg(Integer, CfgKey.QR.QR_SIZE.key(), 0)
        if (configured > 0) {
            return configured
        }
        return Math.max(32, height - 2 * padding)
    }

    /**
     * Resolve template variables against an entity's value map. The leading
     * `myhab://{entityType}/{id}` token in the default template lets the
     * scanner recover the entity type + id regardless of any trailing text.
     */
    private String buildQrContent(String entityType, Map<String, String> vars, String template) {
        Map<String, String> all = [entityType: entityType] + (vars ?: [:])
        String out = template
        all.each { String k, String v -> out = out.replace("{${k}}", v ?: '') }
        return out
    }

    /** QR template variables for a cable. */
    private Map<String, String> cableQrVars(Cable cable) {
        return [
            id         : cable.id?.toString() ?: '',
            code       : cable.code ?: '',
            description: cable.description ?: '',
            category   : cable.category?.name ?: '',
            rack       : cable.rack?.name ?: '',
            patchPanel : cable.patchPanel ? "${cable.patchPanel.name}:${cable.patchPanelPort ?: '?'}" : ''
        ]
    }

    /** QR template variables for a device. */
    private Map<String, String> deviceQrVars(Device device) {
        return [
            id         : device.id?.toString() ?: '',
            code       : device.code ?: '',
            name       : device.name ?: '',
            model      : device.model?.toString() ?: '',
            type       : device.type?.name ?: '',
            description: device.description ?: '',
            rack       : device.rack?.name ?: ''
        ]
    }

    /** QR template variables for a peripheral. */
    private Map<String, String> peripheralQrVars(DevicePeripheral peripheral) {
        return [
            id         : peripheral.id?.toString() ?: '',
            name       : peripheral.name ?: '',
            model      : peripheral.model ?: '',
            category   : peripheral.category?.name ?: '',
            description: peripheral.description ?: ''
        ]
    }

    /** Variables available to the QR content template (for the settings UI). */
    List<String> getQrTemplateVariables() {
        return ['entityType', 'id', 'code', 'description', 'category', 'rack', 'patchPanel']
    }

    /** Current QR settings as a map, shaped for the qrConfig GraphQL query. */
    Map getQrConfig() {
        return [
            enabled           : isQrEnabled(),
            contentTemplate   : getQrTemplate(),
            position          : getQrPosition(),
            size              : cfg(Integer, CfgKey.QR.QR_SIZE.key(), 0),
            availableVariables: getQrTemplateVariables()
        ]
    }

    /** Render a QR code as a square BufferedImage. */
    private BufferedImage renderQr(String content, int sizePx) {
        QRCodeWriter writer = new QRCodeWriter()
        Map hints = [
            (EncodeHintType.ERROR_CORRECTION): ErrorCorrectionLevel.M,
            (EncodeHintType.MARGIN)          : 1
        ]
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        return MatrixToImageWriter.toBufferedImage(matrix)
    }

    /** Null-safe, typed read from the git-backed config with a fallback default. */
    private <T> T cfg(Class<T> cls, String key, T defaultValue) {
        try {
            T value = configProvider?.get(cls, key)
            return value != null ? value : defaultValue
        } catch (Exception ignored) {
            return defaultValue
        }
    }
}
