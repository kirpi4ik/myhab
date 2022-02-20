<template>
	<q-card class="text-white" style="background: linear-gradient(#156cb8, #60a3c2)">
		<q-item>
			<q-item-section avatar>
				<q-avatar size="60px" :class="asset['state'] ? 'shadow-10 bg-blue' : 'shadow-10 bg-yellow-4'">
					<q-icon name="mdi-lightbulb-on" color="yellow-10" size="40px" v-if="!asset['state']" />
					<q-icon name="mdi-lightbulb-off" color="white" size="40px" v-if="asset['state']" />
				</q-avatar>
			</q-item-section>

			<q-item-section>
				<q-item-label class="text-weight-medium text-h5">{{ asset.data.name }}</q-item-label>
				<q-item-label class="text-weight-light text-blue-grey-3">{{ asset.data.description }}</q-item-label>
				<q-item-label class="text-weight-light text-teal-2 text-caption">
					<label v-if="config('key.on.timeout').value"
						>[ timer: {{ humanizeDuration(Number(config('key.on.timeout').value) * 1000, { largest: 2 }) }}</label
					>
					<label v-if="asset.expiration" class="text-weight-light text-blue-grey-3">
						| off at :{{ format(new Date(Number(asset.expiration)), 'HH:mm') }} </label
					>]
				</q-item-label>
			</q-item-section>

			<q-item-section side>
				<q-item-label>
					<q-btn-dropdown size="sm" flat round icon="settings" class="text-white">
						<q-list>
							<q-item
								clickable
								v-close-popup
								@click="setTimeout({ key: 'key.on.timeout', value: item.value, entityId: asset.id, entityType: 'PERIPHERAL' })"
								v-for="item in stItems"
								v-bind:key="item.value"
							>
								<q-item-section>
									<q-item-label>Timeout: {{ humanizeDuration(item.value * 1000, { largest: 2 }) }}</q-item-label>
								</q-item-section>
							</q-item>
							<q-item clickable v-close-popup @click="deleteTimeout({ id: config('key.on.timeout').id })">
								<q-item-section>
									<q-item-label>Sterge timeout</q-item-label>
								</q-item-section>
							</q-item>
							<q-item clickable v-close-popup @click="$router.push({ path: '/admin/peripherals/' + peripheral.id + '/view' })">
								<q-item-section>
									<q-item-label>Detalii</q-item-label>
								</q-item-section>
							</q-item>
						</q-list>
					</q-btn-dropdown>
				</q-item-label>
				<q-item-label>
					<event-logger :peripheral="peripheral" />
				</q-item-label>
			</q-item-section>
		</q-item>

		<q-separator></q-separator>
		<q-card-section>
			<div class="q-pa-sm text-grey-8">
				<toggle v-model="asset['state']" />
			</div>
		</q-card-section>
	</q-card>
</template>
<script>
import { defineComponent, toRefs } from 'vue';
import Toggle from '@vueform/toggle';
import { format } from 'date-fns';
import humanizeDuration from 'humanize-duration';
import _ from 'lodash';
import EventLogger from 'components/EventLogger.vue';
import { useGlobalQueryLoading, useMutation, useQuery } from '@vue/apollo-composable';
import { CONFIGURATION_SET_VALUE, CONFIGURATION_DELETE, CACHE_GET_ALL_VALUES } from '@/graphql/queries';

export default defineComponent({
	name: 'PeripheralLightCard',
	components: {
		Toggle,
		EventLogger,
	},
	props: {
		peripheral: Object,
	},
	setup(props) {
		const { peripheral: asset } = toRefs(props);
		const stItems = [
			{ value: 30 },
			{ value: 60 },
			{ value: 300 },
			{ value: 600 },
			{ value: 1800 },
			{ value: 3600 },
			{ value: 7200 },
			{ value: 10800 },
			{ value: 18000 },
		];
		const config = key =>
			_.find(asset.value.data.configurations, function (cfg) {
				return cfg.key == key;
			});

		const { mutate: setTimeout } = useMutation(CONFIGURATION_SET_VALUE, {
			refetchQueries: [{ query: CACHE_GET_ALL_VALUES, variables: { cacheName: 'expiring' } }],
		});
		const { mutate: deleteTimeout } = useMutation(CONFIGURATION_DELETE);
		return { asset, format, config, humanizeDuration, stItems, setTimeout, deleteTimeout };
	},
});
</script>
<style>
.toggle,
.toggle-container {
	width: 100% !important;
	height: 40px !important;
}

.toggle-handle {
	height: 40px !important;
	width: 40px !important;
}

.toggle-off {
	background-color: red !important;
}

.toggle-on .toggle-handle {
	background: linear-gradient(rgb(0, 191, 54), rgb(53, 113, 61)) !important;
	border: 1px solid #386668;
}

.toggle-off .toggle-handle {
	background: linear-gradient(rgb(191, 0, 0), rgb(255, 190, 98)) !important;
	border: 1px solid #386668;
}
</style>
<style src="@vueform/toggle/themes/default.css"></style>
