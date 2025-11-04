<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-devices" class="q-mr-sm"/>
            Device List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search devices..."
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
            label="Add Device"
            @click="createItem"
            :disable="loading"
          />
        </div>
      </q-card-section>

      <!-- Table Section -->
      <q-table
        :rows="filteredItems"
        :columns="columns"
        :loading="loading"
        v-model:pagination="pagination"
        row-key="id"
        flat
        @row-click="(evt, row) => viewItem(row)"
      >
        <template v-slot:body-cell-code="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.code || 'No Code'"/>
          </q-td>
        </template>

        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="secondary" :label="props.row.name || 'Unnamed'"/>
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
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-pencil" 
                color="amber-7" 
                @click.stop="editItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-delete" 
                color="red-7" 
                @click.stop="deleteItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, onMounted } from 'vue';
import { format } from 'date-fns';
import { useEntityList } from '@/composables';
import { DEVICE_DELETE, DEVICE_LIST_ALL } from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceList',
  setup() {
    const columns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'code', label: 'Code', field: 'code', align: 'left', sortable: true },
      { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
      { name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true },
      { name: 'type', label: 'Type', field: 'type', align: 'left', sortable: true },
      { name: 'rack', label: 'Rack', field: 'rack', align: 'left', sortable: true },
      { name: 'portsCount', label: 'Ports', field: 'portsCount', align: 'left', sortable: true },
      { name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true },
      { name: 'actions', label: 'Actions', field: () => '', align: 'right', sortable: false }
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

    const {
      filteredItems,
      loading,
      filter,
      pagination,
      fetchList,
      viewItem,
      editItem,
      createItem,
      deleteItem
    } = useEntityList({
      entityName: 'Device',
      entityPath: '/admin/devices',
      listQuery: DEVICE_LIST_ALL,
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
        portsCount: device.ports?.length || 0,
        tsCreated: device.tsCreated,
        tsUpdated: device.tsUpdated
      })
    });

    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      columns,
      pagination,
      loading,
      filter,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      formatDate
    };
  }
});
</script>
