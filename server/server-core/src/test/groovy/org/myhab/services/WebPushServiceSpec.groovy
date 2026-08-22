package org.myhab.services

import nl.martijndwars.webpush.Notification
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.ECGenParameterSpec

/**
 * Encoding contract with nl.martijndwars:web-push. The library decodes a subscription's
 * p256dh/auth with {@code Base64.getUrlDecoder()}, so a key carrying '+' or '/' — exactly
 * what a browser's {@code btoa()} emits, and what rows written before the client was fixed
 * still hold — is rejected outright. That failure is swallowed per-subscription in
 * {@code deliver()}, which is why it can silence push delivery without anything looking broken.
 */
class WebPushServiceSpec extends Specification {

    private static final String ENDPOINT = 'https://push.example.com/send/abc'

    void setupSpec() {
        if (Security.getProvider('BC') == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    void "keys are translated into the base64url alphabet"() {
        expect:
            WebPushService.toBase64Url('a+b/c=') == 'a-b_c='
            WebPushService.toBase64Url('a-b_c=') == 'a-b_c='
            WebPushService.toBase64Url(null) == null
    }

    void "a key stored as standard base64 only reaches the push library once translated"() {
        given: 'a real P-256 subscription key whose standard base64 carries + or /'
            byte[] raw = publicKeyWithNonUrlSafeBase64()
            String storedKey = Base64.encoder.encodeToString(raw)
            String storedAuth = Base64.encoder.encodeToString(new byte[16])

        when: 'it is handed to the library as-is, the way the service used to'
            new Notification(ENDPOINT, storedKey, storedAuth, 'payload'.bytes)

        then:
            thrown(IllegalArgumentException)

        when: 'it is translated first'
            new Notification(ENDPOINT,
                    WebPushService.toBase64Url(storedKey),
                    WebPushService.toBase64Url(storedAuth),
                    'payload'.bytes)

        then:
            noExceptionThrown()
    }

    /** A P-256 public key is 65 bytes, so ~93% of them encode with a '+' or a '/'. */
    private static byte[] publicKeyWithNonUrlSafeBase64() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance('ECDH', 'BC')
        generator.initialize(new ECGenParameterSpec('prime256v1'))
        for (int i = 0; i < 200; i++) {
            byte[] raw = ((ECPublicKey) generator.generateKeyPair().public).q.getEncoded(false)
            String encoded = Base64.encoder.encodeToString(raw)
            if (encoded.contains('+') || encoded.contains('/')) {
                return raw
            }
        }
        throw new IllegalStateException('no P-256 key with a non-url-safe base64 encoding in 200 tries')
    }
}
