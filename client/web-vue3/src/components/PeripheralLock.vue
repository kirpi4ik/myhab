<template>
	<div>
		<q-card class="door-lock-card text-white">
			<!-- Status Indicator Badge -->
			<div class="status-indicator">
				<q-icon name="mdi-shield-lock" size="16px" class="q-mr-xs"/>
				<span class="text-caption text-weight-medium">{{ $t('door.small_gate') }}</span>
			</div>

			<q-item class="card-content">
				<!-- Door Lock Icon Avatar -->
				<q-item-section avatar>
					<q-avatar
						size="60px"
						class="lock-avatar"
					>
						<q-icon
							name="fas fa-door-open"
							color="white"
							size="40px"
						/>
						<div class="lock-glow"></div>
					</q-avatar>
				</q-item-section>

				<!-- Lock Info -->
				<q-item-section>
					<q-item-label class="text-weight-medium text-h5 lock-name">
						{{ $t('door.small_gate') }}
					</q-item-label>
					<q-item-label class="text-weight-light text-blue-grey-2 lock-description">
						<q-icon name="mdi-information-outline" size="14px" class="q-mr-xs"/>
						Control acces poartă
					</q-item-label>
				</q-item-section>

				<!-- Event Logger -->
				<q-item-section side>
					<q-item-label>
						<event-logger :peripheral="peripheral" />
					</q-item-label>
				</q-item-section>
			</q-item>

			<q-separator class="separator-line" />

			<!-- Unlock Button Section -->
			<q-card-section class="action-section">
				<q-btn
					unelevated
					class="unlock-btn"
					icon="fas fa-lock-open"
					:label="$t('door.open')"
					no-caps
					:loading="loading"
					:disable="loading"
					@click="showConfirmDialog"
				>
					<template v-slot:loading>
						<q-spinner-dots color="white" size="md" />
					</template>
				</q-btn>
			</q-card-section>

			<!-- Bottom Accent Bar -->
			<div class="accent-bar"></div>
		</q-card>

		<!-- Confirmation Dialog -->
		<q-dialog 
			v-model="confirmDialog" 
			transition-show="jump-up" 
			transition-hide="jump-down"
			persistent
		>
			<q-card class="confirm-dialog-card">
				<q-bar class="dialog-header">
					<q-icon name="mdi-alert" size="24px" />
					<div class="dialog-title">{{ $t('common.alert') }}</div>
					<q-space />
					<q-btn dense flat icon="close" v-close-popup :disable="unlocking">
						<q-tooltip class="bg-primary">{{ $t('common.close') }}</q-tooltip>
					</q-btn>
				</q-bar>

				<q-card-section class="dialog-content">
					<div class="dialog-message">
						<q-icon name="mdi-door-open" size="48px" color="deep-orange-7" class="q-mb-md"/>
						<div class="text-h6">{{ $t('door.confirm_open') }}</div>
					</div>
				</q-card-section>

				<q-card-actions align="right" class="dialog-actions">
					<q-btn 
						flat 
						:label="$t('common.cancel')" 
						color="grey-7"
						class="cancel-btn"
						v-close-popup 
						:disable="unlocking"
					/>
					<q-btn 
						unelevated
						:label="$t('door.open')" 
						color="deep-orange-7"
						icon="fas fa-lock-open"
						class="confirm-btn"
						:loading="unlocking"
						:disable="unlocking"
						@click="openDoor"
					>
						<template v-slot:loading>
							<q-spinner-dots color="white" />
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

<style scoped lang="scss">
// CSS Custom Properties for easier theming and maintenance
.door-lock-card {
  // Define custom properties for reusability
  --card-border-radius: 12px;
  --card-padding: 16px;
  --transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-duration: 0.3s;
  
  // Warning/Alert theme for security door control
  background: linear-gradient(135deg, #f97316 0%, #ea580c 50%, #dc2626 100%);
  transition: all var(--transition-duration) var(--transition-timing);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--card-border-radius);
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 
              0 2px 4px -1px rgba(0, 0, 0, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  will-change: transform;

  // Decorative shield element
  &::before {
    content: '';
    position: absolute;
    top: 20px;
    left: 20px;
    width: 35px;
    height: 35px;
    background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    clip-path: polygon(50% 0%, 100% 38%, 82% 100%, 50% 90%, 18% 100%, 0% 38%);
    opacity: 0.3;
    pointer-events: none;
    z-index: 0;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 20px -5px rgba(234, 88, 12, 0.4), 
                0 6px 10px -5px rgba(234, 88, 12, 0.3),
                0 0 30px rgba(234, 88, 12, 0.2);

    .lock-avatar {
      transform: scale(1.05);
    }

    .unlock-btn {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
    }
  }

  // Reduce motion for accessibility
  @media (prefers-reduced-motion: reduce) {
    transition: none;
    
    * {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  }

  .status-indicator {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 4px 12px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    background: rgba(234, 88, 12, 0.3);
    border-color: rgba(249, 115, 22, 0.5);
    transition: all var(--transition-duration) ease;
  }

  .card-content {
    padding: 48px var(--card-padding) var(--card-padding);
    min-height: 120px;
  }

  .lock-avatar {
    transition: all var(--transition-duration) var(--transition-timing);
    border: 3px solid rgba(255, 255, 255, 0.3);
    position: relative;
    will-change: transform;
    background: linear-gradient(135deg, #ea580c 0%, #dc2626 100%);
    border-color: rgba(249, 115, 22, 0.5);
    box-shadow: 0 4px 8px rgba(234, 88, 12, 0.4),
                0 0 20px rgba(234, 88, 12, 0.2);

    .lock-glow {
      position: absolute;
      inset: -10px;
      background: radial-gradient(circle, rgba(249, 115, 22, 0.3) 0%, transparent 70%);
      animation: pulse 3s ease-in-out infinite;
      pointer-events: none;
    }
  }

  .lock-name {
    transition: color var(--transition-duration) ease;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    letter-spacing: 0.3px;
  }

  .lock-description {
    display: flex;
    align-items: center;
    margin-top: 4px;
    opacity: 0.9;
    font-size: 0.875rem;
  }

  .separator-line {
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(255, 255, 255, 0.2) 50%, 
      transparent 100%);
    height: 1px;
  }

  .action-section {
    padding: 16px var(--card-padding);
    background: rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: center;
    margin-bottom: 6px;
  }

  .unlock-btn {
    background: linear-gradient(135deg, #ffffff 0%, #f1f5f9 100%);
    color: #dc2626;
    font-weight: 600;
    font-size: 1rem;
    padding: 12px 32px;
    border-radius: 8px;
    transition: all 0.2s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    border: 2px solid rgba(249, 115, 22, 0.3);
    min-width: 200px;

    &:hover {
      background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
      color: #b91c1c;
      border-color: rgba(220, 38, 38, 0.4);
    }

    &:active {
      transform: translateY(0);
    }

    .q-icon {
      font-size: 20px;
    }
  }

  .accent-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(249, 115, 22, 0.8) 50%, 
      transparent 100%);
    animation: shimmer 2s ease-in-out infinite;
    box-shadow: 0 0 10px rgba(249, 115, 22, 0.5);
  }

  // Animations
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
  }

  @keyframes shimmer {
    0%, 100% {
      opacity: 0.6;
    }
    50% {
      opacity: 1;
    }
  }
}

// Confirmation Dialog Styling
.confirm-dialog-card {
  min-width: 400px;
  max-width: 500px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);

  @media (max-width: 599px) {
    min-width: 90vw;
  }
}

.dialog-header {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: white;
  padding: 12px 16px;
  font-weight: 600;

  .dialog-title {
    font-size: 1.1rem;
    font-weight: 600;
    margin-left: 8px;
  }
}

.dialog-content {
  padding: 32px 24px;
  background: #ffffff;
}

.dialog-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  color: #1e293b;

  .text-h6 {
    color: #334155;
    font-weight: 500;
  }
}

.dialog-actions {
  padding: 16px 24px 24px;
  gap: 12px;
  background: #f8fafc;
  border-top: 1px solid rgba(0, 0, 0, 0.08);

  .cancel-btn {
    padding: 8px 24px;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.05);
    }
  }

  .confirm-btn {
    padding: 8px 32px;
    font-weight: 600;
    box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
    transition: all 0.2s ease;

    &:hover {
      box-shadow: 0 4px 12px rgba(220, 38, 38, 0.4);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }
}

// Mobile optimization
@media (max-width: 599px) {
  .door-lock-card {
    .card-content {
      padding: 40px 12px 12px;
    }

    .unlock-btn {
      width: 100%;
      min-width: unset;
    }
  }
}
</style>
