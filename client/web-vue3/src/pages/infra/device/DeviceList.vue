<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-devices" class="q-mr-sm"/>
            {{ $t('device.list.title') }}
          </div>
          <q-space/>
          <q-input
            v-model="filter"
            dense
            outlined
            debounce="300"
            :placeholder="$t('device.list.search')"
            clearable
            class="q-mr-sm"
            style="min-width: 250px"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-magnify"/>
            </template>
          </q-input>
          <q-btn
            color="primary"
            icon="mdi-plus-circle"
            :label="$t('device.list.add')"
            @click="createItem"
            :disable="loading"
          />
        </div>
      </q-card-section>

      <!-- Filter Section -->
      <q-card-section class="q-pt-none">
        <table-filter-bar
          :fields="filterFields"
          :rows="filteredItems"
          v-model="filters"
        />
      </q-card-section>

      <!-- Table Section -->
      <q-table
        :rows="filteredRows"
        :columns="columns"
        :loading="loading"
        row-key="id"
        flat
        virtual-scroll
        :rows-per-page-options="[0]"
        hide-pagination
        style="max-height: calc(100vh - 250px)"
        class="sticky-header-table"
        @row-click="(evt, row) => viewItem(row)"
      >
        <template v-slot:body-cell-code="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.code || $t('fields.no_code')"/>
          </q-td>
        </template>

        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="secondary" :label="props.row.name || $t('fields.unnamed')"/>
          </q-td>
        </template>

        <template v-slot:body-cell-type="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.type" 
              color="info" 
              :label="props.row.type"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-portsCount="props">
          <q-td :props="props">
            <q-badge color="teal-7" :label="props.row.portsCount || 0">
              <q-tooltip v-if="props.row.portsCount">{{ props.row.portsCount }} port(s)</q-tooltip>
            </q-badge>
          </q-td>
        </template>

        <template v-slot:body-cell-ip="props">
          <q-td :props="props">
            <q-badge v-if="props.row.ip" color="grey-7" :label="props.row.ip">
              <q-tooltip>{{ props.row.ip }}<span v-if="props.row.port">:{{ props.row.port }}</span></q-tooltip>
            </q-badge>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-status="props">
          <q-td :props="props">
            <q-badge
              :color="getStatusColor(props.row.status)"
              :label="getStatusLabel(props.row.status)"
              :icon="getStatusIcon(props.row.status)"
            />
          </q-td>
        </template>

        <template v-slot:body-cell-tsCreated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn-group>
              <q-btn
                icon="mdi-eye"
                color="blue-6"
                @click.stop="viewItem(props.row)"
                flat
                dense
              >
                <q-tooltip>{{ $t('actions.view') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-pencil"
                color="amber-7"
                @click.stop="editItem(props.row)"
                flat
                dense
              >
                <q-tooltip>{{ $t('actions.edit') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-label-outline"
                color="teal-7"
                @click.stop="downloadLabel(props.row)"
                flat
                dense
                :loading="labelLoading === props.row.id"
              >
                <q-tooltip>{{ $t('qr.label.download') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-delete"
                color="red-7"
                @click.stop="deleteItem(props.row)"
                flat
                dense
              >
                <q-tooltip>{{ $t('actions.delete') }}</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, onMounted, ref } from 'vue';
import { format } from 'date-fns';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { useEntityList, useTableFilters } from '@/composables';
import { DEVICE_DELETE, DEVICE_LIST_ALL } from '@/graphql/queries';
import { labelService } from '@/_services';
import TableFilterBar from '@/components/filters/TableFilterBar.vue';

export default defineComponent({
  name: 'DeviceList',
  components: { TableFilterBar },
  setup() {
    const $q = useQuasar();
    const { t } = useI18n({ useScope: 'global' });
    const labelLoading = ref(null);
    const columns = [
      { name: 'id', label: t('fields.id'), field: 'id', align: 'left', sortable: true },
      { name: 'code', label: t('fields.code'), field: 'code', align: 'left', sortable: true },
      { name: 'name', label: t('fields.name'), field: 'name', align: 'left', sortable: true },
      { name: 'model', label: t('fields.model'), field: 'model', align: 'left', sortable: true },
      { name: 'type', label: t('fields.type'), field: 'type', align: 'left', sortable: true },
      { name: 'rack', label: t('fields.rack'), field: 'rack', align: 'left', sortable: true },
      { name: 'ip', label: t('fields.ip'), field: 'ip', align: 'left', sortable: true },
      { name: 'status', label: t('fields.status'), field: 'status', align: 'left', sortable: true },
      { name: 'portsCount', label: t('fields.ports'), field: 'portsCount', align: 'left', sortable: true },
      { name: 'tsCreated', label: t('fields.created'), field: 'tsCreated', align: 'left', sortable: true },
      {
        name: 'actions',
        label: t('table.header.actions'),
        field: () => '',
        align: 'right', 
        sortable: false,
        headerClasses: 'bg-grey-2',
        classes: 'bg-grey-1',
        headerStyle: 'position: sticky; right: 0; z-index: 1',
        style: 'position: sticky; right: 0'
      }
    ];

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    /**
     * Map a Device.status enum value to badge color/label/icon.
     * Mirrors the helpers in DeviceView so both surfaces render consistently.
     * `null`/`Unknown` is rendered as grey so newly-added devices that haven't
     * been probed yet stand out without screaming "OFFLINE".
     */
    const getStatusColor = (status) => {
      switch ((status || '').toUpperCase()) {
        case 'ONLINE': return 'positive';
        case 'OFFLINE': return 'negative';
        case 'DISABLED': return 'warning';
        default: return 'grey-6';
      }
    };

    const getStatusLabel = (status) => {
      const key = (status || '').toLowerCase();
      const known = ['online', 'offline', 'disabled'];
      return known.includes(key) ? t(`device.status.${key}`) : t('device.status.unknown');
    };

    const getStatusIcon = (status) => {
      switch ((status || '').toUpperCase()) {
        case 'ONLINE': return 'mdi-check-circle';
        case 'OFFLINE': return 'mdi-close-circle';
        case 'DISABLED': return 'mdi-pause-circle';
        default: return 'mdi-help-circle';
      }
    };

    /**
     * Download a printable label (with QR if enabled) for a device.
     */
    const downloadLabel = async (row) => {
      labelLoading.value = row.id;
      try {
        await labelService.downloadDeviceLabel(row.id, { template: 'brother_18mm' });
        $q.notify({ color: 'positive', message: t('qr.label.download_success'), icon: 'mdi-check-circle', position: 'top', timeout: 2000 });
      } catch (error) {
        $q.notify({ color: 'negative', message: t('qr.label.download_error', { error: error.message }), icon: 'mdi-alert-circle', position: 'top' });
      } finally {
        labelLoading.value = null;
      }
    };

    const {
      filteredItems,
      loading,
      filter,
      fetchList,
      viewItem,
      editItem,
      createItem,
      deleteItem
    } = useEntityList({
      entityName: 'Device',
      entityPath: '/admin/devices',
      listQuery: DEVICE_LIST_ALL,
      listKey: 'deviceList',
      deleteMutation: DEVICE_DELETE,
      deleteKey: 'deviceDelete',
      columns,
      transformAfterLoad: (device) => ({
        id: device.id,
        code: device.code,
        name: device.name,
        model: device.model || '-',
        type: device.type?.name || null,
        rack: device.rack?.name || '-',
        ip: device.networkAddress?.ip || null,
        port: device.networkAddress?.port || null,
        status: device.status || null,
        portsCount: device.ports?.length || 0,
        tsCreated: device.tsCreated,
        tsUpdated: device.tsUpdated
      })
    });

    // Structured per-field filters, layered on top of the text-search (filteredItems)
    const filterFields = [
      { key: 'model', label: 'Model', type: 'select' },
      { key: 'type', label: 'Type', type: 'select' },
      { key: 'rack', label: 'Rack', type: 'select' },
      { key: 'status', label: 'Status', type: 'select' },
      { key: 'code', label: 'Code', type: 'text' },
      { key: 'name', label: 'Name', type: 'text' },
    ];
    const { filters, filteredRows } = useTableFilters(filteredItems, filterFields);

    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      filteredRows,
      filterFields,
      filters,
      columns,
      loading,
      filter,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      formatDate,
      getStatusColor,
      getStatusLabel,
      getStatusIcon,
      downloadLabel,
      labelLoading
    };
  }
});
</script>
