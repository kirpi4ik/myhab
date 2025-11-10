<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-map-marker" class="q-mr-sm"/>
            Zone List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search zones..."
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
            label="Add Zone"
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
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
          </q-td>
        </template>

        <template v-slot:body-cell-parent="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.parent" 
              color="secondary" 
              :label="props.row.parent"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-subZones="props">
          <q-td :props="props">
            <q-badge color="teal-7" :label="props.row.subZones || 0">
              <q-tooltip v-if="props.row.subZones">{{ props.row.subZones }} sub-zone(s)</q-tooltip>
            </q-badge>
          </q-td>
        </template>

        <template v-slot:body-cell-peripherals="props">
          <q-td :props="props">
            <q-badge color="info" :label="props.row.peripherals || 0">
              <q-tooltip v-if="props.row.peripherals">{{ props.row.peripherals }} peripheral(s)</q-tooltip>
            </q-badge>
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
import { useEntityList } from '@/composables';
import { ZONE_DELETE, ZONES_GET_ALL } from '@/graphql/queries';

export default defineComponent({
  name: 'ZoneList',
  setup() {
    const columns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
      { name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true },
      { name: 'parent', label: 'Parent Zone', field: 'parent', align: 'left', sortable: true },
      { name: 'subZones', label: 'Sub-zones', field: 'subZones', align: 'center', sortable: true },
      { name: 'peripherals', label: 'Peripherals', field: 'peripherals', align: 'center', sortable: true },
      { 
        name: 'actions', 
        label: 'Actions', 
        field: () => '', 
        align: 'right', 
        sortable: false,
        headerClasses: 'bg-grey-2',
        classes: 'bg-grey-1',
        headerStyle: 'position: sticky; right: 0; z-index: 1',
        style: 'position: sticky; right: 0'
      }
    ];

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
      entityName: 'Zone',
      entityPath: '/admin/zones',
      listQuery: ZONES_GET_ALL,
      deleteMutation: ZONE_DELETE,
      deleteKey: 'zoneDelete',
      columns,
      transformAfterLoad: (zone) => ({
        id: zone.id,
        name: zone.name,
        description: zone.description || '-',
        parent: zone.parent?.name || null,
        subZones: zone.zones?.length || 0,
        peripherals: zone.peripherals?.length || 0
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
      deleteItem
    };
  }
});
</script>
