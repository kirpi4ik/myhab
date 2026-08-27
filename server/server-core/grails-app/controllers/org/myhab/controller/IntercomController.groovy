package org.myhab.controller

import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.domain.device.DevicePeripheral

/**
 * Proxies the intercom cloud helper (bridges/intercom-cloud) under myHAB auth so
 * the browser never holds Tuya secrets or hits Tuya cross-origin (kept as REST —
 * GraphQL doesn't serve binary streams; same split as avatars and screen
 * backgrounds):
 * <ul>
 *   <li>GET /api/intercom/$id/snapshot[?live=1] — last event JPEG, or a live grab</li>
 *   <li>GET /api/intercom/$id/stream            — { hls, expires } for the popup</li>
 * </ul>
 * $id is the INTERCOM peripheral id; it resolves to the connected device's code,
 * which the helper maps to a Tuya device. When the helper isn't configured
 * (intercom.cloud.baseUrl unset — e.g. the demo) both return 204 so the widget
 * degrades to a placeholder.
 */
@Slf4j
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class IntercomController {

    static allowedMethods = [snapshot: 'GET', stream: 'GET']

    def configProvider

    def snapshot() {
        String code = deviceCode()
        if (!code) { render(status: 404); return }
        String base = helperBase()
        if (!base) { render(status: 204); return }
        boolean live = params.boolean('live') ?: false
        proxyBytes("${base}/snapshot.jpg?device=${code}${live ? '&live=1' : ''}", 'image/jpeg')
    }

    def stream() {
        String code = deviceCode()
        if (!code) { render(status: 404); return }
        String base = helperBase()
        if (!base) { render(status: 204); return }
        proxyJson("${base}/stream?device=${code}")
    }

    private String deviceCode() {
        DevicePeripheral peripheral = DevicePeripheral.get(params.long('id'))
        return peripheral?.connectedTo?.find()?.device?.code
    }

    private String helperBase() {
        String base = configProvider.get(String.class, 'intercom.cloud.baseUrl')
        return base?.trim() ? base.replaceAll('/+$', '') : null
    }

    private void proxyBytes(String url, String contentType) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
            conn.setConnectTimeout(3000)
            conn.setReadTimeout(20000)
            int status = conn.responseCode
            if (status == 204) { render(status: 204); return }
            if (status >= 400) { render(status: 502); return }
            byte[] data = conn.inputStream.bytes
            response.setHeader('Cache-Control', 'no-store')
            render(file: new ByteArrayInputStream(data), contentType: contentType)
        } catch (Exception ex) {
            log.warn("intercom snapshot proxy failed: ${ex.message}")
            render(status: 502)
        }
    }

    private void proxyJson(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
            conn.setConnectTimeout(3000)
            conn.setReadTimeout(15000)
            int status = conn.responseCode
            if (status == 204) { render(status: 204); return }
            if (status >= 400) { render(status: 502); return }
            render(text: conn.inputStream.getText('UTF-8'), contentType: 'application/json')
        } catch (Exception ex) {
            log.warn("intercom stream proxy failed: ${ex.message}")
            render(status: 502)
        }
    }
}
