<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-devices" class="q-mr-sm"/>
            Peripheral List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search peripherals..."
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
            label="Add Peripheral"
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
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
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
import {PERIPHERAL_DELETE, PERIPHERAL_LIST_ALL} from "@/graphql/queries";
import {format} from 'date-fns';

export default defineComponent({
  name: 'PeripheralList',
  setup() {
    // Define columns for the table
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true, style: 'max-width: 60px'},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
      {name: 'category', label: 'Category', field: 'category', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'portsCount', label: 'Ports', field: 'portsCount', align: 'left', sortable: true},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true},
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

    // Set initial pagination
    pagination.value.descending = true;

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
      formatDate,
      getCategoryColor
    };
  }
});

</script>
