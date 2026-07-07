/* eslint-env serviceworker */
/*
 * Web Push handlers, imported into the Workbox-generated service worker (sw.js)
 * via `importScripts('push-sw.js')` — see quasar.config.js > pwa >
 * extendGenerateSWOptions. Kept as a standalone script so we can stay on
 * GenerateSW mode (preserving the runtime caching config) while still adding
 * push support.
 *
 * Payload shape (from WebPushService): { id, subject, message, level, fromSender }
 */

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    data = { subject: 'myHAB', message: event.data ? event.data.text() : '' };
  }

  const title = data.subject || 'myHAB';
  const targetUrl = '/messages' + (data.id ? '?id=' + data.id : '');
  const options = {
    body: data.message || '',
    icon: '/icons/icon-192x192.png',
    badge: '/icons/favicon-96x96.png',
    tag: data.id ? 'msg-' + data.id : undefined,
    renotify: !!data.id,
    requireInteraction: data.level === 'ERROR',
    data: { id: data.id, url: targetUrl }
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || '/messages';

  event.waitUntil(
    (async () => {
      const allClients = await self.clients.matchAll({
        type: 'window',
        includeUncontrolled: true
      });
      for (const client of allClients) {
        if ('focus' in client) {
          if ('navigate' in client) {
            try {
              await client.navigate(targetUrl);
            } catch (e) {
              /* cross-navigation may be blocked; focus anyway */
            }
          }
          return client.focus();
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow(targetUrl);
      }
      return undefined;
    })()
  );
});
