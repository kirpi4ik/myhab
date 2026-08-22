/**
 * Web Push (VAPID) subscription lifecycle for native OS notifications.
 *
 * Bridges the browser PushManager and the backend push GraphQL:
 *   enable()  → request permission → subscribe → pushSubscribe mutation
 *   disable() → unsubscribe locally → pushUnsubscribe mutation
 *   syncPushSubscription() → re-register the existing subscription (called on app start)
 *
 * Notifications themselves are rendered by the service worker (public/push-sw.js),
 * so they arrive even when the app/tab is closed. Must be called from a user
 * gesture (browser requirement for Notification.requestPermission).
 */
import { ref } from 'vue';
import { apolloClient } from '@/boot/graphql';
import { authzService } from '@/_services';
import { PUSH_PUBLIC_KEY, PUSH_SUBSCRIBE, PUSH_UNSUBSCRIBE } from '@/graphql/queries';

const isSupported =
  typeof window !== 'undefined' &&
  process.env.MODE === 'pwa' && // a service worker is only registered in PWA builds
  'serviceWorker' in navigator &&
  'PushManager' in window &&
  'Notification' in window;

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = window.atob(base64);
  const output = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; ++i) {
    output[i] = raw.charCodeAt(i);
  }
  return output;
}

/**
 * Subscription keys must travel in the base64url alphabet (RFC 8291): the server decodes
 * them with Base64.getUrlDecoder(), which rejects the '+' and '/' that btoa() emits.
 */
function keyToBase64Url(subscription, name) {
  const key = subscription.getKey(name);
  if (!key) return '';
  return window
    .btoa(String.fromCharCode.apply(null, new Uint8Array(key)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

/** Register (upsert by endpoint) one browser subscription with the backend. */
function sendSubscription(subscription) {
  return apolloClient.mutate({
    mutation: PUSH_SUBSCRIBE,
    variables: {
      endpoint: subscription.endpoint,
      p256dh: keyToBase64Url(subscription, 'p256dh'),
      auth: keyToBase64Url(subscription, 'auth'),
      userAgent: navigator.userAgent
    }
  });
}

/**
 * Re-register the browser's current subscription with the backend, if there is one.
 *
 * The browser holding a subscription is no proof the server still has the matching row:
 * it is pruned when a push endpoint reports HTTP 410, and the service worker replaces it
 * with a fresh endpoint after a `pushsubscriptionchange`. Without this the toggle keeps
 * reading "on" while the server has nothing to deliver to. The mutation is an upsert by
 * endpoint, so repeating it on every app start is free.
 *
 * Standalone (not part of the composable) so app boot can call it outside a component.
 */
export async function syncPushSubscription() {
  if (!isSupported) return false;
  if (Notification.permission !== 'granted') return false;
  if (!authzService.currentUserValue?.access_token) return false;
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    if (!sub) return false;
    await sendSubscription(sub);
    return true;
  } catch (e) {
    console.warn('Failed to re-register push subscription with the server', e);
    return false;
  }
}

export function usePushNotifications() {
  const supported = ref(isSupported);
  const permission = ref(isSupported ? Notification.permission : 'denied');
  const subscribed = ref(false);
  const busy = ref(false);

  const getRegistration = async () => {
    if (!isSupported) return null;
    return navigator.serviceWorker.ready;
  };

  const refresh = async () => {
    if (!isSupported) return;
    permission.value = Notification.permission;
    try {
      const reg = await getRegistration();
      const sub = reg ? await reg.pushManager.getSubscription() : null;
      subscribed.value = !!sub;
    } catch {
      subscribed.value = false;
    }
    // A local subscription may no longer be known to the server — re-register it.
    await syncPushSubscription();
  };

  const enable = async () => {
    if (!isSupported) return false;
    busy.value = true;
    try {
      const result = await Notification.requestPermission();
      permission.value = result;
      if (result !== 'granted') return false;

      const { data } = await apolloClient.query({
        query: PUSH_PUBLIC_KEY,
        fetchPolicy: 'network-only'
      });
      const vapidKey = data?.pushPublicKey;
      if (!vapidKey) {
        console.warn('Push not configured on the server (empty VAPID public key)');
        return false;
      }

      const reg = await getRegistration();
      let sub = await reg.pushManager.getSubscription();
      if (!sub) {
        sub = await reg.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(vapidKey)
        });
      }

      await sendSubscription(sub);

      subscribed.value = true;
      return true;
    } catch (e) {
      console.error('Failed to enable push notifications', e);
      return false;
    } finally {
      busy.value = false;
    }
  };

  const disable = async () => {
    if (!isSupported) return;
    busy.value = true;
    try {
      const reg = await getRegistration();
      const sub = reg ? await reg.pushManager.getSubscription() : null;
      if (sub) {
        const endpoint = sub.endpoint;
        await sub.unsubscribe();
        await apolloClient.mutate({
          mutation: PUSH_UNSUBSCRIBE,
          variables: { endpoint }
        });
      }
      subscribed.value = false;
    } catch (e) {
      console.error('Failed to disable push notifications', e);
    } finally {
      busy.value = false;
    }
  };

  return { supported, permission, subscribed, busy, refresh, enable, disable };
}
