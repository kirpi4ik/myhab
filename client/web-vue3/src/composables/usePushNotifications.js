/**
 * Web Push (VAPID) subscription lifecycle for native OS notifications.
 *
 * Bridges the browser PushManager and the backend push GraphQL:
 *   enable()  → request permission → subscribe → pushSubscribe mutation
 *   disable() → unsubscribe locally → pushUnsubscribe mutation
 *
 * Notifications themselves are rendered by the service worker (public/push-sw.js),
 * so they arrive even when the app/tab is closed. Must be called from a user
 * gesture (browser requirement for Notification.requestPermission).
 */
import { ref } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
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

function keyToBase64(subscription, name) {
  const key = subscription.getKey(name);
  if (!key) return '';
  return window.btoa(String.fromCharCode.apply(null, new Uint8Array(key)));
}

export function usePushNotifications() {
  const { client } = useApolloClient();

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
  };

  const enable = async () => {
    if (!isSupported) return false;
    busy.value = true;
    try {
      const result = await Notification.requestPermission();
      permission.value = result;
      if (result !== 'granted') return false;

      const { data } = await client.query({
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

      await client.mutate({
        mutation: PUSH_SUBSCRIBE,
        variables: {
          endpoint: sub.endpoint,
          p256dh: keyToBase64(sub, 'p256dh'),
          auth: keyToBase64(sub, 'auth'),
          userAgent: navigator.userAgent
        }
      });

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
        await client.mutate({
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
