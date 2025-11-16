/**
 * Composable for managing peripheral state and data
 */
import { ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { PERIPHERAL_LIST_WUI } from '@/graphql/queries';
import _ from 'lodash';

export function usePeripheralState() {
  const { client } = useApolloClient();
  
  const peripherals = ref({});
  const portToPeripheralMap = ref({});
  const assetMap = ref({});
  const loading = ref(false);
  const error = ref(null);

  /**
   * Initialize peripheral map from peripheral data
   */
  const initializePeripheralMap = (peripheral) => {
    if (!peripheral.connectedTo || peripheral.connectedTo.length === 0) return;

    const port = peripheral.connectedTo[0];
    if (!port) return;

    // Map port ID to peripheral IDs
    if (!portToPeripheralMap.value[port.id]) {
      portToPeripheralMap.value[port.id] = [];
    }
    portToPeripheralMap.value[port.id].push(peripheral.id);

    // Map category to port and peripheral
    const category = peripheral.category.name.toLowerCase();
    if (!assetMap.value[category]) {
      assetMap.value[category] = [];
    }
    if (!assetMap.value[category][port.id]) {
      assetMap.value[category][port.id] = [];
    }
    assetMap.value[category][port.id].push(peripheral.id);
  };

  /**
   * Initialize peripheral state from port data
   */
  const initializePeripheralState = (peripheral) => {
    if (!peripheral.connectedTo || peripheral.connectedTo.length === 0) return;

    const port = peripheral.connectedTo[0];
    if (!port) return;

    peripheral.portValue = port.value;
    peripheral.state = peripheral.portValue === 'ON';
    peripheral.portId = port.id;
    // portUid is deprecated, use portId instead
    peripheral.deviceStatus = port.device?.status;
  };

  /**
   * Load peripherals from backend
   */
  const loadPeripherals = async () => {
    loading.value = true;
    error.value = null;

    try {
      const response = await client.query({
        query: PERIPHERAL_LIST_WUI,
        variables: {},
        fetchPolicy: 'network-only',
      });

      // Clone response to avoid mutations
      const data = _.cloneDeep(response.data);
      
      // Initialize maps
      data.devicePeripheralList.forEach(initializePeripheralMap);
      
      // Initialize states
      const assets = data.devicePeripheralList;
      assets.forEach(initializePeripheralState);

      // Convert array to map by ID
      peripherals.value = _.reduce(
        assets,
        (hash, value) => {
          hash[value.id] = value;
          return hash;
        },
        {}
      );

      loading.value = false;
      return peripherals.value;
    } catch (err) {
      error.value = err;
      loading.value = false;
      console.error('Error loading peripherals:', err);
      throw err;
    }
  };

  /**
   * Update peripheral UI based on WebSocket event
   */
  const updatePeripheralFromEvent = (jsonPayload) => {
    try {
      const payload = JSON.parse(jsonPayload);
      const connectedPeripherals = portToPeripheralMap.value[payload.p2];

      if (!connectedPeripherals) return;

      connectedPeripherals.forEach((peripheralId) => {
        const peripheral = peripherals.value[peripheralId];
        if (peripheral) {
          peripheral.portValue = payload.p4;
          peripheral.state = payload.p4 === 'ON';
        }
      });
    } catch (err) {
      console.error('Error updating peripheral from event:', err);
    }
  };

  /**
   * Get peripheral by ID
   */
  const getPeripheral = (id) => {
    return peripherals.value[id];
  };

  /**
   * Check if peripheral exists
   */
  const hasPeripheral = (id) => {
    return !!peripherals.value[id];
  };

  return {
    peripherals,
    portToPeripheralMap,
    assetMap,
    loading,
    error,
    loadPeripherals,
    updatePeripheralFromEvent,
    getPeripheral,
    hasPeripheral
  };
}

