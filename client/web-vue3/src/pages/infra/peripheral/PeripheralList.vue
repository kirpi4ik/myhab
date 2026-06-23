<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-devices" class="q-mr-sm"/>
            {{ $t('peripheral.list.title') }}
          </div>
          <q-space/>
          <q-input
            v-model="filter"
            dense
            outlined
            debounce="300"
            :placeholder="$t('peripheral.list.search')"
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
            :label="$t('peripheral.list.add')"
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
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.name || $t('fields.unnamed')"/>
          </q-td>
        </template>

        <template v-slot:body-cell-category="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.category" 
              :color="getCategoryColor(props.row.category)" 
              :label="props.row.category"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-description="props">
          <q-td :props="props">
            {{ props.row.description ? (props.row.description.length > 50 ? props.row.description.substring(0, 50) + '...' : props.row.description) : '-' }}
          </q-td>
        </template>

        <template v-slot:body-cell-portsCount="props">
          <q-td :props="props">
            <q-badge color="info" :label="props.row.portsCount || 0">
              <q-tooltip v-if="props.row.portsCount">Connected to {{ props.row.portsCount }} port(s)</q-tooltip>
            </q-badge>
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
                @click.stop="viewItem(props.row)"
                color="blue-6"
                flat
                size="sm"
              >
                <q-tooltip>{{ $t('actions.view') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-pencil"
                color="amber-7"
                @click.stop="editItem(props.row)"
                flat
                size="sm"
              >
                <q-tooltip>{{ $t('actions.edit') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-label-outline"
                color="teal-7"
                @click.stop="downloadLabel(props.row)"
                flat
                size="sm"
                :loading="labelLoading === props.row.id"
              >
                <q-tooltip>{{ $t('qr.label.download') }}</q-tooltip>
              </q-btn>
              <q-btn
                icon="mdi-delete"
                color="red-7"
                @click.stop="deleteItem(props.row)"
                flat
                size="sm"
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
import {defineComponent, onMounted, ref} from "vue";
import {useQuasar} from "quasar";
import {useI18n} from "vue-i18n";
import {useEntityList, useTableFilters} from '@/composables';
import {PERIPHERAL_DELETE, PERIPHERAL_LIST_ALL} from "@/graphql/queries";
import {labelService} from '@/_services';
import {format} from 'date-fns';
import TableFilterBar from '@/components/filters/TableFilterBar.vue';

export default defineComponent({
  name: 'PeripheralList',
  components: {TableFilterBar},
  setup() {
    const $q = useQuasar();
    const {t} = useI18n({useScope: 'global'});
    const labelLoading = ref(null);
    // Define columns for the table
    const columns = [
      {name: 'id', label: t('fields.id'), field: 'id', align: 'left', sortable: true, style: 'max-width: 60px'},
      {name: 'name', label: t('fields.name'), field: 'name', align: 'left', sortable: true},
      {name: 'model', label: t('fields.model'), field: 'model', align: 'left', sortable: true},
      {name: 'category', label: t('fields.category'), field: 'category', align: 'left', sortable: true},
      {name: 'description', label: t('fields.description'), field: 'description', align: 'left', sortable: true},
      {name: 'portsCount', label: t('fields.ports'), field: 'portsCount', align: 'left', sortable: true},
      {name: 'tsCreated', label: t('fields.created'), field: 'tsCreated', align: 'left', sortable: true},
      {
        name: 'actions',
        label: t('table.header.actions'),
        field: '',
        align: 'right', 
        sortable: false,
        headerClasses: 'bg-grey-2',
        classes: 'bg-grey-1',
        headerStyle: 'position: sticky; right: 0; z-index: 1',
        style: 'position: sticky; right: 0'
      }
    ];

    // Use the entity list composable
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
      entityName: 'Peripheral',
      entityPath: '/admin/peripherals',
      listQuery: PERIPHERAL_LIST_ALL,
      deleteMutation: PERIPHERAL_DELETE,
      listKey: 'devicePeripheralList',
      columns,
      transformAfterLoad: (peripheral) => ({
        id: peripheral.id,
        name: peripheral.name,
        model: peripheral.model,
        description: peripheral.description,
        category: peripheral.category?.name || null,
        portsCount: peripheral.connectedTo?.length || 0,
        tsCreated: peripheral.tsCreated,
        tsUpdated: peripheral.tsUpdated
      })
    });

    // Structured per-field filters, layered on top of the text-search (filteredItems)
    const filterFields = [
      {key: 'model', label: 'Model', type: 'select'},
      {key: 'category', label: 'Category', type: 'select'},
      {key: 'name', label: 'Name', type: 'text'},
      {key: 'description', label: 'Description', type: 'text'},
    ];
    const {filters, filteredRows} = useTableFilters(filteredItems, filterFields);

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'dd/MM/yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    /**
     * Get color for category badge
     */
    const getCategoryColor = (category) => {
      const colors = {
        'LIGHT': 'orange-7',
        'HEAT': 'red-7',
        'TEMP': 'blue-7',
        'LOCK': 'purple-7',
        'SENSOR': 'teal-7',
        'ACTUATOR': 'green-7',
        'CAMERA': 'indigo-7',
        'ALARM': 'deep-orange-7'
      };
      return colors[category] || 'grey-7';
    };

    /**
     * Download a printable label (with QR if enabled) for a peripheral.
     */
    const downloadLabel = async (row) => {
      labelLoading.value = row.id;
      try {
        await labelService.downloadPeripheralLabel(row.id, {template: 'brother_18mm'});
        $q.notify({color: 'positive', message: t('qr.label.download_success'), icon: 'mdi-check-circle', position: 'top', timeout: 2000});
      } catch (error) {
        $q.notify({color: 'negative', message: t('qr.label.download_error', {error: error.message}), icon: 'mdi-alert-circle', position: 'top'});
      } finally {
        labelLoading.value = null;
      }
    };

    // Fetch data on mount
    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      filteredRows,
      filterFields,
      filters,
      loading,
      filter,
      columns,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      formatDate,
      getCategoryColor,
      downloadLabel,
      labelLoading
    };
  }
});

</script>
