<template>
	<div>
		<q-card class="q-ma-md-xs door-lock-card">
			<q-card-section class="bg-teal-5 text-amber-1 text-h6">
				<event-logger :peripheral="peripheral" />
				{{ $t('door.small_gate') }}
				<q-icon name="fas fa-door-open" class="float-right" size="40px" />
			</q-card-section>
			<q-separator color="white" />
			<q-card-section class="q-pa-none" vertical align="center">
				<div class="q-pa-sm">
					<q-btn 
						flat 
						class="text-h6" 
						icon="fas fa-lock-open" 
						:label="$t('door.open')" 
						no-caps 
						:loading="loading"
						:disable="loading"
						@click="showConfirmDialog"
					>
						<template v-slot:loading>
							<q-spinner-dots />
						</template>
					</q-btn>
				</div>
			</q-card-section>
		</q-card>

		<q-dialog 
			v-model="confirmDialog" 
			transition-show="jump-up" 
			transition-hide="jump-down"
			persistent
		>
			<q-card class="bg-white" style="min-width: 350px">
				<q-bar class="bg-deep-orange-7 text-white">
					<q-icon name="mdi-alert" />
					<div>{{ $t('common.alert') }}</div>
					<q-space />
					<q-btn dense flat icon="close" v-close-popup :disable="unlocking">
						<q-tooltip class="bg-primary">{{ $t('common.close') }}</q-tooltip>
					</q-btn>
				</q-bar>

				<q-card-section>
					<div class="text-h6">{{ $t('door.confirm_open') }}</div>
				</q-card-section>

				<q-card-actions align="right" class="q-px-md q-pb-md">
					<q-btn 
						flat 
						:label="$t('common.cancel')" 
						color="grey-7"
						v-close-popup 
						:disable="unlocking"
					/>
					<q-btn 
						unelevated
						:label="$t('door.open')" 
						color="deep-orange-7"
						icon="fas fa-lock-open"
						:loading="unlocking"
						:disable="unlocking"
						@click="openDoor"
					>
						<template v-slot:loading>
							<q-spinner-dots />
						</template>
					</q-btn>
				</q-card-actions>
			</q-card>
		</q-dialog>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { authzService } from '@/_services';
import { PERIPHERAL_GET_BY_ID, PUSH_EVENT } from '@/graphql/queries';
import EventLogger from 'components/EventLogger.vue';

// Composables
const { client } = useApolloClient();
const $q = useQuasar();

// State
const loading = ref(false);
const unlocking = ref(false);
const confirmDialog = ref(false);
const peripheral = ref({ 
	id: process.env.DOOR_LOCK_ID 
});

/**
 * Load peripheral details from the server
 */
const loadPeripheral = async () => {
	if (!process.env.DOOR_LOCK_ID) {
		$q.notify({
			color: 'warning',
			message: 'Door lock ID is not configured',
			icon: 'mdi-alert',
			position: 'top'
		});
		return;
	}

	loading.value = true;

	try {
		const response = await client.query({
			query: PERIPHERAL_GET_BY_ID,
			variables: { id: process.env.DOOR_LOCK_ID },
			fetchPolicy: 'network-only',
		});

		if (response.data?.devicePeripheral) {
			// Create a mutable copy of the peripheral data (Apollo returns immutable objects)
			const peripheralData = JSON.parse(JSON.stringify(response.data.devicePeripheral));
			
			// Set device state if available
			if (peripheralData.connectedTo?.[0]?.device?.status) {
				peripheralData.deviceState = peripheralData.connectedTo[0].device.status;
			}
			
			peripheral.value = peripheralData;
		}
	} catch (error) {
		console.error('Error loading door lock peripheral:', error);
		$q.notify({
			color: 'negative',
			message: 'Failed to load door lock information',
			icon: 'mdi-alert-circle',
			position: 'top'
		});
	} finally {
		loading.value = false;
	}
};

/**
 * Show confirmation dialog before opening the door
 */
const showConfirmDialog = () => {
	confirmDialog.value = true;
};

/**
 * Open the door by sending an event
 */
const openDoor = async () => {
	if (!peripheral.value?.id) {
		$q.notify({
			color: 'warning',
			message: 'Peripheral information is not available',
			icon: 'mdi-alert',
			position: 'top'
		});
		return;
	}

	if (!authzService.currentUserValue?.login) {
		$q.notify({
			color: 'warning',
			message: 'User information is not available',
			icon: 'mdi-alert',
			position: 'top'
		});
		return;
	}

	unlocking.value = true;

	try {
		const event = {
			p0: 'evt_intercom_door_lock',
			p1: 'PERIPHERAL',
			p2: peripheral.value.id,
			p3: 'mweb',
			p4: 'open',
			p5: "{'unlocked'}",
			p6: authzService.currentUserValue.login,
		};

		await client.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		});

		$q.notify({
			color: 'positive',
			message: 'Door unlock command sent',
			icon: 'mdi-check-circle',
			position: 'top'
		});

		confirmDialog.value = false;
	} catch (error) {
		console.error('Error opening door:', error);
		$q.notify({
			color: 'negative',
			message: 'Failed to open door',
			icon: 'mdi-alert-circle',
			position: 'top'
		});
	} finally {
		unlocking.value = false;
	}
};

// Load peripheral on mount
onMounted(() => {
	loadPeripheral();
});
</script>

<style scoped>
.door-lock-card {
	background-color: #a0d299;
}
</style>
