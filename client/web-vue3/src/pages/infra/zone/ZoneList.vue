<template>
  <q-page padding>
    <q-table
      :dense="$q.screen.lt.md"
      title="Devices"
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :filter="filter"
      :pagination="pagination"
      row-key="id"
      @row-click="onRowClick"
      @request="fetchData"
      binary-state-sort
    >
      <template v-slot:loading>
        <q-inner-loading showing color="primary"/>
      </template>
      <template v-slot:top>
        <q-btn icon="add" color="positive" :disable="loading" label="Add zone" @click="addRow"/>
        <q-space/>
        <q-input dense outlined debounce="300" color="primary" v-model="filter">
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>
      <template v-slot:body-cell-code="props">
        <q-td :props="props">
          <q-badge :label="props.value" class="text-weight-bold text-blue-6 text-h6 bg-transparent"/>
        </q-td>
      </template>
      <template v-slot:body-cell-name="props">
        <q-td :props="props">
          <q-badge :label="props.value" class="text-weight-bold text-blue-6 text-h6 bg-transparent"/>
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn icon="mode_edit" @click="onEdit(props.row)"></q-btn>
          <q-btn icon="delete" @click="confirmDelete = true; selectedRow=props.row"></q-btn>
        </q-td>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";
import {PERIPHERAL_DELETE, ZONES_GET_ALL} from "@/graphql/queries";
import _ from "lodash";

export default defineComponent({
  name: 'ZoneList',
  setup() {
    const uri = '/admin/zones'
    const filter = ref('')
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref();
    const confirmDelete = ref(false);
    const selectedRow = ref(null);
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: 'actions'},
    ];
    const pagination = ref({
      sortBy: 'code',
      descending: false,
      page: 1,
      rowsPerPage: 10
    })
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: ZONES_GET_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = [];
        rows.value = _.transform(response.data.zoneList,
          function (result, value, key) {
            let device = {
              id: value.id,
              name: value.name,
              description: value.description
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
      columns,
      filter,
      pagination,
      loading,
      fetchData,
      confirmDelete,
      selectedRow,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `${uri}/${row.id}/view`})
        }
      },
      onEdit: (row) => {
        router.push({path: `${uri}/${row.id}/edit`})
      },
      onDelete: () => {
        client.mutate({
          mutation: PERIPHERAL_DELETE,
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
