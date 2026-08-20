<template>
	<div class="config-value-picker">
		<q-select
			v-if="kind !== 'text'"
			outlined
			dense
			emit-value
			map-options
			:model-value="modelValue"
			:options="options"
			:loading="loading"
			:label="label"
			:disable="saving"
			option-value="value"
			option-label="label"
			@update:model-value="save"
		>
			<template v-slot:no-option>
				<q-item>
					<q-item-section class="text-grey-6">
						{{ loading ? t('common.loading') : t('dashboard.picker.no_options') }}
					</q-item-section>
				</q-item>
			</template>
		</q-select>

		<q-input
			v-else
			outlined
			dense
			:model-value="modelValue"
			:label="label"
			:disable="saving"
			@blur="(e) => save(e.target.value)"
		/>

		<div class="text-caption text-grey-6 q-mt-xs">
			{{ t('dashboard.picker.sets_key', { key: configKey }) }}
		</div>
	</div>
</template>

<script>
import { defineComponent, onMounted, ref } from 'vue';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { apolloClient } from 'boot/graphql';
import { APP_CONFIG_UPDATE } from 'src/graphql/queries/appConfig';
import { DEVICE_LIST_ALL } from 'src/graphql/queries/devices';
import { PERIPHERAL_LIST_ALL } from 'src/graphql/queries/peripherals';
import { ZONES_GET_ALL } from 'src/graphql/queries/zones';

/**
 * Sets one app-config key by picking a real entity instead of typing a raw id.
 *
 * The ids these keys hold (`specialDevices.*`, `specialZones.*`) are meaningless to
 * look at and impossible to guess, and the app-config admin screen is a plain key/value
 * editor — so the only way to fill one in today is to already know the answer. This
 * resolves the key's declared `kind` to the matching list query and offers the choice.
 *
 * Writes through the same `appConfigUpdate` mutation as that screen, so the value is
 * committed to the git-backed config repo exactly as if it had been typed there.
 */
const SOURCES = {
	device: { query: DEVICE_LIST_ALL, field: 'deviceList' },
	peripheral: { query: PERIPHERAL_LIST_ALL, field: 'devicePeripheralList' },
	zone: { query: ZONES_GET_ALL, field: 'zoneList' },
};

export default defineComponent({
	name: 'ConfigValuePicker',

	props: {
		/** The app-config key being set, e.g. `specialDevices.heatPump.deviceId`. */
		configKey: { type: String, required: true },
		/** Human name for the setting, shown as the field label. */
		label: { type: String, default: '' },
		/** One of device | peripheral | zone | text. */
		kind: { type: String, default: 'text' },
		modelValue: { type: [String, Number], default: null },
	},

	emits: ['saved'],

	setup(props, { emit }) {
		const { t } = useI18n();
		const $q = useQuasar();
		const options = ref([]);
		const loading = ref(false);
		const saving = ref(false);

		const load = async () => {
			const source = SOURCES[props.kind];
			if (!source) return;
			loading.value = true;
			try {
				const { data } = await apolloClient.query({
					query: source.query,
					fetchPolicy: 'cache-first',
				});
				options.value = (data?.[source.field] || []).map((row) => ({
					value: String(row.id),
					// Fall back through the fields these entities actually carry, so the
					// list is readable whichever kind it is.
					label: row.name || row.code || row.description || `#${row.id}`,
				}));
			} catch (err) {
				// Not fatal: the picker degrades to an empty list with its own message.
				console.error(`ConfigValuePicker: failed to load ${props.kind} list`, err);
			} finally {
				loading.value = false;
			}
		};

		const save = async (value) => {
			if (value == null || value === '') return;
			saving.value = true;
			try {
				const { data } = await apolloClient.mutate({
					mutation: APP_CONFIG_UPDATE,
					variables: {
						key: props.configKey,
						value: String(value),
						commitMessage: `Set ${props.configKey} from the dashboard`,
					},
				});
				const result = data?.appConfigUpdate;
				if (!result?.success) {
					throw new Error(result?.error || 'Update rejected');
				}
				$q.notify({ type: 'positive', message: t('dashboard.picker.saved', { key: props.configKey }) });
				emit('saved', { key: props.configKey, value: String(value) });
			} catch (err) {
				console.error('ConfigValuePicker: save failed', err);
				$q.notify({ type: 'negative', message: t('dashboard.picker.save_failed', { key: props.configKey }) });
			} finally {
				saving.value = false;
			}
		};

		onMounted(load);

		return { t, options, loading, saving, save };
	},
});
</script>

<style scoped>
.config-value-picker {
	font-size: 0.75rem;
}
</style>
