import {PUSH_EVENT} from '@/graphql/queries';
import {authzService} from '@/_services';
import {apolloClient} from '@/boot/graphql';

export const peripheralService = {
	toggle,
	toggleWithOptimisticUpdate,
	unlockDoor,
	handlePeripheralAction,
	peripheralInit,
};

/**
 * Generic toggle function for peripherals
 * @param {Object} peripheral - The peripheral object
 * @param {string} eventName - The event name (e.g., 'evt_light', 'evt_heat', 'evt_switch')
 * @returns {Promise<void>}
 */
async function toggle(peripheral, eventName) {
	if (!peripheral?.id) {
		console.warn('Invalid peripheral');
		return;
	}

	const newValue = peripheral.state === true ? 'off' : 'on';

	try {
		const event = {
			p0: eventName,
			p1: 'PERIPHERAL',
			p2: peripheral.id,
			p3: 'mweb',
			p4: newValue,
			p6: authzService.currentUserValue?.login || 'unknown',
		};

		await apolloClient.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		});
	} catch (error) {
		console.error(`Error toggling peripheral (${eventName}):`, error);
		throw error;
	}
}

/**
 * Toggle peripheral with optimistic state update
 * @param {Object} peripheral - The peripheral object
 * @param {string} eventName - The event name
 * @returns {Promise<void>}
 */
async function toggleWithOptimisticUpdate(peripheral, eventName) {
	if (!peripheral?.connectedTo?.length) {
		console.warn('Invalid peripheral or no connected ports');
		return;
	}

	const newValue = peripheral.state ? 'off' : 'on';

	try {
		const event = {
			p0: eventName,
			p1: 'PERIPHERAL',
			p2: peripheral.id,
			p3: 'mweb',
			p4: newValue,
			p6: authzService.currentUserValue?.login || 'unknown',
		};

		await apolloClient.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		});

		// Optimistically update state
		peripheral.state = !peripheral.state;
		peripheral.portValue = newValue.toUpperCase();
	} catch (error) {
		console.error(`Error toggling peripheral (${eventName}):`, error);
		throw error;
	}
}

/**
 * Unlock door
 * @param {number} peripheralId - The peripheral ID
 * @param {string} unlockCode - The unlock code (optional)
 * @returns {Promise<void>}
 */
async function unlockDoor(peripheralId, unlockCode = '') {
	if (!peripheralId) {
		console.warn('No peripheral ID for unlock');
		return;
	}

	try {
		const event = {
			p0: 'evt_intercom_door_lock',
			p1: 'PERIPHERAL',
			p2: peripheralId,
			p3: 'mweb',
			p4: 'open',
			p5: `{"unlockCode": "${unlockCode}"}`,
			p6: authzService.currentUserValue?.login || 'unknown',
		};

		await apolloClient.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		});
	} catch (error) {
		console.error('Error unlocking door:', error);
		throw error;
	}
}

/**
 * Handle peripheral action based on category
 * @param {Object} peripheral - The peripheral object with category
 * @param {Function} onDoorLock - Callback for door lock action (optional)
 * @returns {Promise<void>}
 */
async function handlePeripheralAction(peripheral, onDoorLock = null) {
	if (!peripheral?.category) {
		console.warn('Invalid peripheral');
		return;
	}

	switch (peripheral.category.name) {
		case 'DOOR_LOCK':
			if (onDoorLock) {
				onDoorLock(peripheral.id);
			}
			break;
		case 'LIGHT':
			await toggleWithOptimisticUpdate(peripheral, 'evt_light');
			break;
		case 'HEAT':
			await toggleWithOptimisticUpdate(peripheral, 'evt_heat');
			break;
		default:
			// Unhandled peripheral category - no action needed
			break;
	}
}

/**
 * Initialize peripheral with cache data (expiration, etc.)
 * @param {Object|null} cacheMap - Map of cache data keyed by port ID
 * @param {Object} peripheral - The peripheral object to initialize
 * @returns {Object} Initialized peripheral with cache data
 */
function peripheralInit(cacheMap, peripheral) {
	if (!peripheral) {
		console.warn('No peripheral provided to peripheralInit');
		return null;
	}

	// Clone the peripheral to avoid mutations
	const initialized = {...peripheral};

	// Ensure data structure exists and copy all peripheral properties into it
	if (!initialized.data) {
		initialized.data = {};
	}

	// Copy essential properties to data object for component access
	initialized.data.id = peripheral.id;
	initialized.data.name = peripheral.name;
	initialized.data.description = peripheral.description;
	initialized.data.category = peripheral.category;
	initialized.data.value = peripheral.value;

	// Always initialize state (default to false if value is undefined)
	initialized.data.state = peripheral.value === 'ON';
	initialized.state = peripheral.value === 'ON';

	// Add connected port information to data
	if (peripheral.connectedTo) {
		initialized.data.connectedTo = peripheral.connectedTo;
	}

	// Add configurations to both root and data for compatibility
	if (peripheral.configurations) {
		initialized.configurations = peripheral.configurations;
		initialized.data.configurations = peripheral.configurations;
	}

	// Add cache data if available (expiration times)
	if (cacheMap && peripheral.connectedTo?.length > 0) {
		const firstPort = peripheral.connectedTo[0];
		const cacheData = cacheMap[firstPort.id];
		
		if (cacheData) {
			initialized.expiration = cacheData.expiration || cacheData.cachedValue;
		}
	}

	return initialized;
}
