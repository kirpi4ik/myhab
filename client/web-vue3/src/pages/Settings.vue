<template>
	<q-page class="q-pa-md">
		<div class="row q-mb-md items-center">
			<q-icon name="mdi-cog" size="32px" class="q-mr-sm text-primary"/>
			<div class="text-h5">{{ $t('navigation.settings') }}</div>
		</div>

		<q-card flat bordered class="q-mb-md">
			<q-card-section>
				<div class="text-h6">{{ $t('settings.language.title') }}</div>
				<div class="text-caption text-grey-7 q-mb-md">
					{{ $t('settings.language.caption') }}
				</div>

				<q-banner v-if="!isLoggedIn" class="bg-orange-1 text-orange-9 q-mb-md" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-alert" />
					</template>
					{{ $t('settings.language.login_required') }}
				</q-banner>

				<q-select
					:model-value="languagePref"
					:options="languageOptions"
					:label="$t('settings.language.label')"
					:disable="!isLoggedIn || savingLanguage"
					:loading="savingLanguage"
					emit-value
					map-options
					outlined
					dense
					style="max-width: 320px"
					@update:model-value="onLanguageChange"
				>
					<template v-slot:prepend>
						<q-icon name="mdi-translate" />
					</template>
				</q-select>
			</q-card-section>
		</q-card>

		<q-card flat bordered class="q-mb-md">
			<q-card-section>
				<div class="text-h6">{{ $t('settings.timezone.title') }}</div>
				<div class="text-caption text-grey-7 q-mb-md">
					{{ $t('settings.timezone.caption') }}
				</div>

				<q-banner v-if="!isLoggedIn" class="bg-orange-1 text-orange-9 q-mb-md" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-alert" />
					</template>
					{{ $t('settings.timezone.login_required') }}
				</q-banner>

				<q-select
					:model-value="timezonePref"
					:options="timezoneOptions"
					:label="$t('settings.timezone.label')"
					:disable="!isLoggedIn || savingTimezone"
					:loading="savingTimezone"
					emit-value
					map-options
					outlined
					dense
					style="max-width: 320px"
					@update:model-value="onTimezoneChange"
				>
					<template v-slot:prepend>
						<q-icon name="mdi-clock-outline" />
					</template>
				</q-select>
			</q-card-section>
		</q-card>

		<q-card flat bordered class="q-mb-md">
			<q-card-section>
				<div class="text-h6">Push notifications</div>
				<div class="text-caption text-grey-7 q-mb-md">
					Receive message alerts as native system notifications on this device, even when the
					app is closed. This setting is per device and is remembered between sessions.
				</div>

				<q-banner v-if="!isLoggedIn" class="bg-orange-1 text-orange-9 q-mb-md" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-alert" />
					</template>
					You must be logged in to enable push notifications.
				</q-banner>

				<q-banner v-else-if="!pushSupported" class="bg-blue-1 text-blue-9" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-information" />
					</template>
					Push notifications require the installed app (PWA) and are not available in this browser.
				</q-banner>

				<q-item v-else tag="label" class="q-pa-none">
					<q-item-section avatar>
						<q-icon name="mdi-bell-ring-outline" color="primary" />
					</q-item-section>
					<q-item-section>
						<q-item-label>Enable on this device</q-item-label>
						<q-item-label caption>{{ pushHint }}</q-item-label>
					</q-item-section>
					<q-item-section side>
						<q-spinner-dots v-if="pushBusy" size="24px" color="primary" />
						<q-toggle
							v-else
							:model-value="pushSubscribed"
							:disable="pushPermission === 'denied'"
							color="primary"
							@update:model-value="onTogglePush"
						/>
					</q-item-section>
				</q-item>
			</q-card-section>
		</q-card>

		<q-card flat bordered class="q-mb-md">
			<q-card-section>
				<div class="text-h6">Muted messages</div>
				<div class="text-caption text-grey-7 q-mb-md">
					Messages matching these rules skip your inbox and push notifications — they are filed
					directly as read or archived. Create one with "Mute similar" on any message in the
					inbox. Deleting a rule affects future messages only.
				</div>

				<q-banner v-if="!isLoggedIn" class="bg-orange-1 text-orange-9 q-mb-md" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-alert" />
					</template>
					You must be logged in to manage muted messages.
				</q-banner>

				<div v-else-if="rulesLoading" class="q-py-sm">
					<q-spinner-dots size="24px" color="primary" />
				</div>

				<div v-else-if="rules.length === 0" class="text-caption text-grey-6 q-py-sm">
					Nothing is muted. Open a message in the inbox and choose "Mute similar".
				</div>

				<q-list v-else separator>
					<q-item v-for="rule in rules" :key="rule.id">
						<q-item-section avatar>
							<q-icon name="mdi-bell-off-outline" color="deep-orange" />
						</q-item-section>
						<q-item-section>
							<q-item-label>{{ ruleLabel(rule) }}</q-item-label>
							<q-item-label caption>Filed as {{ rule.targetState }}</q-item-label>
						</q-item-section>
						<q-item-section side>
							<q-btn
								flat
								round
								dense
								icon="mdi-delete"
								color="grey-7"
								:loading="deletingRuleId === rule.id"
								@click="deleteRule(rule)"
							/>
						</q-item-section>
					</q-item>
				</q-list>
			</q-card-section>
		</q-card>

		<q-card flat bordered>
			<q-card-section>
				<div class="text-h6">Dashboard widgets</div>
				<div class="text-caption text-grey-7 q-mb-md">
					Choose which widgets are shown on your dashboard. Changes are saved automatically
					and follow your account across browsers and devices.
				</div>

				<q-banner v-if="!isLoggedIn" class="bg-orange-1 text-orange-9 q-mb-md" dense>
					<template v-slot:avatar>
						<q-icon name="mdi-alert" />
					</template>
					You must be logged in to manage widget visibility.
				</q-banner>

				<q-list separator>
					<q-item
						v-for="section in sections"
						:key="section.id"
						class="q-pa-none"
						:style="{ display: 'block' }"
					>
						<q-item-section>
							<q-item-label header class="text-weight-bold text-grey-9 q-pa-sm">
								{{ section.title }}
							</q-item-label>
							<q-list>
								<q-item
									v-for="widget in section.widgets"
									:key="widget.id"
									tag="label"
									:disable="!isLoggedIn || saving[widget.id]"
									v-ripple
								>
									<q-item-section avatar>
										<q-checkbox
											:model-value="isVisible(widget.id)"
											@update:model-value="(v) => onToggle(widget.id, v)"
											:disable="!isLoggedIn || saving[widget.id]"
										/>
									</q-item-section>
									<q-item-section>
										<q-item-label>{{ widget.label }}</q-item-label>
										<q-item-label caption>{{ widget.id }}</q-item-label>
									</q-item-section>
									<q-item-section side v-if="saving[widget.id]">
										<q-spinner-dots size="20px" color="primary"/>
									</q-item-section>
								</q-item>
							</q-list>
						</q-item-section>
					</q-item>
				</q-list>
			</q-card-section>
		</q-card>
	</q-page>
</template>

<script>
import { defineComponent, computed, reactive, ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { useApolloClient } from '@vue/apollo-composable';

import { authzService } from '@/_services';
import { applyUserLocale, localeOptions } from '@/_services/locale.service';
import {
	ME_UPDATE_LANGUAGE,
	ME_UPDATE_TIMEZONE,
	MY_NOTIFICATION_RULES,
	NOTIFICATION_RULE_DELETE,
} from '@/graphql/queries';
import { useUserPrefsStore } from 'src/store/user-prefs.store';
import { useDashboardWidgets } from 'src/composables/useDashboardWidgets';
import { usePushNotifications } from 'src/composables';

export default defineComponent({
	name: 'SettingsPage',
	setup() {
		const $q = useQuasar();
		const { t } = useI18n({ useScope: 'global' });
		const { client } = useApolloClient();
		const prefs = useUserPrefsStore();
		const widgets = computed(() => useDashboardWidgets());

		const isLoggedIn = computed(() => authzService.currentUserValue?.id != null);

		// Push notifications: per-device Web Push subscription (persistent across
		// sessions via the browser + backend PushSubscription row).
		const {
			supported: pushSupported,
			permission: pushPermission,
			subscribed: pushSubscribed,
			busy: pushBusy,
			refresh: refreshPush,
			enable: enablePush,
			disable: disablePush,
		} = usePushNotifications();

		// Muted messages: per-user notification rules, created from the inbox's
		// "Mute similar" action. This card only lists and removes them.
		const rules = ref([]);
		const rulesLoading = ref(false);
		const deletingRuleId = ref(null);

		const ruleLabel = (rule) => {
			switch (rule.matchType) {
				case 'SENDER': return `All messages from "${rule.pattern}"`;
				case 'KEY_PREFIX': return `Messages of kind "${rule.pattern}"`;
				case 'SUBJECT_REGEX': return `Subject matches /${rule.pattern}/`;
				default: return `${rule.matchType}: ${rule.pattern}`;
			}
		};

		const loadRules = async () => {
			if (!isLoggedIn.value) return;
			rulesLoading.value = true;
			try {
				const response = await client.query({ query: MY_NOTIFICATION_RULES, fetchPolicy: 'network-only' });
				rules.value = response.data?.myNotificationRules ?? [];
			} catch (err) {
				$q.notify({ color: 'negative', icon: 'mdi-alert', message: 'Failed to load muted messages' });
			} finally {
				rulesLoading.value = false;
			}
		};

		const deleteRule = async (rule) => {
			deletingRuleId.value = rule.id;
			try {
				const response = await client.mutate({
					mutation: NOTIFICATION_RULE_DELETE,
					variables: { id: rule.id },
					fetchPolicy: 'no-cache',
				});
				if (response.data?.notificationRuleDelete?.success) {
					rules.value = rules.value.filter((r) => r.id !== rule.id);
					$q.notify({ color: 'positive', icon: 'mdi-check-circle', message: 'Rule removed', timeout: 2000 });
				} else {
					$q.notify({
						color: 'negative', icon: 'mdi-alert',
						message: response.data?.notificationRuleDelete?.error || 'Failed to remove rule',
					});
				}
			} catch (err) {
				$q.notify({ color: 'negative', icon: 'mdi-alert', message: 'Failed to remove rule' });
			} finally {
				deletingRuleId.value = null;
			}
		};

		const pushHint = computed(() => {
			if (pushPermission.value === 'denied') return 'Blocked in your browser settings — allow notifications for this site to enable.';
			if (pushSubscribed.value) return 'Enabled — you will receive message alerts on this device.';
			return 'Currently disabled on this device.';
		});

		const onTogglePush = async (value) => {
			if (value) {
				const ok = await enablePush();
				if (ok) {
					$q.notify({ color: 'positive', icon: 'mdi-check-circle', message: 'Push notifications enabled', timeout: 2000 });
				} else {
					$q.notify({
						color: 'negative',
						icon: 'mdi-alert',
						message: pushPermission.value === 'denied'
							? 'Notifications are blocked in your browser settings'
							: 'Could not enable push notifications',
					});
				}
			} else {
				await disablePush();
				$q.notify({ color: 'grey-8', icon: 'mdi-bell-off', message: 'Push notifications disabled', timeout: 2000 });
			}
		};

		onMounted(refreshPush);
		onMounted(loadRules);

		// Language preference: null = automatic (follow browser).
		const languagePref = ref(authzService.currentUserValue?.language ?? null);
		const languageOptions = computed(() => localeOptions(t('settings.language.auto')));
		const savingLanguage = ref(false);

		const onLanguageChange = async (value) => {
			const previous = languagePref.value;
			languagePref.value = value;
			savingLanguage.value = true;
			try {
				const response = await client.mutate({
					mutation: ME_UPDATE_LANGUAGE,
					variables: { language: value },
					fetchPolicy: 'no-cache',
				});
				const result = response.data.meUpdateLanguage;
				if (result?.success) {
					authzService.updateCurrentUser({ language: value ?? null });
					applyUserLocale(value); // null -> browser fallback
					$q.notify({ color: 'positive', icon: 'mdi-check-circle', message: t('settings.language.saved'), timeout: 2000 });
				} else {
					languagePref.value = previous;
					$q.notify({ color: 'negative', icon: 'mdi-alert', message: result?.error || t('settings.language.save_error') });
				}
			} catch (err) {
				languagePref.value = previous;
				$q.notify({ color: 'negative', icon: 'mdi-alert', message: t('settings.language.save_error') });
			} finally {
				savingLanguage.value = false;
			}
		};

		// Timezone preference: null = automatic (follow browser).
		const timezonePref = ref(authzService.currentUserValue?.timezone ?? null);
		const savingTimezone = ref(false);
		const timezoneOptions = computed(() => {
			const zones = [
				'Europe/Chisinau', 'Europe/Bucharest', 'Europe/Kyiv', 'Europe/London',
				'Europe/Paris', 'Europe/Berlin', 'Europe/Madrid', 'Europe/Moscow',
				'UTC', 'America/New_York', 'America/Chicago', 'America/Los_Angeles',
				'Asia/Dubai', 'Asia/Tokyo',
			];
			return [
				{ label: t('settings.timezone.auto'), value: null },
				...zones.map((z) => ({ label: z, value: z })),
			];
		});

		const onTimezoneChange = async (value) => {
			const previous = timezonePref.value;
			timezonePref.value = value;
			savingTimezone.value = true;
			try {
				const response = await client.mutate({
					mutation: ME_UPDATE_TIMEZONE,
					variables: { timezone: value },
					fetchPolicy: 'no-cache',
				});
				const result = response.data.meUpdateTimezone;
				if (result?.success) {
					authzService.updateCurrentUser({ timezone: value ?? null });
					$q.notify({ color: 'positive', icon: 'mdi-check-circle', message: t('settings.timezone.saved'), timeout: 2000 });
				} else {
					timezonePref.value = previous;
					$q.notify({ color: 'negative', icon: 'mdi-alert', message: result?.error || t('settings.timezone.save_error') });
				}
			} catch (err) {
				timezonePref.value = previous;
				$q.notify({ color: 'negative', icon: 'mdi-alert', message: t('settings.timezone.save_error') });
			} finally {
				savingTimezone.value = false;
			}
		};

		const sections = computed(() => {
			const all = widgets.value;
			return [
				{ id: 'quickAccess', title: 'Quick Access', widgets: all.filter(w => w.section === 'quickAccess') },
				{ id: 'monitoring', title: 'Device Monitoring', widgets: all.filter(w => w.section === 'monitoring') },
			];
		});

		const isVisible = (id) => prefs.isWidgetVisible(id);

		// Per-widget loading flag so checkboxes spin only for the one being toggled.
		const saving = reactive({});

		const onToggle = async (widgetId, visible) => {
			saving[widgetId] = true;
			try {
				await prefs.setWidgetVisible(widgetId, visible);
			} catch (err) {
				$q.notify({
					color: 'negative',
					icon: 'mdi-alert',
					message: 'Failed to save preference. Reverted.',
					timeout: 3000,
				});
			} finally {
				saving[widgetId] = false;
			}
		};

		return {
			isLoggedIn,
			sections,
			isVisible,
			saving,
			onToggle,
			languagePref,
			languageOptions,
			savingLanguage,
			onLanguageChange,
			timezonePref,
			timezoneOptions,
			savingTimezone,
			onTimezoneChange,
			pushSupported,
			pushPermission,
			pushSubscribed,
			pushBusy,
			pushHint,
			onTogglePush,
			rules,
			rulesLoading,
			deletingRuleId,
			ruleLabel,
			deleteRule,
		};
	},
});
</script>
