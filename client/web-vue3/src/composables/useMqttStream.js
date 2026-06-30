import { onMounted, onUnmounted, watch } from 'vue';
import { useWebSocketStore } from '@/store/websocket.store';

/**
 * Subscribe to the live raw-MQTT feed broadcast by the backend on
 * `/topic/mqtt`. The subscription is created on mount and torn down on unmount,
 * so raw traffic only flows to clients that are actually viewing a monitor
 * (Explorer / PortView). It also (re)subscribes whenever the websocket
 * connection (re)enters the 'ONLINE' state.
 *
 * @param {(msg: {topic: string, payload: string, ts: number, deviceCode: ?string, portType: ?string, portCode: ?string}) => void} callback
 * @param {object} [options]
 * @param {(msg) => boolean} [options.filter] Optional predicate; callback runs only when it returns true.
 * @returns {{ wsStore: ReturnType<typeof useWebSocketStore> }}
 */
export function useMqttStream(callback, options = {}) {
  const wsStore = useWebSocketStore();
  let subscription = null;

  const handle = (msg) => {
    if (options.filter && !options.filter(msg)) return;
    callback(msg);
  };

  const subscribe = () => {
    if (subscription) return;
    subscription = wsStore.subscribe('/topic/mqtt', handle);
  };

  const unsubscribe = () => {
    if (subscription) {
      try {
        subscription.unsubscribe();
      } catch (e) {
        /* connection already gone */
      }
      subscription = null;
    }
  };

  // Re-subscribe when the connection comes (back) online; drop the stale handle
  // when it goes offline so a fresh subscribe happens on reconnect.
  watch(
    () => wsStore.connection,
    (state) => {
      if (state === 'ONLINE') subscribe();
      else unsubscribe();
    }
  );

  onMounted(subscribe);
  onUnmounted(unsubscribe);

  return { wsStore };
}

export default useMqttStream;
