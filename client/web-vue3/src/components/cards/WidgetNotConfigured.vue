<template>
	<q-card flat bordered class="widget-not-configured">
		<q-card-section class="column items-center text-center q-pa-md">
			<q-icon name="mdi-tune-variant" size="lg" class="text-grey-5 q-mb-sm" />
			<div class="text-subtitle1 text-grey-8">{{ widget.label }}</div>
			<div class="text-caption text-grey-6 q-mt-xs">
				{{ t('dashboard.not_configured.needs', missing.length) }}
			</div>
		</q-card-section>

		<q-separator />

		<!--
			Admins can fix it here. Everyone else is told what is missing so they can
			ask — the underlying mutation is ROLE_ADMIN anyway, so offering the control
			would only produce a permission error.
		-->
		<q-card-section v-if="canConfigure" class="q-gutter-sm">
			<config-value-picker
				v-for="entry in missing"
				:key="entry.key"
				:config-key="entry.key"
				:label="entry.label"
				:kind="entry.kind"
				@saved="onSaved"
			/>
		</q-card-section>

		<q-card-section v-else class="q-pt-none">
			<div class="text-caption text-grey-7">{{ t('dashboard.not_configured.missing') }}</div>
			<ul class="q-my-xs q-pl-md text-caption text-grey-7">
				<li v-for="entry in missing" :key="entry.key">
					{{ entry.label }} (<code>{{ entry.key }}</code>)
				</li>
			</ul>
			<div class="text-caption text-grey-6">
				{{ t('dashboard.not_configured.admin_hint') }}
			</div>
		</q-card-section>
	</q-card>
</template>

<script>
import { computed, defineComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import ConfigValuePicker from 'components/ConfigValuePicker.vue';
import { authzService } from '@/_services';
import { Role } from '@/_helpers/role';
import { useAppConfigStore } from 'src/store/app-config.store';

/**
 * Stands in for a widget whose configuration is missing.
 *
 * Rendered by DashboardActions *instead of* the widget, which is the point: the widget
 * never mounts, so it never queries `device(id: null)` against a non-null parameter and
 * never triggers the global raw-GraphQL error toast. Previously an unconfigured widget
 * produced one such toast per dashboard load and per WebSocket refetch, with nothing
 * indicating which setting was at fault.
 */
export default defineComponent({
	name: 'WidgetNotConfigured',

	components: { ConfigValuePicker },

	props: {
		/** The registry entry, as returned by useDashboardWidgets(). */
		widget: { type: Object, required: true },
		/** Its unmet `requiredConfig` entries, from useWidgetConfigStatus(). */
		missing: { type: Array, default: () => [] },
	},

	setup() {
		const { t } = useI18n();
		const appConfig = useAppConfigStore();

		const canConfigure = computed(() =>
			(authzService.currentUserValue?.permissions || []).includes(Role.Admin),
		);

		/*
		 * Update the store directly rather than waiting for the WebSocket echo: the
		 * store is reactive, so the placeholder disappears and the real widget mounts
		 * as soon as the last missing key is set.
		 */
		const onSaved = ({ key, value }) => appConfig.updateOne(key, value);

		return { t, canConfigure, onSaved };
	},
});
</script>

<style scoped>
.widget-not-configured {
	background: #fafafa;
}
</style>
