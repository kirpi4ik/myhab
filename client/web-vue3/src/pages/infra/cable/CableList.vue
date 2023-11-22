<template>
  <q-page padding>
    <q-grid :data="rows" :columns="columns" :columns_filter="true" :pagination="{rowsPerPage:10}" >
      <template v-slot:header="props">
        <q-tr :props="props">
          <q-th
            :key="col.name"
            v-for="col in props.cols">
            {{ col.label }}
          </q-th>
        </q-tr>
      </template>
      <template v-slot:body="props" >
        <q-tr :props="props" @click="onRowClick(props.row)">
          <q-td key="name" style="max-width: 50px">
            {{ props.row.id }}
          </q-td>
          <q-td key="name" >
            {{ props.row.location }}
          </q-td>
          <q-td key="name">
            {{ props.row.category }}
          </q-td>
          <q-td key="name">
            {{ props.row.code }}
          </q-td>
          <q-td key="name">
            {{ props.row.description }}
          </q-td>
          <q-td key="name">
            {{ props.row.patchPanel }}
          </q-td>
          <q-td key="name">
            <q-btn icon="mode_edit" @click.stop="onEdit(props.row)"></q-btn>
            <q-btn icon="delete" @click="confirmDelete = true; selectedRow=props.row"></q-btn>
          </q-td>
        </q-tr>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn icon="mode_edit" @click="onEdit(props.row)"></q-btn>
          <q-btn icon="delete" @click="confirmDelete = true; selectedRow=props.row"></q-btn>
        </q-td>
      </template>
    </q-grid>


  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";
import {CABLE_DELETE, CABLE_LIST_ALL} from "@/graphql/queries";
import _ from "lodash";

export default defineComponent({
  name: 'CableList',
  setup() {
    const uri = '/admin/cables'
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref([]);
    const confirmDelete = ref(false);
    const selectedRow = ref(null);
    const categoryList = ref([]);
    const columns = [
      {
        name: 'id',
        label: 'ID',
        field: 'id',
        sortable: true,
        align: 'left'
      },
      {name: 'location', label: 'Rack', field: 'location', sortable: true, filter_type: 'select'},
      {name: 'category', label: 'Category', field: 'category', sortable: true, filter_type: 'select'},
      {name: 'code', label: 'Code', field: 'code', sortable: true},
      {name: 'description', label: 'Description', field: 'description', sortable: true},
      {name: 'patchPanel', label: 'Panel port', field: 'patchPanel', sortable: true},
      {name: 'actions', label: 'Actions', field: 'actions', sortable: false, filter_type: ''}
    ];
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: CABLE_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        categoryList.value = _.transform(response.data.cableCategoryList,
          function (result, value, key) {
            result.push(value.name)
          })
        rows.value = [];
        rows.value = _.transform(response.data.cableList,
          function (result, value, key) {
            let device = {
              id: value.id,
              location: value.rack.name,
              category: value.category != null ? value.category.name : "",
              code: value.code,
              description: value.description,
              patchPanel: value.patchPanel != null ? (value.patchPanel.name + '(' + value.patchPanelPort + '/' + value.patchPanel.size + ')') : ''
            }
            result.push(device)
          }
        );
        loading.value = false;
      });
    }
    onMounted(() => {
      fetchData()
    })
    return {
      rows,
      loading,
      fetchData,
      confirmDelete,
      selectedRow,
      categoryList,
      columns,
      onRowClick: (row) => {
          router.push({path: `${uri}/${row.id}/view`})
      },
      onEdit: (row) => {
        router.push({path: `${uri}/${row.id}/edit`})
      },
      onDelete: () => {
        client.mutate({
          mutation: CABLE_DELETE,
          variables: {id: selectedRow.value.id},
        }).then(response => {
          fetchData()
        });
      },
      addRow: () => {
        router.push({path: `${uri}/new`})
      },
    }
  }
});
</script>
