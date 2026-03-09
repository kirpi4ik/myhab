<template>
	<q-dialog v-model="dialogVisible" persistent transition-show="jump-up" transition-hide="jump-down">
		<q-card style="min-width: 420px; max-width: 500px; border-radius: 12px;">
			<!-- Header -->
			<q-bar class="bg-primary text-white">
				<q-icon name="mdi-share-variant" size="20px"/>
				<div class="q-ml-sm text-weight-bold">Share Access</div>
				<q-space/>
				<q-btn dense flat icon="close" @click="close" :disable="creating"/>
			</q-bar>

			<!-- Success View -->
			<template v-if="createdToken">
				<q-card-section class="text-center q-pa-lg">
					<q-icon name="mdi-check-circle" size="56px" color="positive" class="q-mb-md"/>
					<div class="text-h6 q-mb-sm">Share Link Created</div>
					<div class="text-caption text-grey-7 q-mb-lg">Copy and send this link to grant temporary access</div>

					<q-input
						:model-value="shareLink"
						readonly
						outlined
						dense
						class="q-mb-sm"
					>
						<template v-slot:append>
							<q-btn flat round dense icon="mdi-content-copy" @click="copyLink">
								<q-tooltip>Copy link</q-tooltip>
							</q-btn>
						</template>
					</q-input>
					<div v-if="copied" class="text-positive text-caption">Copied to clipboard!</div>
				</q-card-section>
				<q-card-actions align="right" class="q-pa-md">
					<q-btn flat label="Close" color="primary" @click="close"/>
				</q-card-actions>
			</template>

			<!-- Form View -->
			<template v-else>
				<q-card-section class="q-pa-lg">
					<div class="text-caption text-grey-7 q-mb-md">
						Create a temporary access link for this gate. The link will expire after the set date or when all allowed actions are used.
					</div>

					<!-- PIN -->
					<q-input
						v-model="form.pin"
						label="PIN (optional)"
						outlined
						dense
						class="q-mb-md"
						hint="Leave empty for no PIN protection"
					>
						<template v-slot:prepend>
							<q-icon name="mdi-lock"/>
						</template>
					</q-input>

					<!-- Start Date -->
					<q-input
						v-model="form.shareStartDate"
						label="Starts on"
						outlined
						dense
						class="q-mb-md"
						readonly
					>
						<template v-slot:prepend>
							<q-icon name="mdi-calendar-start"/>
						</template>
						<template v-slot:append>
							<q-icon name="mdi-calendar" class="cursor-pointer">
								<q-popup-proxy cover transition-show="scale" transition-hide="scale">
									<q-date v-model="form.shareStartDate" mask="YYYY-MM-DD" :options="startDateOptions">
										<div class="row items-center justify-end">
											<q-btn v-close-popup label="OK" flat color="primary"/>
										</div>
									</q-date>
								</q-popup-proxy>
							</q-icon>
						</template>
					</q-input>

					<!-- Expiry Date -->
					<q-input
						v-model="form.shareExpireDate"
						label="Expires on"
						outlined
						dense
						class="q-mb-md"
						readonly
						:error="dateError !== ''"
						:error-message="dateError"
					>
						<template v-slot:prepend>
							<q-icon name="mdi-calendar-clock"/>
						</template>
						<template v-slot:append>
							<q-icon name="mdi-calendar" class="cursor-pointer">
								<q-popup-proxy cover transition-show="scale" transition-hide="scale">
									<q-date v-model="form.shareExpireDate" mask="YYYY-MM-DD" :options="expireDateOptions">
										<div class="row items-center justify-end">
											<q-btn v-close-popup label="OK" flat color="primary"/>
										</div>
									</q-date>
								</q-popup-proxy>
							</q-icon>
						</template>
					</q-input>

					<!-- Actions Allowed -->
					<q-input
						v-model.number="form.actionsAllowed"
						label="Max uses"
						outlined
						dense
						type="number"
						:rules="[val => val >= 1 || 'At least 1 use required']"
						class="q-mb-sm"
						hint="How many times the link can be used"
					>
						<template v-slot:prepend>
							<q-icon name="mdi-counter"/>
						</template>
					</q-input>

					<div v-if="createError" class="text-negative text-caption q-mt-sm">
						{{ createError }}
					</div>
				</q-card-section>

				<q-card-actions align="right" class="q-pa-md q-pt-none">
					<q-btn flat label="Cancel" color="grey-7" @click="close" :disable="creating"/>
					<q-btn
						unelevated
						label="Create Share Link"
						color="primary"
						icon="mdi-link-plus"
						:loading="creating"
						@click="createShare"
					/>
				</q-card-actions>
			</template>
		</q-card>
	</q-dialog>
</template>

<script>
import { defineComponent, ref, reactive, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar, copyToClipboard } from 'quasar';
import { SHARED_WIDGET_CREATE } from '@/graphql/queries';

export default defineComponent({
	name: 'ShareWidgetDialog',
	props: {
		modelValue: { type: Boolean, default: false },
		peripheralId: { type: String, required: true },
		widgetType: { type: String, default: 'GATE_ACCESS' },
	},
	emits: ['update:modelValue'],
	setup(props, { emit }) {
		const $q = useQuasar();
		const { client } = useApolloClient();

		const dialogVisible = computed({
			get: () => props.modelValue,
			set: (val) => emit('update:modelValue', val),
		});

		const today = new Date();
		const defaultExpiry = new Date(today);
		defaultExpiry.setDate(defaultExpiry.getDate() + 7);
		const formatDate = (d) => d.toISOString().split('T')[0];

		const form = reactive({
			pin: '',
			shareStartDate: formatDate(today),
			shareExpireDate: formatDate(defaultExpiry),
			actionsAllowed: 5,
		});

		const creating = ref(false);
		const createError = ref('');
		const createdToken = ref('');
		const copied = ref(false);

		const shareLink = computed(() => {
			if (!createdToken.value) return '';
			return `${globalThis.location.origin}/shared/${createdToken.value}`;
		});

		const dateError = computed(() => {
			if (form.shareStartDate && form.shareExpireDate) {
				if (form.shareExpireDate <= form.shareStartDate) {
					return 'Expiry date must be after start date';
				}
			}
			return '';
		});

		const startDateOptions = (date) => {
			return date >= formatDate(today);
		};

		const expireDateOptions = (date) => {
			return date > form.shareStartDate;
		};

		const createShare = async () => {
			if (form.actionsAllowed < 1) {
				createError.value = 'At least 1 use is required';
				return;
			}
			if (!form.shareStartDate) {
				createError.value = 'Start date is required';
				return;
			}
			if (!form.shareExpireDate) {
				createError.value = 'Expiry date is required';
				return;
			}
			if (form.shareExpireDate <= form.shareStartDate) {
				createError.value = 'Expiry date must be after start date';
				return;
			}

			creating.value = true;
			createError.value = '';

			try {
				const response = await client.mutate({
					mutation: SHARED_WIDGET_CREATE,
					variables: {
						input: {
							widgetType: props.widgetType,
							peripheralId: props.peripheralId,
							pin: form.pin || null,
							shareStartDate: new Date(form.shareStartDate + 'T00:00:00').toISOString(),
							shareExpireDate: new Date(form.shareExpireDate + 'T23:59:59').toISOString(),
							actionsAllowed: form.actionsAllowed,
						}
					},
					fetchPolicy: 'no-cache',
				});

				const result = response.data.sharedWidgetCreate;
				if (result.success) {
					createdToken.value = result.token;
				} else {
					createError.value = result.error || 'Failed to create share link';
				}
			} catch (e) {
				createError.value = e.message || 'Failed to create share link';
			} finally {
				creating.value = false;
			}
		};

		const copyLink = async () => {
			try {
				await copyToClipboard(shareLink.value);
				copied.value = true;
				$q.notify({ color: 'positive', message: 'Link copied!', icon: 'mdi-check', position: 'top', timeout: 1500 });
				setTimeout(() => { copied.value = false; }, 2000);
			} catch {
				$q.notify({ color: 'negative', message: 'Failed to copy', position: 'top' });
			}
		};

		const close = () => {
			dialogVisible.value = false;
			setTimeout(() => {
				createdToken.value = '';
				createError.value = '';
				copied.value = false;
				form.pin = '';
				form.shareStartDate = formatDate(today);
				form.shareExpireDate = formatDate(defaultExpiry);
				form.actionsAllowed = 5;
			}, 300);
		};

		return {
			dialogVisible,
			form,
			creating,
			createError,
			createdToken,
			shareLink,
			copied,
			dateError,
			startDateOptions,
			expireDateOptions,
			createShare,
			copyLink,
			close,
		};
	}
});
</script>
