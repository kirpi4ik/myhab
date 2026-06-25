package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Mints a short-lived Google OAuth2 access token from a service-account JSON
 * key, using the JWT-bearer grant. Cloud Text-to-Speech does not accept API
 * keys ("API keys are not supported by this API"), so this is the supported
 * way to authenticate.
 *
 * <p>Pure JDK (RS256 via {@code java.security}) — no extra dependency. Tokens
 * are cached per service-account email and refreshed a minute before expiry.
 * The token exchange uses the isolated {@link VoiceHttp} instance.</p>
 */
@Slf4j
class GoogleAuth {

    private static final Object LOCK = new Object()
    private static String cachedEmail
    private static String cachedToken
    private static long cachedExpiryEpochSec = 0

    /**
     * Return a valid access token for the given credential, minting/caching as
     * needed. The credential may be the service-account JSON itself (starts with
     * '{') or a path to the downloaded JSON key file on the server.
     */
    static String accessToken(String credential) {
        Map sa = new JsonSlurper().parseText(resolveJson(credential)) as Map
        String email = sa.client_email
        String tokenUri = (sa.token_uri ?: 'https://oauth2.googleapis.com/token') as String

        synchronized (LOCK) {
            long now = System.currentTimeMillis() / 1000L
            if (cachedToken && email == cachedEmail && now < cachedExpiryEpochSec - 60) {
                return cachedToken
            }
            String jwt = buildJwt(email, sa.private_key as String, tokenUri, now)
            Map tokenResponse = exchange(tokenUri, jwt)
            cachedToken = tokenResponse.access_token as String
            cachedEmail = email
            long expiresIn = (tokenResponse.expires_in ?: 3600) as long
            if (expiresIn <= 0) expiresIn = 3600
            cachedExpiryEpochSec = now + expiresIn
            return cachedToken
        }
    }

    /** Accept inline service-account JSON or a path to the JSON key file. */
    private static String resolveJson(String credential) {
        String c = credential?.trim()
        if (!c) {
            throw new IllegalStateException('Google TTS service account is not set')
        }
        if (c.startsWith('{')) {
            return c
        }
        File f = new File(c)
        if (f.isFile()) {
            return f.getText('UTF-8')
        }
        throw new IllegalStateException(
                'Google TTS credential must be a service account JSON (starting with "{") or a path to the JSON key file — got neither (API keys are not supported)')
    }

    private static String buildJwt(String email, String privateKeyPem, String aud, long now) {
        String header = b64url(JsonOutput.toJson([alg: 'RS256', typ: 'JWT']).bytes)
        String claims = b64url(JsonOutput.toJson([
            iss  : email,
            scope: 'https://www.googleapis.com/auth/cloud-platform',
            aud  : aud,
            iat  : now,
            exp  : now + 3600
        ]).bytes)
        String signingInput = "${header}.${claims}"
        return "${signingInput}.${b64url(sign(signingInput.getBytes('UTF-8'), privateKeyPem))}"
    }

    private static byte[] sign(byte[] data, String privateKeyPem) {
        String clean = privateKeyPem
                .replace('-----BEGIN PRIVATE KEY-----', '')
                .replace('-----END PRIVATE KEY-----', '')
                .replaceAll('\\s', '')
        byte[] der = Base64.decoder.decode(clean)
        def key = KeyFactory.getInstance('RSA').generatePrivate(new PKCS8EncodedKeySpec(der))
        Signature sig = Signature.getInstance('SHA256withRSA')
        sig.initSign(key)
        sig.update(data)
        return sig.sign()
    }

    private static Map exchange(String tokenUri, String jwt) {
        HttpResponse<String> response
        try {
            response = VoiceHttp.INSTANCE.post(tokenUri)
                    .header('content-type', 'application/x-www-form-urlencoded')
                    .field('grant_type', 'urn:ietf:params:oauth:grant-type:jwt-bearer')
                    .field('assertion', jwt)
                    .asString()
        } catch (Exception ex) {
            throw new IllegalStateException("Google token exchange transport error: ${ex.message}", ex)
        }
        if (response.status >= 400) {
            log.warn("Google token exchange HTTP ${response.status}: ${response.body}")
            throw new IllegalStateException("Google token exchange HTTP ${response.status}")
        }
        return new JsonSlurper().parseText(response.body ?: '{}') as Map
    }

    private static String b64url(byte[] bytes) {
        Base64.urlEncoder.withoutPadding().encodeToString(bytes)
    }
}
