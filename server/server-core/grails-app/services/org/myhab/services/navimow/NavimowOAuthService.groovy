package org.myhab.services.navimow

import com.hazelcast.core.HazelcastInstance
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.init.cache.CacheMap

import java.security.SecureRandom

/**
 * Handles the OAuth2 authorization-code flow for the Segway Navimow cloud
 * (a clean-room port of segwaynavimow/NavimowHA {@code auth.py} +
 * {@code config_flow.py}).
 *
 * <p>The flow has two halves:</p>
 * <ol>
 *   <li><b>Start</b> ({@link #startAuthorization}) — generate a random
 *       {@code state} (CSRF), stash {@code state → deviceId} in Hazelcast with
 *       a 10-minute TTL, and build the full authorize URL the UI should open
 *       in a browser tab. The URL points at Navimow's hosted login page
 *       (default {@code navimow-h5-fra.willand.com}); the user signs in there
 *       and Navimow redirects back with {@code ?code=…&state=…}.</li>
 *   <li><b>Callback</b> ({@link #handleCallback}) — invoked by
 *       {@code NavimowOAuthController}. Validates {@code state}, exchanges
 *       {@code code} for an access token at Navimow's token endpoint, and
 *       persists the token (plus refresh_token when present) onto the device's
 *       {@link Configuration} sidecar so the existing
 *       {@link NavimowApiClient}/{@code NavimowInfoSyncJob} stack can read it
 *       without further changes.</li>
 * </ol>
 *
 * <p>Defaults (client_id, client_secret, authorize URL, token URL, base URL,
 * callback path) come from {@code ConfigProvider}; each can be overridden via
 * an entry in the GIT-backed yaml. The defaults below match what's publicly
 * shipped in the Home Assistant integration (the {@code client_id="homeassistant"}
 * + secret are visible in the open NavimowHA repo, so there's no leak — they
 * just authenticate us as "the HA client" from Navimow's perspective).</p>
 */
@Slf4j
@Transactional
class NavimowOAuthService {

    static final String DEFAULT_AUTHORIZE_URL = 'https://navimow-h5-fra.willand.com/smartHome/login'
    static final String DEFAULT_TOKEN_URL = 'https://navimow-fra.ninebot.com/openapi/oauth/getAccessToken'
    static final String DEFAULT_API_BASE_URL = 'https://navimow-fra.ninebot.com'
    static final String DEFAULT_CLIENT_ID = 'homeassistant'
    static final String DEFAULT_CLIENT_SECRET = '57056e15-722e-42be-bbaa-b0cbfb208a52'
    // Mirrors HA's OAuth callback path; gives the best chance Navimow's
    // (likely) redirect_uri whitelist accepts our request.
    static final String DEFAULT_CALLBACK_PATH = '/auth/external/callback'
    static final long STATE_TTL_MS = 10L * 60L * 1000L
    private static final SecureRandom RNG = new SecureRandom()

    HazelcastInstance hazelcastInstance
    def configProvider

    /**
     * Begin an OAuth flow for the given Navimow Device.
     *
     * @return absolute URL the UI must open in a new browser tab. The user
     *         logs in there; Navimow redirects to our callback, which finishes
     *         the exchange and saves the token.
     */
    String startAuthorization(Long deviceId, String myhabBaseUrl) {
        Device device = Device.get(deviceId)
        if (device == null) {
            throw new NavimowApiException("navimowOAuthStart: Device ${deviceId} not found")
        }
        if (device.model != DeviceModel.NAVIMOW_SEGWAY) {
            throw new NavimowApiException("navimowOAuthStart: Device ${deviceId} is not a NAVIMOW_SEGWAY (model=${device.model})")
        }
        if (!myhabBaseUrl?.trim()) {
            throw new NavimowApiException('navimowOAuthStart: callerBaseUrl is required to build redirect_uri')
        }

        String state = randomState()
        long expireOn = System.currentTimeMillis() + STATE_TTL_MS
        hazelcastInstance.getMap(CacheMap.OAUTH_STATE.name).put(state, [
                deviceId: deviceId,
                expireOn: expireOn,
                callbackBaseUrl: myhabBaseUrl,
        ])

        String authorizeUrl = cfg('navimow.oauth.authorize_url', DEFAULT_AUTHORIZE_URL)
        String clientId = cfg('navimow.oauth.client_id', DEFAULT_CLIENT_ID)
        String redirectUri = buildRedirectUri(myhabBaseUrl)

        // Standard OAuth2 authorization-code params + the channel=homeassistant
        // hint that Navimow's login UI keys off. Scope is left empty (the HA
        // integration doesn't set one either; Navimow appears to issue a
        // default scope for the homeassistant client).
        StringBuilder sb = new StringBuilder(authorizeUrl)
        sb.append(authorizeUrl.contains('?') ? '&' : '?')
        sb.append("response_type=code")
        sb.append("&client_id=").append(urlEncode(clientId))
        sb.append("&redirect_uri=").append(urlEncode(redirectUri))
        sb.append("&state=").append(urlEncode(state))
        sb.append("&channel=homeassistant")
        String url = sb.toString()
        log.info("Navimow OAuth start: device=${deviceId} state=${state.take(8)}… redirect=${redirectUri}")
        return url
    }

    /**
     * Finish an OAuth flow. Returns a status map suitable for the controller
     * to render the success/failure HTML.
     *
     * @return {@code [success: Boolean, error: String, deviceId: Long, deviceCode: String]}
     */
    Map handleCallback(String code, String state) {
        if (!code?.trim()) return [success: false, error: 'Missing authorization code']
        if (!state?.trim()) return [success: false, error: 'Missing state']

        Map entry = (Map) hazelcastInstance.getMap(CacheMap.OAUTH_STATE.name).remove(state)
        if (entry == null) {
            log.warn("Navimow OAuth callback: unknown or expired state ${state.take(8)}…")
            return [success: false, error: 'OAuth state expired or invalid — please retry from the device page']
        }
        long expireOn = (entry.expireOn ?: 0L) as long
        if (expireOn < System.currentTimeMillis()) {
            return [success: false, error: 'OAuth state expired — please retry from the device page']
        }

        Long deviceId = entry.deviceId as Long
        Device device = Device.get(deviceId)
        if (device == null) {
            return [success: false, error: "Device ${deviceId} no longer exists"]
        }
        String myhabBaseUrl = entry.callbackBaseUrl as String

        // Exchange the code for an access token via standard OAuth2.
        Map tokenResponse
        try {
            tokenResponse = exchangeCode(code, buildRedirectUri(myhabBaseUrl))
        } catch (Exception ex) {
            log.error("Navimow OAuth token exchange failed for device=${deviceId}: ${ex.message}", ex)
            return [success: false, error: "Token exchange failed: ${ex.message}"]
        }

        String accessToken = (tokenResponse.access_token
                ?: tokenResponse.accessToken
                ?: tokenResponse.token) as String
        String refreshToken = (tokenResponse.refresh_token ?: tokenResponse.refreshToken) as String
        if (!accessToken) {
            return [success: false, error: "Token endpoint returned no access_token (body: ${tokenResponse})"]
        }

        try {
            upsertConfig(device, CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key(), accessToken)
            if (refreshToken) {
                upsertConfig(device, CfgKey.DEVICE.DEVICE_OAUTH_REFRESH_TOKEN.key(), refreshToken)
            }
            // Auto-populate base_url if the device doesn't have one yet — gives
            // first-time users a one-click setup (token + base URL together).
            if (!readConfig(device, CfgKey.DEVICE.DEVICE_NAVIMOW_API_BASE_URL.key())) {
                upsertConfig(device, CfgKey.DEVICE.DEVICE_NAVIMOW_API_BASE_URL.key(),
                        cfg('navimow.api.base_url', DEFAULT_API_BASE_URL))
            }
        } catch (Exception ex) {
            log.error("Navimow OAuth: failed to persist token for device=${deviceId}: ${ex.message}", ex)
            return [success: false, error: "Stored token failed to save: ${ex.message}"]
        }

        log.info("Navimow OAuth success: device=${deviceId} (${device.code}) token=${accessToken.take(12)}… refresh=${refreshToken ? 'yes' : 'no'}")
        return [success: true, deviceId: deviceId, deviceCode: device.code]
    }

    // ----------------------------------------------------------------------

    private Map exchangeCode(String code, String redirectUri) {
        String tokenUrl = cfg('navimow.oauth.token_url', DEFAULT_TOKEN_URL)
        String clientId = cfg('navimow.oauth.client_id', DEFAULT_CLIENT_ID)
        String clientSecret = cfg('navimow.oauth.client_secret', DEFAULT_CLIENT_SECRET)

        HttpResponse<String> response = Unirest.post(tokenUrl)
                .header('Content-Type', 'application/x-www-form-urlencoded')
                .header('Accept', 'application/json')
                .field('grant_type', 'authorization_code')
                .field('client_id', clientId)
                .field('client_secret', clientSecret)
                .field('code', code)
                .field('redirect_uri', redirectUri)
                .asString()

        if (response.status >= 400) {
            throw new NavimowApiException("Token endpoint HTTP ${response.status}: ${response.body}")
        }
        Map body = new JsonSlurper().parseText(response.body ?: '{}') as Map
        // Segway sometimes wraps responses in {code,data,desc}; unwrap if needed.
        if (body.code != null && body.data instanceof Map) {
            if ((body.code as Integer) != 1) {
                throw new NavimowApiException("Token endpoint envelope code=${body.code} desc=${body.desc}")
            }
            return body.data as Map
        }
        return body
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s ?: '', 'UTF-8')
    }

    private static String buildRedirectUri(String baseUrl) {
        return "${baseUrl.replaceAll(/\/+$/, '')}${DEFAULT_CALLBACK_PATH}"
    }

    private static String randomState() {
        byte[] buf = new byte[24]
        RNG.nextBytes(buf)
        return Base64.urlEncoder.withoutPadding().encodeToString(buf)
    }

    private String cfg(String key, String defaultValue) {
        try {
            String v = configProvider?.get(String.class, key)
            return v?.trim() ? v.trim() : defaultValue
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    private static String readConfig(Device device, String cfgKey) {
        Configuration row = Configuration.where {
            entityId == device.id && entityType == EntityType.DEVICE && key == cfgKey
        }.find()
        String v = row?.value
        return v?.trim() ? v.trim() : null
    }

    private static void upsertConfig(Device device, String cfgKey, String value) {
        Configuration row = Configuration.where {
            entityId == device.id && entityType == EntityType.DEVICE && key == cfgKey
        }.find()
        if (row == null) {
            row = new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: cfgKey)
        }
        row.value = value
        row.save(flush: true, failOnError: true)
    }
}
