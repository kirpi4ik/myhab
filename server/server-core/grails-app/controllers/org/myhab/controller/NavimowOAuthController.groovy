package org.myhab.controller

import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.services.navimow.NavimowOAuthService

/**
 * Browser-facing callback endpoint for the Navimow OAuth2 authorization-code
 * flow.
 *
 * <p>The path {@code /auth/external/callback} intentionally mirrors the URL
 * shape Home Assistant uses, because Navimow's OAuth server is expected to
 * validate the {@code redirect_uri} against a per-{@code client_id} whitelist.
 * We use the same {@code client_id="homeassistant"} as the open-source
 * NavimowHA component (its credentials are public), so the closer our
 * redirect path looks to HA's, the better the chance of being accepted.</p>
 *
 * <p>This endpoint is always unauthenticated — the user signing in to Navimow
 * isn't logged into myHAB at the moment Navimow redirects them back. CSRF
 * protection comes from the random {@code state} param that
 * {@link NavimowOAuthService#startAuthorization} stashes in Hazelcast and the
 * controller validates here.</p>
 */
@Slf4j
@Secured(['permitAll'])
class NavimowOAuthController {

    static allowedMethods = [callback: 'GET']

    NavimowOAuthService navimowOAuthService

    /**
     * GET /auth/external/callback?code=…&state=…
     *
     * <p>Renders a self-contained HTML page that:</p>
     * <ul>
     *   <li>Shows the user the outcome (success or specific error).</li>
     *   <li>On success, posts a {@code window.opener.postMessage}
     *       (origin = same as the popup) so the parent DeviceEdit view can
     *       refresh its data + close the popup automatically.</li>
     * </ul>
     */
    def callback() {
        String code = params.code
        String state = params.state
        String oauthError = params.error
        if (oauthError) {
            log.warn("Navimow OAuth callback returned upstream error: ${oauthError} (${params.error_description})")
            render contentType: 'text/html', text: renderResult([
                    success: false,
                    error  : "Navimow rejected the login: ${oauthError}${params.error_description ? ' — ' + params.error_description : ''}"
            ])
            return
        }

        Map result = navimowOAuthService.handleCallback(code, state)
        render contentType: 'text/html', text: renderResult(result)
    }

    /**
     * Build the result HTML page. Kept inline (no GSP) because this view is
     * trivial, ephemeral, and we want it to render even if the user has
     * stripped the resource manifest.
     */
    private static String renderResult(Map result) {
        boolean ok = result.success as boolean
        String icon = ok ? '✔' : '✖'
        String title = ok ? 'Navimow account connected' : 'Navimow connection failed'
        String body = ok
                ? "Device <code>${escape(result.deviceCode)}</code> now has an access token. You can close this window."
                : "<strong>Error:</strong> ${escape(result.error)}"
        String color = ok ? '#0a7' : '#c33'

        return """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <title>${escape(title)}</title>
  <style>
    body { font-family: -apple-system, system-ui, sans-serif; max-width: 480px; margin: 80px auto; padding: 24px; color: #222; }
    .card { border: 1px solid #ddd; border-radius: 12px; padding: 32px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,.05); }
    .icon { font-size: 48px; color: ${color}; line-height: 1; margin-bottom: 8px; }
    h1 { margin: 0 0 16px; font-size: 20px; }
    p { margin: 0; line-height: 1.5; color: #444; }
    code { background: #f3f3f5; padding: 1px 6px; border-radius: 4px; }
    button { margin-top: 24px; padding: 8px 16px; border: 0; border-radius: 6px; background: #444; color: white; cursor: pointer; }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">${icon}</div>
    <h1>${escape(title)}</h1>
    <p>${body}</p>
    <button onclick="window.close()">Close</button>
  </div>
  <script>
    try {
      if (window.opener) {
        window.opener.postMessage({
          type: 'navimow-oauth',
          success: ${ok},
          deviceId: ${result.deviceId ?: 'null'},
          error: ${ok ? 'null' : '"' + escape(result.error).replace('"', '\\\\"') + '"'}
        }, window.location.origin);
      }
    } catch (e) { /* parent on a different origin — user just sees the page */ }
    ${ok ? 'setTimeout(function(){ try{ window.close(); }catch(e){} }, 1500);' : ''}
  </script>
</body>
</html>"""
    }

    private static String escape(Object value) {
        if (value == null) return ''
        return value.toString()
                .replace('&', '&amp;')
                .replace('<', '&lt;')
                .replace('>', '&gt;')
                .replace('"', '&quot;')
                .replace("'", '&#39;')
    }
}
