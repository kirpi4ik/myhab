/**
 * Composable for controlling peripherals (lights, heat, locks)
 * This composable provides UI-specific functionality and delegates
 * business logic to peripheralService
 */
import { ref } from 'vue';
import { peripheralService } from '@/_services/controls';

export function usePeripheralControl() {
  const unlockDialog = ref({
    show: false,
    unlockCode: null,
    assetId: null,
  });

  /**
   * Toggle light on/off
   */
  const toggleLight = async (peripheral) => {
    await peripheralService.toggleWithOptimisticUpdate(peripheral, 'evt_light');
  };

  /**
   * Toggle heat on/off
   */
  const toggleHeat = async (peripheral) => {
    await peripheralService.toggleWithOptimisticUpdate(peripheral, 'evt_heat');
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
      await peripheralService.unlockDoor(
        unlockDialog.value.assetId,
        unlockDialog.value.unlockCode || ''
      );
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
    await peripheralService.handlePeripheralAction(peripheral, showUnlockDialog);
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

