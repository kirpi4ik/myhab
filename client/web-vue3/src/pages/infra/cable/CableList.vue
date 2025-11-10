<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-ethernet-cable" class="q-mr-sm"/>
            Cable List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search cables..."
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
            label="Add Cable"
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
        virtual-scroll
        :rows-per-page-options="[0]"
        style="max-height: calc(100vh - 250px)"
        class="sticky-header-table"
        @row-click="(evt, row) => viewItem(row)"
      >
        <template v-slot:body-cell-code="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.code || 'No Code'"/>
          </q-td>
        </template>

        <template v-slot:body-cell-location="props">
          <q-td :props="props">
            <q-badge color="secondary" :label="props.row.location || '-'"/>
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
import {defineComponent, onMounted, computed} from "vue";
import {useEntityList} from '@/composables';
import {CABLE_DELETE, CABLE_LIST_ALL} from "@/graphql/queries";
import {format} from 'date-fns';

export default defineComponent({
  name: 'CableList',
  setup() {
    // Define columns for the table
    const columns = [
      {name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left', style: 'max-width: 60px'},
      {name: 'code', label: 'Code', field: 'code', sortable: true, align: 'left'},
      {name: 'location', label: 'Rack', field: 'location', sortable: true, align: 'left'},
      {name: 'category', label: 'Category', field: 'category', sortable: true, align: 'left'},
      {name: 'description', label: 'Description', field: 'description', sortable: true, align: 'left'},
      {name: 'patchPanel', label: 'Panel Port', field: 'patchPanel', sortable: true, align: 'left'},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', sortable: true, align: 'left'},
      {
        name: 'actions', 
        label: 'Actions', 
        field: '', 
        sortable: false, 
        align: 'right',
        headerClasses: 'bg-grey-2',
        classes: 'bg-grey-1',
        headerStyle: 'position: sticky; right: 0; z-index: 1',
        style: 'position: sticky; right: 0'
      }
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
      entityName: 'Cable',
      entityPath: '/admin/cables',
      listQuery: CABLE_LIST_ALL,
      deleteMutation: CABLE_DELETE,
      columns,
      transformAfterLoad: (cable) => ({
        id: cable.id,
        code: cable.code,
        location: cable.rack?.name || '-',
        category: cable.category?.name || null,
        description: cable.description,
        patchPanel: cable.patchPanel 
          ? `${cable.patchPanel.name} (${cable.patchPanelPort}/${cable.patchPanel.size})` 
          : null,
        tsCreated: cable.tsCreated,
        tsUpdated: cable.tsUpdated
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
        'CAT5': 'blue-6',
        'CAT5E': 'blue-7',
        'CAT6': 'green-7',
        'CAT6A': 'green-8',
        'CAT7': 'purple-7',
        'FIBER': 'orange-7',
        'COAX': 'brown-7',
        'POWER': 'red-7'
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
