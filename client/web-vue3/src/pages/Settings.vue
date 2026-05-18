<template>
	<q-page class="q-pa-md">
		<div class="row q-mb-md items-center">
			<q-icon name="mdi-cog" size="32px" class="q-mr-sm text-primary"/>
			<div class="text-h5">Settings</div>
		</div>

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
import { defineComponent, computed, reactive } from 'vue';
import { useQuasar } from 'quasar';

import { authzService } from '@/_services';
import { useUserPrefsStore } from 'src/store/user-prefs.store';
import { useDashboardWidgets } from 'src/composables/useDashboardWidgets';

export default defineComponent({
	name: 'SettingsPage',
	setup() {
		const $q = useQuasar();
		const prefs = useUserPrefsStore();
		const widgets = computed(() => useDashboardWidgets());

		const isLoggedIn = computed(() => authzService.currentUserValue?.id != null);

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
		};
	},
});
</script>
