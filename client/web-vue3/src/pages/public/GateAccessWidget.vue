<template>
	<div class="gate-access-page" :class="{ 'gate-unlocked': unlockSuccess }">
		<!-- Loading State -->
		<div v-if="loading" class="state-container">
			<q-spinner-dots size="60px" color="white"/>
			<div class="state-text">Loading...</div>
		</div>

		<!-- Error State -->
		<div v-else-if="errorState" class="state-container error-state">
			<q-icon name="mdi-alert-circle-outline" size="80px" color="red-4"/>
			<div class="state-title">Access Unavailable</div>
			<div class="state-text">{{ errorMessage }}</div>
		</div>

		<!-- PIN Entry -->
		<div v-else-if="showPinEntry" class="state-container pin-container">
			<q-icon name="mdi-lock" size="64px" color="amber-4" class="q-mb-lg"/>
			<div class="state-title">Enter PIN</div>
			<div class="state-text q-mb-lg">A PIN is required to access this gate control</div>
			<q-input
				v-model="pinInput"
				type="password"
				outlined
				dark
				dense
				label="PIN"
				class="pin-input q-mb-md"
				@keyup.enter="verifyPin"
				:error="pinError"
				:error-message="pinErrorMessage"
			>
				<template v-slot:prepend>
					<q-icon name="mdi-key" color="amber-4"/>
				</template>
			</q-input>
			<q-btn
				unelevated
				color="amber-8"
				text-color="white"
				label="Continue"
				icon="mdi-arrow-right"
				class="pin-submit-btn"
				:loading="verifying"
				@click="verifyPin"
			/>
		</div>

		<!-- Gate Control -->
		<div v-else class="gate-container">
			<div class="gate-header">
				<q-icon name="mdi-shield-lock-outline" size="28px" color="white" class="q-mr-sm"/>
				<span class="gate-label">{{ peripheralName || 'Gate Access' }}</span>
			</div>

			<!-- Lock Visual -->
			<div class="lock-visual" :class="{ 'unlocked': unlockSuccess }">
				<div class="lock-circle" @click="showConfirm">
					<q-icon
						:name="unlockSuccess ? 'mdi-lock-open-variant' : 'mdi-lock'"
						size="72px"
						:color="unlockSuccess ? 'green-4' : 'white'"
					/>
					<div class="lock-pulse" v-if="!unlockSuccess && !unlocking"></div>
				</div>
			</div>

			<div class="gate-instruction" v-if="!unlockSuccess && !unlocking">
				Tap to unlock
			</div>
			<div class="gate-instruction success-text" v-else-if="unlockSuccess">
				Gate unlocked!
			</div>
			<div class="gate-instruction" v-else>
				<q-spinner-dots size="24px" color="white"/>
				Sending command...
			</div>

			<!-- Actions Info -->
			<div class="actions-info" v-if="actionsRemaining != null">
				<q-icon name="mdi-key-variant" size="16px" class="q-mr-xs"/>
				{{ actionsRemaining }} use{{ actionsRemaining !== 1 ? 's' : '' }} remaining
			</div>
		</div>

		<!-- Confirm Dialog -->
		<q-dialog v-model="confirmDialog" persistent transition-show="jump-up" transition-hide="jump-down">
			<q-card class="confirm-card">
				<q-bar class="confirm-bar">
					<q-icon name="mdi-alert" size="24px"/>
					<div class="q-ml-sm text-weight-bold">Confirm</div>
					<q-space/>
					<q-btn dense flat icon="close" v-close-popup :disable="unlocking"/>
				</q-bar>
				<q-card-section class="text-center q-pa-lg">
					<q-icon name="mdi-door-open" size="48px" color="deep-orange-7" class="q-mb-md"/>
					<div class="text-h6 text-grey-8">Open the gate?</div>
				</q-card-section>
				<q-card-actions align="right" class="q-pa-md">
					<q-btn flat label="Cancel" color="grey-7" v-close-popup :disable="unlocking"/>
					<q-btn
						unelevated
						label="Open Gate"
						color="deep-orange-7"
						icon="mdi-lock-open"
						:loading="unlocking"
						@click="executeAction"
					/>
				</q-card-actions>
			</q-card>
		</q-dialog>
	</div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Utils } from '@/_helpers';

export default defineComponent({
	name: 'GateAccessWidget',
	setup() {
		const route = useRoute();

		const loading = ref(true);
		const errorState = ref(false);
		const errorMessage = ref('');
		const showPinEntry = ref(false);
		const pinInput = ref('');
		const pinError = ref(false);
		const pinErrorMessage = ref('');
		const verifying = ref(false);

		const peripheralName = ref('');
		const actionsRemaining = ref(null);
		const unlocking = ref(false);
		const unlockSuccess = ref(false);
		const confirmDialog = ref(false);

		const token = ref('');

		const apiBase = () => `${Utils.host()}/api/public/share/${token.value}`;

		const loadWidget = async () => {
			token.value = route.params.token;
			if (!token.value) {
				errorState.value = true;
				errorMessage.value = 'Invalid share link';
				loading.value = false;
				return;
			}

			try {
				const res = await fetch(apiBase());
				if (res.status === 404) {
					errorState.value = true;
					errorMessage.value = 'This share link does not exist';
					loading.value = false;
					return;
				}
				const data = await res.json();

				if (data.state !== 'VALID') {
					errorState.value = true;
					const stateMessages = {
						'EXPIRED': 'This share link has expired',
						'DISABLED': 'This share link has been disabled',
						'ARCHIVED': 'This share link is no longer available',
						'NOT_YET_ACTIVE': 'This share link is not yet active'
					};
					errorMessage.value = stateMessages[data.state] || `Share link status: ${data.state}`;
					loading.value = false;
					return;
				}

				peripheralName.value = data.peripheralName || 'Gate Access';
				actionsRemaining.value = Math.max(0, data.actionsAllowed - data.actionsUsed);

				if (data.requiresPin) {
					showPinEntry.value = true;
				}
			} catch {
				errorState.value = true;
				errorMessage.value = 'Failed to load share link';
			} finally {
				loading.value = false;
			}
		};

		const verifyPin = () => {
			if (!pinInput.value || pinInput.value.trim() === '') {
				pinError.value = true;
				pinErrorMessage.value = 'Please enter the PIN';
				return;
			}
			pinError.value = false;
			showPinEntry.value = false;
		};

		const showConfirm = () => {
			if (unlocking.value || unlockSuccess.value) return;
			confirmDialog.value = true;
		};

		const executeAction = async () => {
			unlocking.value = true;
			confirmDialog.value = false;

			try {
				const body = {};
				if (pinInput.value) {
					body.pin = pinInput.value;
				}

				const res = await fetch(`${apiBase()}/action`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify(body)
				});

				const data = await res.json();

				if (res.status === 403 && data.error === 'Invalid PIN') {
					showPinEntry.value = true;
					pinInput.value = '';
					pinError.value = true;
					pinErrorMessage.value = 'Invalid PIN, please try again';
					unlocking.value = false;
					return;
				}

				if (!res.ok || !data.success) {
					errorState.value = true;
					errorMessage.value = data.error || 'Failed to open gate';
					unlocking.value = false;
					return;
				}

				actionsRemaining.value = data.actionsRemaining;
				unlockSuccess.value = true;

				setTimeout(() => {
					unlockSuccess.value = false;
					if (data.actionsRemaining <= 0) {
						errorState.value = true;
						errorMessage.value = 'This share link has expired (no uses remaining)';
					}
				}, 5000);
			} catch {
				errorState.value = true;
				errorMessage.value = 'Failed to connect to server';
			} finally {
				unlocking.value = false;
			}
		};

		onMounted(() => {
			loadWidget();
		});

		return {
			loading,
			errorState,
			errorMessage,
			showPinEntry,
			pinInput,
			pinError,
			pinErrorMessage,
			verifying,
			peripheralName,
			actionsRemaining,
			unlocking,
			unlockSuccess,
			confirmDialog,
			verifyPin,
			showConfirm,
			executeAction,
		};
	}
});
</script>

<style lang="scss" scoped>
.gate-access-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
	color: white;
	padding: 24px;
	transition: background 0.8s ease;

	&.gate-unlocked {
		background: linear-gradient(135deg, #0d3320 0%, #145a32 50%, #1e8449 100%);
	}
}

.state-container {
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
	max-width: 400px;
}

.state-title {
	font-size: 1.5rem;
	font-weight: 600;
	margin-top: 16px;
}

.state-text {
	font-size: 1rem;
	opacity: 0.8;
	margin-top: 8px;
}

.error-state .state-title {
	color: #ef9a9a;
}

.pin-container {
	width: 100%;
	max-width: 360px;
}

.pin-input {
	width: 100%;

	:deep(.q-field__control) {
		background: rgba(255, 255, 255, 0.1);
	}
}

.pin-submit-btn {
	width: 100%;
	padding: 12px;
	font-size: 1rem;
	font-weight: 600;
}

.gate-container {
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
}

.gate-header {
	display: flex;
	align-items: center;
	margin-bottom: 48px;
	opacity: 0.9;
}

.gate-label {
	font-size: 1.2rem;
	font-weight: 500;
	letter-spacing: 0.5px;
}

.lock-visual {
	margin-bottom: 32px;

	&.unlocked .lock-circle {
		border-color: rgba(76, 175, 80, 0.5);
		box-shadow: 0 0 40px rgba(76, 175, 80, 0.3);
	}
}

.lock-circle {
	width: 180px;
	height: 180px;
	border-radius: 50%;
	border: 3px solid rgba(255, 255, 255, 0.25);
	display: flex;
	align-items: center;
	justify-content: center;
	cursor: pointer;
	position: relative;
	background: rgba(255, 255, 255, 0.05);
	transition: all 0.3s ease;
	box-shadow: 0 0 30px rgba(255, 255, 255, 0.1);

	&:hover {
		border-color: rgba(255, 255, 255, 0.5);
		box-shadow: 0 0 40px rgba(255, 255, 255, 0.2);
		transform: scale(1.03);
	}

	&:active {
		transform: scale(0.97);
	}
}

.lock-pulse {
	position: absolute;
	inset: -12px;
	border-radius: 50%;
	border: 2px solid rgba(255, 255, 255, 0.15);
	animation: pulse-ring 2.5s ease-in-out infinite;
	pointer-events: none;
}

@keyframes pulse-ring {
	0%, 100% {
		opacity: 0.3;
		transform: scale(1);
	}
	50% {
		opacity: 0.8;
		transform: scale(1.08);
	}
}

.gate-instruction {
	font-size: 1rem;
	opacity: 0.7;
	display: flex;
	align-items: center;
	gap: 8px;
}

.success-text {
	color: #81c784;
	opacity: 1;
	font-weight: 600;
	font-size: 1.1rem;
}

.actions-info {
	margin-top: 32px;
	padding: 8px 16px;
	border-radius: 20px;
	background: rgba(255, 255, 255, 0.08);
	font-size: 0.85rem;
	opacity: 0.6;
	display: flex;
	align-items: center;
}

.confirm-card {
	min-width: 340px;
	border-radius: 12px;
}

.confirm-bar {
	background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
	color: white;
}

@media (max-width: 599px) {
	.lock-circle {
		width: 150px;
		height: 150px;
	}
}
</style>
