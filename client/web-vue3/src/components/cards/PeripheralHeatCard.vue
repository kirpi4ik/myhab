<template>
	<q-card
		class="text-white"
		:style="asset['state'] ? 'background: linear-gradient(#2e383b, #62666c)' : 'background: linear-gradient(#b8303c, #c25751)'"
	>
		<q-item>
			<q-item-section avatar>
				<q-avatar size="60px" :class="asset['state'] ? 'shadow-10 bg-blue' : 'shadow-10 bg-yellow-4'">
					<q-icon name="mdi-car-seat-heater" color="yellow-10" size="40px" v-if="!asset['state']" />
					<q-icon name="mdi-car-seat-cooler" color="white" size="40px" v-if="asset['state']" />
				</q-avatar>
			</q-item-section>

			<q-item-section>
				<q-item-label class="text-weight-medium text-h5">{{ asset.data.name }}</q-item-label>
				<q-item-label class="text-weight-light text-blue-grey-3">{{ asset.data.description }}</q-item-label>
			</q-item-section>

			<q-item-section side>
				<q-item-label>
					<q-btn size="sm" flat round icon="settings" class="text-white" />
				</q-item-label>
				<q-item-label>
					<event-logger :peripheral="peripheral" />
				</q-item-label>
			</q-item-section>
		</q-item>

		<q-separator></q-separator>
		<q-card-section>
			<div class="q-pa-sm text-grey-8">
				<toggle v-model="asset['state']" @change="heatService.toggle(peripheral)" />
			</div>
		</q-card-section>
	</q-card>
</template>
<script>
import _ from 'lodash';
import { useStore } from 'vuex';
import { heatService } from '@/_services/controls';
import EventLogger from 'components/EventLogger.vue';
import { computed, defineComponent, toRefs, watch } from 'vue';
import Toggle from '@vueform/toggle';
import { format } from 'date-fns';
import humanizeDuration from 'humanize-duration';
import { useApolloClient, useGlobalQueryLoading, useMutation } from '@vue/apollo-composable';
import {
	CACHE_DELETE,
	CACHE_GET_VALUE,
	CONFIGURATION_REMOVE_CONFIG_BY_KEY,
	CONFIGURATION_SET_VALUE,
	PERIPHERAL_GET_BY_ID,
} from '@/graphql/queries';

export default defineComponent({
	name: 'PeripheralHeatCard',
	components: {
		Toggle,
		EventLogger,
	},
	props: {
		peripheral: Object,
	},
	setup(props, { emit }) {
		const store = useStore();
		const { client } = useApolloClient();
		let { peripheral: asset } = toRefs(props);
		const portId = asset.value.data.connectedTo[0].id;

		const loadDetails = () => {
			client
				.query({
					query: PERIPHERAL_GET_BY_ID,
					variables: { id: asset.value.id },
					fetchPolicy: 'network-only',
				})
				.then(data => {
					let assetRW = _.cloneDeep(data.data.devicePeripheral);
					const { data: cfg } = client
						.query({
							query: CACHE_GET_VALUE,
							variables: { cacheName: 'expiring', cacheKey: portId },
							fetchPolicy: 'network-only',
						})
						.then(cfg => {
							if (cfg.data.cache) {
								assetRW['expiration'] = cfg.data.cache.cachedValue;
							}
							compPeripheral.value = assetRW;
						});
				});
		};

		const wsMessage = computed(() => store.getters.ws.message);
		watch(
			() => store.getters.ws.message,
			function () {
				if (wsMessage.value.eventName == 'evt_port_value_persisted') {
					let payload = JSON.parse(wsMessage.value.jsonPayload);
					if (portId == payload.p2) {
						asset.value['value'] = payload.p4;
						asset.value['state'] = payload.p4 === 'OFF';
						loadDetails();
					}
				} else if (wsMessage.value.eventName == 'evt_cfg_value_changed') {
					let payload = JSON.parse(wsMessage.value.jsonPayload);
					if (asset.value.id == payload.p3 && 'PERIPHERAL' == payload.p2) {
						loadDetails();
					}
				}
			},
		);

		const config = key =>
			_.find(asset.value.data.configurations, function (cfg) {
				return cfg.key == key;
			});

		const compPeripheral = computed({
			get: () => props.peripheral,
			set: value => emit('onUpdate', value),
		});

		const { mutate: setTimeout } = useMutation(CONFIGURATION_SET_VALUE, {
			update: () => {
				loadDetails();
			},
		});
		const { mutate: deleteCache } = useMutation(CACHE_DELETE, { variables: { cacheName: 'expiring', cacheKey: portId } });
		const { mutate: deleteTimeout } = useMutation(CONFIGURATION_REMOVE_CONFIG_BY_KEY, {
			update: () => {
				deleteCache();
				loadDetails();
			},
		});

		return {
			asset,
			format,
			config,
			humanizeDuration,
			setTimeout,
			deleteTimeout,
			portId,
			heatService,
			loading: useGlobalQueryLoading(),
		};
	},
});
</script>
<style src="@vueform/toggle/themes/default.css"></style>
