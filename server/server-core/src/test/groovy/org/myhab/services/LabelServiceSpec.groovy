package org.myhab.services

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import grails.testing.services.ServiceUnitTest
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.device.Cable
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class LabelServiceSpec extends Specification implements ServiceUnitTest<LabelService> {

    private static BufferedImage readPng(byte[] bytes) {
        return ImageIO.read(new ByteArrayInputStream(bytes))
    }

    private static String decodeQr(BufferedImage img) {
        def source = new BufferedImageLuminanceSource(img)
        def bitmap = new BinaryBitmap(new HybridBinarizer(source))
        return new MultiFormatReader().decode(bitmap).text
    }

    void "QR-disabled label keeps the original canvas width"() {
        given:
            Map template = LabelService.TEMPLATES['brother_18mm']

        when: "rendering without a QR image (current behaviour)"
            byte[] bytes = service.generateLabelImage([Code: 'C-042'], 350, 96, template, null, 'right')
            BufferedImage img = readPng(bytes)

        then:
            img.width == 350
            img.height == 96
    }

    void "QR-enabled label widens the canvas and the QR decodes to the entity token"() {
        given:
            Map template = LabelService.TEMPLATES['brother_18mm']
            int padding = template.padding as int
            int qrDim = 64
            BufferedImage qr = service.renderQr('myhab://CABLE/42', qrDim)

        when: "compositing the QR on the right"
            byte[] bytes = service.generateLabelImage([Code: 'C-042'], 350, 96, template, qr, 'right')
            BufferedImage img = readPng(bytes)

        then: "canvas grows by the QR width + padding"
            img.width == 350 + qrDim + padding

        and: "the rendered QR is scannable and carries the stable entity token"
            decodeQr(img) == 'myhab://CABLE/42'
    }

    void "buildQrContent resolves template variables, keeping the myhab://TYPE/ID token"() {
        given:
            Cable cable = Stub(Cable) {
                getId() >> 42L
                getCode() >> 'C-042'
                getDescription() >> 'Living room'
                getCategory() >> null
                getRack() >> null
                getPatchPanel() >> null
            }
            Map vars = service.cableQrVars(cable)

        expect:
            service.buildQrContent('CABLE', vars, 'myhab://{entityType}/{id}') == 'myhab://CABLE/42'
            service.buildQrContent('CABLE', vars, 'myhab://{entityType}/{id} {code}') == 'myhab://CABLE/42 C-042'
            service.buildQrContent('DEVICE', [id: '7'], 'myhab://{entityType}/{id}') == 'myhab://DEVICE/7'
    }

    void "config helpers fall back to defaults when the key is absent"() {
        given:
            service.configProvider = Stub(ConfigProvider) {
                get(_, _) >> null
            }

        expect:
            !service.isQrEnabled()
            service.qrTemplate == LabelService.DEFAULT_QR_TEMPLATE
            service.qrPosition == 'right'
    }

    void "config helpers read configured values"() {
        given:
            service.configProvider = Stub(ConfigProvider) {
                get(Boolean, CfgKey.QR.QR_ENABLED.key()) >> true
                get(String, CfgKey.QR.QR_CONTENT_TEMPLATE.key()) >> 'myhab://{entityType}/{id}#x'
                get(String, CfgKey.QR.QR_POSITION.key()) >> 'left'
                get(Integer, CfgKey.QR.QR_SIZE.key()) >> 0
            }

        expect:
            service.isQrEnabled()
            service.qrTemplate == 'myhab://{entityType}/{id}#x'
            service.qrPosition == 'left'

        and: "the GraphQL-facing map exposes the same values + variable list"
            Map cfg = service.qrConfig
            cfg.enabled
            cfg.position == 'left'
            'entityType' in cfg.availableVariables
            'id' in cfg.availableVariables
    }
}
