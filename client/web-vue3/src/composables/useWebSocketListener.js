import { watch } from 'vue';
import { useWebSocketStore } from '@/store/websocket.store';

/**
 * Composable for listening to WebSocket messages by event name
 * Automatically parses JSON payload and calls callback when event matches
 * 
 * @param {string} eventName - The event name to listen for (e.g., 'evt_port_value_persisted')
 * @param {function} callback - Callback function that receives the parsed payload
 * @param {object} options - Optional configuration
 * @param {function} options.filter - Optional filter function to determine if callback should run
 * @returns {object} - Object containing wsStore for additional access if needed
 * 
 * @example
 * // Basic usage
 * useWebSocketListener('evt_port_value_persisted', (payload) => {
 *   console.log('Port updated:', payload);
 * });
 * 
 * @example
 * // With filter
 * useWebSocketListener('evt_port_value_persisted', (payload) => {
 *   loadDetails();
 * }, {
 *   filter: (payload) => payload.p2 === portId
 * });
 */
export function useWebSocketListener(eventName, callback, options = {}) {
  const wsStore = useWebSocketStore();

  watch(
    () => wsStore.ws.message,
    (message) => {
      if (message?.eventName === eventName) {
        try {
          const payload = JSON.parse(message.jsonPayload);
          
          // Apply filter if provided
          if (options.filter && !options.filter(payload)) {
            return;
          }
          
          callback(payload);
        } catch (error) {
          if (process.env.DEV) {
            console.error('Error parsing WebSocket message:', error);
          }
        }
      }
    }
  );

  return { wsStore };
}

/**
 * Composable for listening to multiple WebSocket events
 * 
 * @param {Array<{eventName: string, callback: function, filter?: function}>} listeners
 * @returns {object} - Object containing wsStore
 * 
 * @example
 * useWebSocketListeners([
 *   {
 *     eventName: 'evt_port_value_persisted',
 *     callback: (payload) => updatePort(payload),
 *     filter: (payload) => payload.p2 === portId
 *   },
 *   {
 *     eventName: 'evt_cfg_value_changed',
 *     callback: (payload) => loadDetails()
 *   }
 * ]);
 */
export function useWebSocketListeners(listeners) {
  const wsStore = useWebSocketStore();

  watch(
    () => wsStore.ws.message,
    (message) => {
      if (!message?.eventName) return;

      listeners.forEach(({ eventName, callback, filter }) => {
        if (message.eventName === eventName) {
          try {
            const payload = JSON.parse(message.jsonPayload);
            
            // Apply filter if provided
            if (filter && !filter(payload)) {
              return;
            }
            
            callback(payload);
          } catch (error) {
            if (process.env.DEV) {
              console.error(`Error parsing WebSocket message for ${eventName}:`, error);
            }
          }
        }
      });
    }
  );

  return { wsStore };
}

