<template>
	<q-btn size="md" round icon="mdi-thermometer-lines" class="bg-blue-grey-8 text-blue-grey-1" @click="showNSc" />
	<q-dialog
		v-model="visibleNSc"
		transition-show="jump-up"
		transition-hide="jump-down"
		class="heat-scheduler"
		:maximized="$q.platform.is.mobile ? visibleNSc : false"
	>
		<q-card class="bg-white" :style="$q.platform.is.mobile ? '' : 'width: 500px; max-width: 80vw;'">
			<q-bar class="bg-green-5 text-white">
				<q-icon name="mdi-thermometer" />
				<div>Thermostat scheduler</div>
				<q-space />
				<q-btn dense flat icon="close" v-close-popup>
					<q-tooltip class="bg-primary">Close</q-tooltip>
				</q-btn>
			</q-bar>
			<q-card-section align="around" class="scroll">
				<div style="margin-left: 5px; margin-top: 30px; margin-bottom: 10px">
					<slider
						v-model="heatScheduler.temp"
						orientation="horizontal"
						:height="16"
						:width="90 + '%'"
						:min="0"
						:max="40"
						:step="1"
						tooltip
						color="#4080f7"
						tooltipText="%v ℃"
						tooltipColor="#4080f7"
						tooltipTextColor="#FEFEFE"
						sticky
					/>
				</div>
				<div class="text-grey-10">
					<q-badge rounded color="orange-8" :label="heatScheduler.temp + ' ℃'" size="md" />
					<q-input filled v-model="heatScheduler.time" mask="time" :rules="['time']">
						<template v-slot:append>
							<q-icon name="access_time" class="cursor-pointer">
								<q-popup-proxy cover transition-show="scale" transition-hide="scale">
									<q-time v-model="heatScheduler.time" color="orange">
										<div class="row items-center justify-end">
											<q-btn v-close-popup label="Set" color="dark" flat />
										</div>
									</q-time>
								</q-popup-proxy>
							</q-icon>
						</template>
					</q-input>
					<q-btn
						label="Add"
						class="bg-green-5"
						icon="mdi-plus"
						@click="addListItemConfig(zone.id, 'key.temp.schedule.list.value', heatScheduler.time, heatScheduler.temp)"
					/>
				</div>
			</q-card-section>
			<q-card-section align="around" class="scroll">
				<q-table :rows="heatScheduler.scheduleItems" :columns="data.columns" row-key="name" dense style="width: 100%">
					<template v-slot:body-cell-actions="props">
						<q-td :props="props">
							<q-btn dense round flat color="grey" icon="delete" @click="onDelete(props.row)"></q-btn>
						</q-td>
					</template>
				</q-table>
			</q-card-section>
			<q-card-actions align="right">
				<q-btn label="CLOSE" color="secondary" v-close-popup />
			</q-card-actions>
		</q-card>
	</q-dialog>
</template>
<script>
import { defineComponent, ref } from 'vue';
import slider from 'vue3-slider';
import { useApolloClient } from '@vue/apollo-composable';
import { CONFIGURATION_ADDLIST_CONFIG_VALUE, CONFIGURATION_GET_LIST_VALUE, CONFIGURATION_REMOVE_CONFIG } from '@/graphql/queries';
import { apolloProvider } from 'boot/graphql';

export default defineComponent({
	name: 'HeatScheduler',
	props: {
		zone: Object,
	},
	components: {
		slider,
	},
	setup(props) {
		let visibleNSc = ref(false);
		const { client } = useApolloClient();
		const showNSc = (e) => {
			visibleNSc.value = true;
      e.stopPropagation()
    };

		let heatScheduler = ref({
			scheduleItems: [],
			temp: ref(10),
			time: ref('00:00'),
		});

		const scheduleInit = () => {
			client
				.query({
					query: CONFIGURATION_GET_LIST_VALUE,
					variables: {
						entityId: props.zone.id,
						entityType: 'ZONE',
						key: 'key.temp.schedule.list.value',
					},
					fetchPolicy: 'network-only',
				})
				.then(response => {
					heatScheduler.value['scheduleItems'] = [];
					response.data.configListByKey.forEach(function (config, index) {
						let configValue = JSON.parse(config.value);
						heatScheduler.value['scheduleItems'].push({ id: config.id, time: configValue.time, temp: configValue.temp });
					});
				});
		};

		const addListItemConfig = (zoneId, key, time, temp) => {
			let jsonValue = JSON.stringify({ time: time, temp: temp });
			if (jsonValue != null) {
				apolloProvider.defaultClient
					.mutate({
						mutation: CONFIGURATION_ADDLIST_CONFIG_VALUE,
						variables: { key: key, value: jsonValue, entityId: zoneId, entityType: 'ZONE' },
					})
					.then(response => {
						scheduleInit();
					});
			}
			return true;
		};

		const onDelete = row => {
			apolloProvider.defaultClient
				.mutate({
					mutation: CONFIGURATION_REMOVE_CONFIG,
					variables: { id: row.id },
				})
				.then(response => {
					scheduleInit();
				});
		};

		scheduleInit();
		return {
			addListItemConfig,
			visibleNSc,
			showNSc,
			onDelete,
			heatScheduler,
			data: {
				columns: [
					{
						name: 'time',
						required: true,
						label: 'Ora',
						align: 'left',
						field: row => row.time,
						format: val => `${val}`,
						sortable: true,
					},
					{
						name: 'temp',
						required: true,
						label: '*C',
						align: 'left',
						field: row => row.temp,
						format: val => `${val}`,
						sortable: true,
					},
					{
						name: 'actions',
						align: 'right',
						label: 'Action',
					},
				],
			},
		};
	},
});
</script>
<style>
.heat-scheduler .q-badge {
	background-color: var(--q-primary);
	border-radius: 4px;
	color: #fff;
	font-size: 22px;
	font-weight: 400;
	line-height: 12px;
	min-height: 12px;
	padding: 13px 13px;
	vertical-align: initial;
}
</style>
