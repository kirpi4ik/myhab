package org.myhab.services

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import nl.martijndwars.webpush.Encoding
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.apache.http.HttpResponse
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.myhab.config.ConfigProvider
import org.myhab.domain.PushSubscription
import org.myhab.domain.User
import org.myhab.domain.UserMessage

import java.nio.charset.StandardCharsets
import java.security.Security
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Delivers {@link UserMessage} rows as Web Push (VAPID) notifications so they
 * surface as native OS notifications in the PWA — even when the app is closed.
 *
 * <p>Mirrors the graceful Telegram {@code sendNotification} pattern: build →
 * send → log, <b>never throw</b>, so a push failure can't break the message
 * producer. Sends run off the caller thread (a small pool) so a slow browser
 * push endpoint never blocks {@code NotificationService.notify}. Subscriptions
 * the push service reports as gone (HTTP 404/410) are pruned.</p>
 *
 * <p>The VAPID key pair lives in the git-backed config
 * ({@code push.vapid.publicKey} / {@code push.vapid.privateKey} /
 * {@code push.vapid.subject}). When keys are absent the feature is simply
 * inactive (a single warning is logged).</p>
 */
@Slf4j
class WebPushService {

    static transactional = false

    ConfigProvider configProvider

    private final ExecutorService executor = Executors.newFixedThreadPool(2)
    private PushService pushService
    private boolean warnedMissingKeys = false

    /** VAPID public key (base64url) for the client's {@code applicationServerKey}. */
    String getPublicKey() {
        return configProvider.get(String, 'push.vapid.publicKey')
    }

    /**
     * Fire-and-forget: push {@code um} to every subscription of {@code user}.
     * Returns immediately; delivery + pruning happen on a background thread.
     */
    void sendToUser(User user, UserMessage um) {
        if (user == null || um == null) return
        PushService svc = pushService()
        if (svc == null) return

        Long userId = user.id
        String payload = JsonOutput.toJson([
                id        : um.id,
                subject   : um.subject,
                message   : um.message,
                level     : um.level?.name(),
                fromSender: um.fromSender
        ])

        executor.submit({ deliver(svc, userId, payload) } as Runnable)
    }

    private void deliver(PushService svc, Long userId, String payload) {
        try {
            PushSubscription.withNewTransaction {
                List<PushSubscription> subs = PushSubscription.where { user.id == userId }.list()
                byte[] body = payload.getBytes(StandardCharsets.UTF_8)
                subs.each { PushSubscription sub ->
                    try {
                        Notification notification = new Notification(
                                sub.endpoint, toBase64Url(sub.p256dhKey), toBase64Url(sub.authKey), body)
                        // AES128GCM (RFC 8291), not the library's AESGCM default: that is the
                        // superseded draft-04 scheme and Safari/iOS refuses to decrypt it.
                        HttpResponse resp = svc.send(notification, Encoding.AES128GCM)
                        int status = resp.statusLine.statusCode
                        if (status == 404 || status == 410) {
                            log.info("Pruning expired push subscription id=${sub.id} (HTTP ${status})")
                            sub.delete()
                        } else if (status >= 400) {
                            log.warn("Push send failed for subscription id=${sub.id}: HTTP ${status}")
                        }
                    } catch (Exception ex) {
                        log.warn("Push send error for subscription id=${sub.id}: ${ex.message}")
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Web push delivery failed for user=${userId}: ${ex.message}", ex)
        }
    }

    /**
     * Translate a subscription key into the base64url alphabet the push library requires:
     * it decodes p256dh/auth with {@code Base64.getUrlDecoder()}, which throws on the '+'
     * and '/' that a browser's {@code btoa()} produces. Applied on read rather than by
     * migrating the table, so rows written by older clients keep working.
     */
    static String toBase64Url(String key) {
        return key == null ? null : key.replace('+', '-').replace('/', '_')
    }

    private synchronized PushService pushService() {
        if (pushService != null) return pushService
        String pub = configProvider.get(String, 'push.vapid.publicKey')
        String priv = configProvider.get(String, 'push.vapid.privateKey')
        // No product default: RFC 8292 wants a contact the push service can reach for
        // this deployment, and inventing one points complaints at someone else.
        String subject = configProvider.get(String, 'push.vapid.subject') ?: 'mailto:admin@localhost'
        if (!pub || !priv) {
            if (!warnedMissingKeys) {
                log.warn('Web push inactive: push.vapid.publicKey/privateKey not configured')
                warnedMissingKeys = true
            }
            return null
        }
        try {
            if (Security.getProvider('BC') == null) {
                Security.addProvider(new BouncyCastleProvider())
            }
            pushService = new PushService(pub, priv, subject)
            log.info('Web push service initialized')
            return pushService
        } catch (Exception ex) {
            log.error("Failed to initialize web push service: ${ex.message}", ex)
            return null
        }
    }
}
