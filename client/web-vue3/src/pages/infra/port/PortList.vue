<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-ethernet" class="q-mr-sm"/>
            Port List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search ports..."
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
            label="Add Port"
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
        <template v-slot:body-cell-internalRef="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.internalRef"/>
          </q-td>
        </template>

        <template v-slot:body-cell-type="props">
          <q-td :props="props">
            <q-badge v-if="props.row.type" color="info" :label="props.row.type"/>
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-state="props">
          <q-td :props="props">
            <q-badge v-if="props.row.state" :color="getStateColor(props.row.state)" :label="props.row.state"/>
            <span v-else class="text-grey-6">-</span>
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
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-pencil" 
                color="amber-7" 
                @click.stop="editItem(props.row)" 
                flat
                size="sm"
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-delete" 
                color="red-7" 
                @click.stop="deleteItem(props.row)" 
                flat
                size="sm"
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
import {defineComponent, onMounted} from "vue";
import {useEntityList} from '@/composables';
import {PORT_DELETE_BY_ID, PORT_LIST_ALL} from "@/graphql/queries";

export default defineComponent({
  name: 'PortList',
  setup() {
    // Define columns for the table
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true, style: 'max-width: 60px'},
      {name: 'device', label: 'Device', field: 'deviceName', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Ref ID', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'type', label: 'Type', field: 'type', align: 'left', sortable: true},
      {name: 'state', label: 'State', field: 'state', align: 'left', sortable: true},
      {name: 'value', label: 'Value', field: 'value', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: '', align: 'right', sortable: false}
    ];

    // Use the entity list composable
    const {
      items,
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
      entityName: 'Port',
      entityPath: '/admin/ports',
      listQuery: PORT_LIST_ALL,
      deleteMutation: PORT_DELETE_BY_ID,
      listKey: 'devicePortList',
      columns,
      transformAfterLoad: (port) => ({
        id: port.id,
        internalRef: port.internalRef,
        name: port.name,
        description: port.description || '',
        value: port.value || '',
        type: port.type,
        state: port.state,
        deviceId: port.device.id,
        deviceCode: port.device.code,
        deviceName: port.device.name
      })
    });

    // Set initial pagination
    pagination.value.descending = true;

    /**
     * Get color for state badge
     */
    const getStateColor = (state) => {
      const stateColors = {
        'ACTIVE': 'positive',
        'INACTIVE': 'grey',
        'ERROR': 'negative',
        'WARNING': 'warning',
        'UNKNOW': 'grey-5'
      };
      return stateColors[state] || 'grey';
    };

    // Fetch data on mount
    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      loading,
      filter,
      pagination,
      columns,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      getStateColor
    };
  }
});

</script>
