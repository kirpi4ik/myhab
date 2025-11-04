/**
 * Composable for controlling peripherals (lights, heat, locks)
 */
import { ref } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { PUSH_EVENT } from '@/graphql/queries';
import { authzService } from '@/_services';

export function usePeripheralControl() {
  const { client } = useApolloClient();
  
  const unlockDialog = ref({
    show: false,
    unlockCode: null,
    assetId: null,
  });

  /**
   * Toggle light on/off
   */
  const toggleLight = async (peripheral) => {
    if (!peripheral || !peripheral.connectedTo || peripheral.connectedTo.length === 0) {
      console.warn('Invalid peripheral or no connected ports');
      return;
    }

    const newValue = peripheral.state ? 'off' : 'on';

    try {
      const event = {
        p0: 'evt_light',
        p1: 'PERIPHERAL',
        p2: peripheral.id,
        p3: 'mweb',
        p4: newValue,
        p6: authzService.currentUserValue?.login || 'unknown',
      };

      await client.mutate({
        mutation: PUSH_EVENT,
        variables: { input: event },
      });

      // Optimistically update state
      peripheral.state = !peripheral.state;
      peripheral.portValue = newValue.toUpperCase();
    } catch (error) {
      console.error('Error toggling light:', error);
      throw error;
    }
  };

  /**
   * Toggle heat on/off
   */
  const toggleHeat = async (peripheral) => {
    if (!peripheral || !peripheral.connectedTo || peripheral.connectedTo.length === 0) {
      console.warn('Invalid peripheral or no connected ports');
      return;
    }

    const newValue = peripheral.state ? 'off' : 'on';

    try {
      const event = {
        p0: 'evt_heat',
        p1: 'PERIPHERAL',
        p2: peripheral.id,
        p3: 'mweb',
        p4: newValue,
        p6: authzService.currentUserValue?.login || 'unknown',
      };

      await client.mutate({
        mutation: PUSH_EVENT,
        variables: { input: event },
      });

      // Optimistically update state
      peripheral.state = !peripheral.state;
      peripheral.portValue = newValue.toUpperCase();
    } catch (error) {
      console.error('Error toggling heat:', error);
      throw error;
    }
  };

  /**
   * Show unlock confirmation dialog
   */
  const showUnlockDialog = (assetId) => {
    unlockDialog.value = {
      show: true,
      unlockCode: null,
      assetId,
    };
  };

  /**
   * Hide unlock dialog
   */
  const hideUnlockDialog = () => {
    unlockDialog.value = {
      show: false,
      unlockCode: null,
      assetId: null,
    };
  };

  /**
   * Unlock door
   */
  const unlockDoor = async () => {
    if (!unlockDialog.value.assetId) {
      console.warn('No asset ID for unlock');
      return;
    }

    try {
      const event = {
        p0: 'evt_intercom_door_lock',
        p1: 'PERIPHERAL',
        p2: unlockDialog.value.assetId,
        p3: 'mweb',
        p4: 'open',
        p5: `{"unlockCode": "${unlockDialog.value.unlockCode || ''}"}`,
        p6: authzService.currentUserValue?.login || 'unknown',
      };

      await client.mutate({
        mutation: PUSH_EVENT,
        variables: { input: event },
      });

      hideUnlockDialog();
    } catch (error) {
      console.error('Error unlocking door:', error);
      throw error;
    }
  };

  /**
   * Handle peripheral action based on category
   */
  const handlePeripheralAction = async (peripheral) => {
    if (!peripheral || !peripheral.category) {
      console.warn('Invalid peripheral');
      return;
    }

    switch (peripheral.category.name) {
      case 'DOOR_LOCK':
        showUnlockDialog(peripheral.id);
        break;
      case 'LIGHT':
        await toggleLight(peripheral);
        break;
      case 'HEAT':
        await toggleHeat(peripheral);
        break;
      default:
        // Unhandled peripheral category - no action needed
        break;
    }
  };

  return {
    unlockDialog,
    toggleLight,
    toggleHeat,
    showUnlockDialog,
    hideUnlockDialog,
    unlockDoor,
    handlePeripheralAction
  };
}

