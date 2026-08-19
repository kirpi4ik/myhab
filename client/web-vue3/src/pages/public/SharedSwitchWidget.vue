<template>
	<div class="switch-access-page" :class="{ 'is-on': isOn }">
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
			<div class="state-text q-mb-lg">A PIN is required to access this control</div>
			<q-input
				v-model="pinInput"
				type="number"
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
				:loading="verifyingPin"
				@click="verifyPin"
			/>
		</div>

		<!-- Switch Control -->
		<div v-else class="switch-container">
			<div class="switch-header">
				<q-icon :name="typeIcon" size="28px" color="white" class="q-mr-sm"/>
				<span class="switch-label">{{ peripheralName }}</span>
				<q-btn
					flat
					round
					dense
					size="sm"
					icon="mdi-refresh"
					color="white"
					class="q-ml-sm"
					:loading="refreshing"
					@click="loadWidget(true)"
				>
					<q-tooltip>Refresh state</q-tooltip>
				</q-btn>
			</div>

			<!-- State Visual -->
			<div class="state-visual" :class="{ 'on': isOn }">
				<div class="state-circle">
					<q-icon :name="isOn ? typeIcon : typeIconOff" size="72px" :color="isOn ? 'light-blue-2' : 'white'"/>
					<div class="state-pulse" v-if="isOn"></div>
				</div>
			</div>

			<div class="switch-state-text" :class="{ 'on-text': isOn }">
				{{ isOn ? 'RUNNING' : 'STOPPED' }}
			</div>

			<!-- Controls -->
			<div class="switch-actions">
				<q-btn
					unelevated
					size="lg"
					color="light-blue-7"
					text-color="white"
					icon="mdi-play"
					label="Start"
					class="action-btn"
					:loading="sending === 'on'"
					:disable="!canStart || sending !== null"
					@click="confirmStart"
				/>
				<q-btn
					unelevated
					size="lg"
					color="blue-grey-7"
					text-color="white"
					icon="mdi-stop"
					label="Stop"
					class="action-btn"
					:loading="sending === 'off'"
					:disable="!offAllowed || sending !== null"
					@click="executeAction('off')"
				/>
			</div>

			<div class="hint-text" v-if="!canStart">
				No uses remaining — you can still stop it
			</div>

			<!-- Info chips -->
			<div class="actions-info" v-if="actionsRemaining != null">
				<q-icon name="mdi-key-variant" size="16px" class="q-mr-xs"/>
				{{ actionsRemaining }} start{{ actionsRemaining !== 1 ? 's' : '' }} remaining
			</div>

			<div class="actions-info" v-if="autoOffText">
				<q-icon name="mdi-timer-outline" size="16px" class="q-mr-xs"/>
				Stops automatically after {{ autoOffText }}
			</div>

			<div class="actions-info" v-if="expiresAt">
				<q-icon name="mdi-clock-outline" size="16px" class="q-mr-xs"/>
				Expires {{ expiresAt }}
			</div>
		</div>

		<!-- Confirm Dialog -->
		<q-dialog v-model="confirmDialog" persistent transition-show="jump-up" transition-hide="jump-down">
			<q-card class="confirm-card">
				<q-bar class="confirm-bar">
					<q-icon name="mdi-alert" size="24px"/>
					<div class="q-ml-sm text-weight-bold">Confirm</div>
					<q-space/>
					<q-btn dense flat icon="close" v-close-popup :disable="sending !== null"/>
				</q-bar>
				<q-card-section class="text-center q-pa-lg">
					<q-icon :name="typeIcon" size="48px" color="light-blue-7" class="q-mb-md"/>
					<div class="text-h6 text-grey-8">Start {{ peripheralName }}?</div>
					<div class="text-caption text-grey-7 q-mt-sm">
						This uses one of your {{ actionsRemaining }} remaining start{{ actionsRemaining !== 1 ? 's' : '' }}.
					</div>
				</q-card-section>
				<q-card-actions align="right" class="q-pa-md">
					<q-btn flat label="Cancel" color="grey-7" v-close-popup :disable="sending !== null"/>
					<q-btn
						unelevated
						label="Start"
						color="light-blue-7"
						icon="mdi-play"
						:loading="sending === 'on'"
						@click="executeAction('on')"
					/>
				</q-card-actions>
			</q-card>
		</q-dialog>
	</div>
</template>

<script>
import { computed, defineComponent, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { Utils } from '@/_helpers';
import humanizeDuration from 'humanize-duration';

const TYPE_ICONS = {
	WATER_PUMP: { on: 'mdi-water-pump', off: 'mdi-water-pump-off', fallbackName: 'Water Pump' },
	SPRINKLER: { on: 'mdi-sprinkler-variant', off: 'mdi-sprinkler', fallbackName: 'Sprinkler' },
	LIGHT: { on: 'mdi-lightbulb-on', off: 'mdi-lightbulb-outline', fallbackName: 'Light' },
};

const STATE_MESSAGES = {
	EXPIRED: 'This share link has expired',
	DISABLED: 'This share link has been disabled',
	ARCHIVED: 'This share link is no longer available',
	NOT_YET_ACTIVE: 'This share link is not yet active',
};

export default defineComponent({
	name: 'SharedSwitchWidget',
	props: {
		widgetType: { type: String, default: 'LIGHT' },
	},
	setup(props) {
		const route = useRoute();

		const loading = ref(true);
		const refreshing = ref(false);
		const errorState = ref(false);
		const errorMessage = ref('');
		const showPinEntry = ref(false);
		const pinInput = ref('');
		const pinError = ref(false);
		const pinErrorMessage = ref('');
		const verifyingPin = ref(false);

		const peripheralName = ref('');
		const actionsRemaining = ref(null);
		const expiresAt = ref(null);
		const isOn = ref(false);
		const offAllowed = ref(false);
		const autoOffSeconds = ref(null);
		const sending = ref(null);
		const confirmDialog = ref(false);

		const token = ref('');

		const icons = computed(() => TYPE_ICONS[props.widgetType] || TYPE_ICONS.LIGHT);
		const typeIcon = computed(() => icons.value.on);
		const typeIconOff = computed(() => icons.value.off);
		const canStart = computed(() => actionsRemaining.value > 0);
		const autoOffText = computed(() => {
			if (!autoOffSeconds.value) return null;
			return humanizeDuration(Number(autoOffSeconds.value) * 1000, { largest: 2, language: 'en', round: true });
		});

		const apiBase = () => `${Utils.host()}/api/public/share/${token.value}`;

		const loadWidget = async (isRefresh = false) => {
			token.value = route.params.token;
			if (!token.value) {
				errorState.value = true;
				errorMessage.value = 'Invalid share link';
				loading.value = false;
				return;
			}

			if (isRefresh) {
				refreshing.value = true;
			}

			try {
				const res = await fetch(apiBase());
				if (res.status === 404) {
					errorState.value = true;
					errorMessage.value = 'This share link does not exist';
					return;
				}
				const data = await res.json();

				// A spent or date-expired link keeps its Stop control, so only fall into
				// the error state when the link cannot be used at all.
				if (data.state !== 'VALID' && !data.offAllowed) {
					errorState.value = true;
					errorMessage.value = STATE_MESSAGES[data.state] || `Share link status: ${data.state}`;
					return;
				}

				peripheralName.value = data.peripheralName || icons.value.fallbackName;
				actionsRemaining.value = Math.max(0, data.actionsAllowed - data.actionsUsed);
				isOn.value = data.currentState === true;
				offAllowed.value = data.offAllowed === true;
				autoOffSeconds.value = data.autoOffTimeout || null;
				if (data.shareExpireDate) {
					expiresAt.value = new Date(data.shareExpireDate).toLocaleDateString(undefined, {
						year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
					});
				}

				if (data.requiresPin && !isRefresh) {
					showPinEntry.value = true;
				}
			} catch {
				errorState.value = true;
				errorMessage.value = 'Failed to load share link';
			} finally {
				loading.value = false;
				refreshing.value = false;
			}
		};

		const verifyPin = async () => {
			if (!pinInput.value || pinInput.value.trim() === '') {
				pinError.value = true;
				pinErrorMessage.value = 'Please enter the PIN';
				return;
			}

			verifyingPin.value = true;
			pinError.value = false;

			try {
				const res = await fetch(`${apiBase()}/verify-pin`, {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ pin: pinInput.value })
				});

				const data = await res.json();

				if (!res.ok || !data.success) {
					pinError.value = true;
					pinErrorMessage.value = data.error || 'Invalid PIN';
					return;
				}

				pinError.value = false;
				showPinEntry.value = false;
			} catch {
				pinError.value = true;
				pinErrorMessage.value = 'Failed to verify PIN. Please try again.';
			} finally {
				verifyingPin.value = false;
			}
		};

		const confirmStart = () => {
			if (sending.value || !canStart.value) return;
			confirmDialog.value = true;
		};

		const executeAction = async (action) => {
			sending.value = action;
			confirmDialog.value = false;

			try {
				const body = { action };
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
					return;
				}

				if (!res.ok || !data.success) {
					errorState.value = true;
					errorMessage.value = data.error || `Failed to ${action === 'on' ? 'start' : 'stop'}`;
					return;
				}

				actionsRemaining.value = data.actionsRemaining;
				offAllowed.value = data.offAllowed === true;
				isOn.value = action === 'on';
			} catch {
				errorState.value = true;
				errorMessage.value = 'Failed to connect to server';
			} finally {
				sending.value = null;
				// The device confirms asynchronously over MQTT — re-read so the shown
				// state is the real one rather than the optimistic guess above.
				setTimeout(() => loadWidget(true), 1500);
			}
		};

		onMounted(() => {
			loadWidget();
		});

		return {
			loading,
			refreshing,
			errorState,
			errorMessage,
			showPinEntry,
			pinInput,
			pinError,
			pinErrorMessage,
			verifyingPin,
			peripheralName,
			actionsRemaining,
			expiresAt,
			isOn,
			offAllowed,
			autoOffText,
			sending,
			confirmDialog,
			canStart,
			typeIcon,
			typeIconOff,
			loadWidget,
			verifyPin,
			confirmStart,
			executeAction,
		};
	}
});
</script>

<style lang="scss" scoped>
.switch-access-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
	color: white;
	padding: 24px;
	transition: background 0.8s ease;

	&.is-on {
		background: linear-gradient(135deg, #0c4a6e 0%, #0369a1 50%, #0891b2 100%);
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

.switch-container {
	display: flex;
	flex-direction: column;
	align-items: center;
	text-align: center;
}

.switch-header {
	display: flex;
	align-items: center;
	margin-bottom: 32px;
	opacity: 0.9;
}

.switch-label {
	font-size: 1.2rem;
	font-weight: 500;
	letter-spacing: 0.5px;
}

.state-visual {
	margin-bottom: 16px;

	&.on .state-circle {
		border-color: rgba(56, 189, 248, 0.5);
		box-shadow: 0 0 40px rgba(56, 189, 248, 0.35);
	}
}

.state-circle {
	width: 170px;
	height: 170px;
	border-radius: 50%;
	border: 3px solid rgba(255, 255, 255, 0.25);
	display: flex;
	align-items: center;
	justify-content: center;
	position: relative;
	background: rgba(255, 255, 255, 0.05);
	transition: all 0.3s ease;
	box-shadow: 0 0 30px rgba(255, 255, 255, 0.1);
}

.state-pulse {
	position: absolute;
	inset: -12px;
	border-radius: 50%;
	border: 2px solid rgba(56, 189, 248, 0.25);
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

.switch-state-text {
	font-size: 1rem;
	font-weight: 600;
	letter-spacing: 1.5px;
	opacity: 0.7;
	margin-bottom: 28px;

	&.on-text {
		color: #7dd3fc;
		opacity: 1;
	}
}

.switch-actions {
	display: flex;
	gap: 16px;
	flex-wrap: wrap;
	justify-content: center;
}

.action-btn {
	min-width: 140px;
	font-weight: 600;
}

.hint-text {
	margin-top: 16px;
	font-size: 0.85rem;
	color: #fcd34d;
	opacity: 0.9;
}

.actions-info {
	margin-top: 16px;
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
	background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
	color: white;
}

@media (max-width: 599px) {
	.state-circle {
		width: 140px;
		height: 140px;
	}

	.action-btn {
		min-width: 120px;
	}
}
</style>
