package org.myhab.controller

import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.domain.device.DevicePeripheral

import java.util.Base64

/**
 * Proxies go2rtc (the Tuya-WebRTC streaming gateway) under myHAB auth so the
 * browser never holds Tuya secrets or hits go2rtc cross-origin. go2rtc speaks
 * Tuya's proprietary WebRTC-over-MQTT and delivers the REAL doorbell feed — Tuya's
 * cloud HLS only ever serves a black "connecting" placeholder. Kept as REST (GraphQL
 * doesn't serve binary streams; same split as avatars and screen backgrounds):
 * <ul>
 *   <li>GET /api/intercom/$id/snapshot     — a live JPEG frame (go2rtc frame.jpeg)</li>
 *   <li>GET /api/intercom/$id/stream.m3u8  — HLS playlist, segment URIs rewritten
 *       back through this controller so hls.js can carry the JWT on every request</li>
 *   <li>GET /api/intercom/$id/hls?u=...    — one proxied HLS segment</li>
 * </ul>
 * $id is the INTERCOM peripheral id; it resolves to the connected device's code,
 * which is also the go2rtc stream name. When go2rtc isn't configured
 * (intercom.go2rtc.baseUrl unset — e.g. the demo) snapshot returns 204 so the widget
 * degrades to a placeholder.
 */
@Slf4j
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class IntercomController {

    static allowedMethods = [snapshot: 'GET', streamPlaylist: 'GET', hlsSegment: 'GET']

    def configProvider

    def snapshot() {
        String src = deviceCode()
        if (!src) { render(status: 404); return }
        String base = go2rtcBase()
        if (!base) { render(status: 204); return }
        response.setHeader('Cache-Control', 'no-store')
        proxyBytes("${base}/api/frame.jpeg?src=${src}", 'image/jpeg')
    }

    /**
     * GET /api/intercom/$id/stream.m3u8 — go2rtc's HLS playlist, with every segment
     * (and init-segment) URI rewritten to /api/intercom/$id/hls?u=<upstream>, so
     * hls.js fetches segments back through here and its xhrSetup Bearer header
     * authenticates each one.
     */
    def streamPlaylist() {
        String src = deviceCode()
        if (!src) { render(status: 404); return }
        String base = go2rtcBase()
        if (!base) { render(status: 204); return }
        String upstream = "${base}/api/stream.m3u8?src=${src}"
        try {
            HttpURLConnection conn = open(upstream, 15000)
            int status = conn.responseCode
            if (status >= 400) { render(status: 502); return }
            String playlist = conn.inputStream.getText('UTF-8')
            response.setHeader('Cache-Control', 'no-store')
            render(text: rewritePlaylist(playlist, upstream, params.id), contentType: 'application/vnd.apple.mpegurl')
        } catch (Exception ex) {
            log.warn("intercom playlist proxy failed: ${ex.message}")
            render(status: 502)
        }
    }

    /** GET /api/intercom/$id/hls?u=<base64url upstream> — one HLS segment. */
    def hlsSegment() {
        String base = go2rtcBase()
        if (!base) { render(status: 404); return }
        String url
        try {
            url = new String(Base64.urlDecoder.decode(params.u ?: ''), 'UTF-8')
        } catch (Exception ignored) {
            render(status: 400); return
        }
        // SSRF guard: only ever fetch back from the configured go2rtc.
        if (!url.startsWith(base + '/')) { render(status: 403); return }
        try {
            HttpURLConnection conn = open(url, 15000)
            int status = conn.responseCode
            if (status >= 400) { render(status: 502); return }
            String ct = conn.contentType ?: 'video/mp2t'
            response.setHeader('Cache-Control', 'no-store')
            render(file: new ByteArrayInputStream(conn.inputStream.bytes), contentType: ct)
        } catch (Exception ex) {
            log.warn("intercom segment proxy failed: ${ex.message}")
            render(status: 502)
        }
    }

    // -- helpers ---------------------------------------------------------------

    private String rewritePlaylist(String playlist, String upstreamUrl, String id) {
        URI baseUri = new URI(upstreamUrl)
        StringBuilder out = new StringBuilder()
        playlist.eachLine { String line ->
            String trimmed = line.trim()
            if (trimmed.isEmpty()) {
                out.append(line).append('\n')
            } else if (trimmed.startsWith('#')) {
                // Rewrite a URI embedded in a tag (e.g. #EXT-X-MAP:URI="init.mp4").
                out.append(trimmed.contains('URI="') ? rewriteTagUri(line, baseUri, id) : line).append('\n')
            } else {
                out.append(proxied(baseUri.resolve(trimmed).toString(), id)).append('\n')
            }
        }
        return out.toString()
    }

    private String rewriteTagUri(String line, URI baseUri, String id) {
        return line.replaceAll(/URI="([^"]+)"/) { full, uri ->
            "URI=\"${proxied(baseUri.resolve(uri).toString(), id)}\""
        }
    }

    private String proxied(String absoluteUrl, String id) {
        String u = Base64.urlEncoder.withoutPadding().encodeToString(absoluteUrl.getBytes('UTF-8'))
        return "/api/intercom/${id}/hls?u=${u}"
    }

    private String deviceCode() {
        DevicePeripheral peripheral = DevicePeripheral.get(params.long('id'))
        return peripheral?.connectedTo?.find()?.device?.code
    }

    private String go2rtcBase() {
        String base = configProvider.get(String.class, 'intercom.go2rtc.baseUrl')
        return base?.trim() ? base.replaceAll('/+$', '') : null
    }

    private HttpURLConnection open(String url, int readTimeout) {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
        conn.setConnectTimeout(3000)
        conn.setReadTimeout(readTimeout)
        return conn
    }

    private void proxyBytes(String url, String contentType) {
        try {
            HttpURLConnection conn = open(url, 20000)
            int status = conn.responseCode
            if (status == 204) { render(status: 204); return }
            if (status >= 400) { render(status: 502); return }
            render(file: new ByteArrayInputStream(conn.inputStream.bytes), contentType: contentType)
        } catch (Exception ex) {
            log.warn("intercom snapshot proxy failed: ${ex.message}")
            render(status: 502)
        }
    }
}
