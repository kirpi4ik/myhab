<template>
	<q-page padding>
		<q-card flat bordered>
			<q-card-section>
				<div class="row items-center q-gutter-sm">
					<q-icon name="mdi-share-variant" size="sm" color="primary"/>
					<div class="text-h5 text-primary">Shared Links</div>
					<q-space/>
					<q-btn-toggle
						v-model="stateFilter"
						no-caps
						rounded
						toggle-color="primary"
						:options="stateOptions"
					/>
				</div>
			</q-card-section>

			<q-table
				:rows="filteredRows"
				:columns="columns"
				row-key="id"
				:loading="loading"
				flat
				bordered
				separator="cell"
				:pagination="{ rowsPerPage: 20 }"
			>
				<template v-slot:body-cell-token="props">
					<q-td :props="props">
						<code class="text-caption">{{ props.row.token.substring(0, 12) }}...</code>
						<q-btn flat round dense size="xs" icon="mdi-content-copy" @click="copyLink(props.row.token)" class="q-ml-xs">
							<q-tooltip>Copy link</q-tooltip>
						</q-btn>
					</q-td>
				</template>

				<template v-slot:body-cell-description="props">
					<q-td :props="props">
						<div
							v-if="props.row.description"
							class="ellipsis cursor-pointer"
							style="max-width: 200px;"
							@click="showDetail(props.row)"
						>
							{{ props.row.description }}
							<q-tooltip>Click to see full details</q-tooltip>
						</div>
					</q-td>
				</template>

				<template v-slot:body-cell-state="props">
					<q-td :props="props">
						<q-badge
							:color="stateColor(props.row.state)"
							:label="props.row.state"
						/>
					</q-td>
				</template>

				<template v-slot:body-cell-hasPin="props">
					<q-td :props="props">
						<q-icon
							:name="props.row.hasPin ? 'mdi-lock' : 'mdi-lock-open-outline'"
							:color="props.row.hasPin ? 'amber-8' : 'grey-5'"
							size="sm"
						/>
					</q-td>
				</template>

				<template v-slot:body-cell-usage="props">
					<q-td :props="props">
						<div class="cursor-pointer text-primary" @click="showDetail(props.row)">
							{{ props.row.actionsUsed }} / {{ props.row.actionsAllowed }}
							<q-tooltip>Click to see usage history</q-tooltip>
						</div>
					</q-td>
				</template>

				<template v-slot:body-cell-shareExpireDate="props">
					<q-td :props="props">
						{{ formatDate(props.row.shareExpireDate) }}
					</q-td>
				</template>

				<template v-slot:body-cell-tsCreated="props">
					<q-td :props="props">
						{{ formatDate(props.row.tsCreated) }}
					</q-td>
				</template>

				<template v-slot:body-cell-actions="props">
					<q-td :props="props">
						<q-btn
							flat
							dense
							round
							icon="mdi-history"
							color="primary"
							@click="showDetail(props.row)"
						>
							<q-tooltip>Usage history</q-tooltip>
						</q-btn>
						<q-btn
							v-if="props.row.state === 'VALID'"
							flat
							dense
							round
							icon="mdi-close-circle"
							color="negative"
							@click="updateState(props.row, 'DISABLED', 'Disabled by admin')"
						>
							<q-tooltip>Disable</q-tooltip>
						</q-btn>
						<q-btn
							v-if="props.row.state !== 'ARCHIVED'"
							flat
							dense
							round
							icon="mdi-archive"
							color="grey"
							@click="updateState(props.row, 'ARCHIVED', 'Archived by admin')"
						>
							<q-tooltip>Archive</q-tooltip>
						</q-btn>
						<q-btn
							v-if="props.row.state === 'DISABLED'"
							flat
							dense
							round
							icon="mdi-check-circle"
							color="positive"
							@click="updateState(props.row, 'VALID', 'Re-enabled by admin')"
						>
							<q-tooltip>Re-enable</q-tooltip>
						</q-btn>
					</q-td>
				</template>
			</q-table>
		</q-card>

		<!-- Detail Dialog -->
		<q-dialog v-model="detailDialog">
			<q-card style="min-width: 400px; max-width: 560px;">
				<q-bar class="bg-primary text-white">
					<q-icon name="mdi-information-outline" size="20px"/>
					<div class="q-ml-sm text-weight-bold">Share Link Details</div>
					<q-space/>
					<q-btn dense flat icon="close" v-close-popup/>
				</q-bar>
				<q-card-section v-if="detailRow">
					<div class="text-subtitle2 text-grey-7 q-mb-xs">Description</div>
					<div class="text-body1 q-mb-md" style="white-space: pre-wrap;">{{ detailRow.description || 'No description' }}</div>
					<q-separator class="q-mb-md"/>
					<div class="row q-col-gutter-sm">
						<div class="col-6">
							<div class="text-caption text-grey-7">Peripheral</div>
							<div>{{ detailRow.peripheralName }}</div>
						</div>
						<div class="col-6">
							<div class="text-caption text-grey-7">State</div>
							<q-badge :color="stateColor(detailRow.state)" :label="detailRow.state"/>
						</div>
						<div class="col-6">
							<div class="text-caption text-grey-7">Usage</div>
							<div>{{ detailRow.actionsUsed }} / {{ detailRow.actionsAllowed }}</div>
						</div>
						<div class="col-6">
							<div class="text-caption text-grey-7">Expires</div>
							<div>{{ formatDate(detailRow.shareExpireDate) }}</div>
						</div>
						<div class="col-6">
							<div class="text-caption text-grey-7">Created by</div>
							<div>{{ detailRow.createdByUsername }}</div>
						</div>
						<div class="col-6">
							<div class="text-caption text-grey-7">Created</div>
							<div>{{ formatDate(detailRow.tsCreated) }}</div>
						</div>
					</div>
				</q-card-section>

				<q-separator/>

				<q-expansion-item
					v-if="detailRow"
					v-model="auditExpanded"
					icon="mdi-history"
					:label="`Usage history${auditRows.length ? ' (' + auditRows.length + ')' : ''}`"
					header-class="text-primary text-weight-medium"
				>
					<q-inner-loading :showing="auditLoading" style="min-height: 60px;">
						<q-spinner-dots size="32px" color="primary"/>
					</q-inner-loading>

					<div v-if="!auditLoading && !auditRows.length" class="q-pa-md text-grey-7">
						No usage recorded yet.
					</div>

					<q-list v-if="!auditLoading && auditRows.length" separator dense>
						<q-item v-for="entry in auditRows" :key="entry.id">
							<q-item-section>
								<q-item-label>{{ formatDate(entry.tsCreated) }}</q-item-label>
								<q-item-label caption>
									<span v-if="entry.remoteAddress">{{ entry.remoteAddress }}</span>
									<span v-if="entry.userAgent" class="ellipsis-2-lines">
										{{ entry.userAgent }}
										<q-tooltip>{{ entry.userAgent }}</q-tooltip>
									</span>
								</q-item-label>
							</q-item-section>
							<q-item-section side>
								<div class="row items-center q-gutter-xs">
									<q-badge outline color="primary" :label="entry.action"/>
									<q-badge :color="auditResultColor(entry.result)" :label="auditResultLabel(entry.result)">
										<q-tooltip v-if="entry.resultDescription">{{ entry.resultDescription }}</q-tooltip>
									</q-badge>
								</div>
							</q-item-section>
						</q-item>
					</q-list>
				</q-expansion-item>
			</q-card>
		</q-dialog>
	</q-page>
</template>

<script>
import { computed, defineComponent, onMounted, ref } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar, copyToClipboard } from 'quasar';
import { format, parseISO } from 'date-fns';
import { SHARED_WIDGETS, SHARED_WIDGET_AUDIT, SHARED_WIDGET_UPDATE_STATE } from '@/graphql/queries';

export default defineComponent({
	name: 'SharedWidgetList',
	setup() {
		const $q = useQuasar();
		const { client } = useApolloClient();
		const rows = ref([]);
		const loading = ref(false);
		const stateFilter = ref(null);
		const detailDialog = ref(false);
		const detailRow = ref(null);
		const auditRows = ref([]);
		const auditLoading = ref(false);
		const auditExpanded = ref(true);

		const stateOptions = [
			{ label: 'All', value: null },
			{ label: 'Valid', value: 'VALID' },
			{ label: 'Disabled', value: 'DISABLED' },
			{ label: 'Expired', value: 'EXPIRED' },
			{ label: 'Archived', value: 'ARCHIVED' },
		];

		const columns = [
			{ name: 'token', label: 'Token', field: 'token', align: 'left', sortable: false },
			{ name: 'widgetType', label: 'Type', field: 'widgetType', align: 'left', sortable: true },
			{ name: 'peripheralName', label: 'Peripheral', field: 'peripheralName', align: 'left', sortable: true },
			{ name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true },
			{ name: 'state', label: 'State', field: 'state', align: 'center', sortable: true },
			{ name: 'hasPin', label: 'PIN', field: 'hasPin', align: 'center', sortable: true },
			{ name: 'usage', label: 'Usage', field: 'actionsUsed', align: 'center', sortable: true },
			{ name: 'shareExpireDate', label: 'Expires', field: 'shareExpireDate', align: 'left', sortable: true },
			{ name: 'createdByUsername', label: 'Created By', field: 'createdByUsername', align: 'left', sortable: true },
			{ name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true },
			{ name: 'actions', label: 'Actions', field: 'actions', align: 'center', sortable: false },
		];

		const filteredRows = computed(() => {
			if (!stateFilter.value) return rows.value;
			return rows.value.filter(r => r.state === stateFilter.value);
		});

		const fetchData = async () => {
			loading.value = true;
			try {
				const response = await client.query({
					query: SHARED_WIDGETS,
					fetchPolicy: 'network-only'
				});
				rows.value = (response.data.sharedWidgets || []).map(r => structuredClone(r));
			} catch (e) {
				console.error('Failed to load shared widgets', e);
				$q.notify({ color: 'negative', message: 'Failed to load shared links', icon: 'mdi-alert-circle', position: 'top' });
			} finally {
				loading.value = false;
			}
		};

		const updateState = async (row, newState, description) => {
			try {
				const response = await client.mutate({
					mutation: SHARED_WIDGET_UPDATE_STATE,
					variables: { id: row.id, state: newState, stateDescription: description },
					fetchPolicy: 'no-cache'
				});
				if (response.data.sharedWidgetUpdateState?.success) {
					await fetchData();
					$q.notify({ color: 'positive', message: `State updated to ${newState}`, position: 'top', timeout: 1500 });
				} else {
					$q.notify({ color: 'negative', message: response.data.sharedWidgetUpdateState?.error || 'Update failed', position: 'top' });
				}
			} catch (e) {
				console.error('Failed to update shared widget state', e);
				$q.notify({ color: 'negative', message: 'Failed to update state', position: 'top' });
			}
		};

		const stateColor = (state) => {
			switch (state) {
				case 'VALID': return 'positive';
				case 'DISABLED': return 'negative';
				case 'EXPIRED': return 'warning';
				case 'ARCHIVED': return 'grey';
				default: return 'grey';
			}
		};

		const formatDate = (dateStr) => {
			if (!dateStr) return '';
			try {
				return format(parseISO(dateStr), 'MMM d, yyyy HH:mm');
			} catch (e) {
				console.warn('Date parse error', e);
				return dateStr;
			}
		};

		const copyLink = async (token) => {
			try {
				const link = `${globalThis.location.origin}/shared/${token}`;
				await copyToClipboard(link);
				$q.notify({ color: 'positive', message: 'Link copied!', icon: 'mdi-check', position: 'top', timeout: 1500 });
			} catch {
				$q.notify({ color: 'negative', message: 'Failed to copy', position: 'top' });
			}
		};

		const auditResultColor = (result) => {
			switch (result) {
				case 'SUCCESS': return 'positive';
				case 'DENIED_PIN': return 'negative';
				case 'DENIED_STATE': return 'warning';
				default: return 'grey';
			}
		};

		const auditResultLabel = (result) => {
			switch (result) {
				case 'SUCCESS': return 'Success';
				case 'DENIED_PIN': return 'Invalid PIN';
				case 'DENIED_STATE': return 'Denied';
				default: return result;
			}
		};

		const fetchAudit = async (sharedWidgetId) => {
			auditLoading.value = true;
			auditRows.value = [];
			try {
				const response = await client.query({
					query: SHARED_WIDGET_AUDIT,
					variables: { sharedWidgetId, count: 200, offset: 0 },
					fetchPolicy: 'network-only'
				});
				auditRows.value = response.data.sharedWidgetAudit || [];
			} catch (e) {
				console.error('Failed to load shared widget audit', e);
				$q.notify({ color: 'negative', message: 'Failed to load usage history', position: 'top' });
			} finally {
				auditLoading.value = false;
			}
		};

		const showDetail = (row) => {
			detailRow.value = row;
			detailDialog.value = true;
			fetchAudit(row.id);
		};

		onMounted(() => fetchData());

		return {
			rows,
			loading,
			stateFilter,
			stateOptions,
			columns,
			filteredRows,
			detailDialog,
			detailRow,
			auditRows,
			auditLoading,
			auditExpanded,
			auditResultColor,
			auditResultLabel,
			updateState,
			stateColor,
			formatDate,
			copyLink,
			showDetail,
			fetchData,
		};
	}
});
</script>
