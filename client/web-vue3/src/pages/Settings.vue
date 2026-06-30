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
import { defineComponent, computed, reactive, ref } from 'vue';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { useApolloClient } from '@vue/apollo-composable';

import { authzService } from '@/_services';
import { applyUserLocale, localeOptions } from '@/_services/locale.service';
import { ME_UPDATE_LANGUAGE, ME_UPDATE_TIMEZONE } from '@/graphql/queries';
import { useUserPrefsStore } from 'src/store/user-prefs.store';
import { useDashboardWidgets } from 'src/composables/useDashboardWidgets';

export default defineComponent({
	name: 'SettingsPage',
	setup() {
		const $q = useQuasar();
		const { t } = useI18n({ useScope: 'global' });
		const { client } = useApolloClient();
		const prefs = useUserPrefsStore();
		const widgets = computed(() => useDashboardWidgets());

		const isLoggedIn = computed(() => authzService.currentUserValue?.id != null);

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
		};
	},
});
</script>
