<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-script-text" class="q-mr-sm"/>
            Scenario List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search scenarios..."
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
            label="Add Scenario"
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

        <template v-slot:body-cell-body="props">
          <q-td :props="props">
            <div class="text-grey-8">
              {{ props.row.body || '-' }}
            </div>
          </q-td>
        </template>

        <template v-slot:body-cell-tsCreated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-tsUpdated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsUpdated) }}
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
import { SCENARIO_DELETE_BY_ID, SCENARIO_LIST_ALL } from '@/graphql/queries';

export default defineComponent({
  name: 'ScenarioList',
  setup() {
    const columns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
      { name: 'body', label: 'Body', field: 'body', align: 'left', sortable: true },
      { name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true },
      { name: 'tsUpdated', label: 'Updated', field: 'tsUpdated', align: 'left', sortable: true },
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
      entityName: 'Scenario',
      entityPath: '/admin/scenarios',
      listQuery: SCENARIO_LIST_ALL,
      deleteMutation: SCENARIO_DELETE_BY_ID,
      deleteKey: 'scenarioDelete',
      columns,
      transformAfterLoad: (scenario) => ({
        id: scenario.id,
        name: scenario.name || 'Unnamed',
        body: scenario.body ? (scenario.body.length > 100 ? scenario.body.substring(0, 100) + '...' : scenario.body) : '-',
        tsCreated: scenario.tsCreated,
        tsUpdated: scenario.tsUpdated
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

