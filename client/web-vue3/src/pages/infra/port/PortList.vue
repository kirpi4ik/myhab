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
      <template v-slot:header-cell-device>
        <q-select v-model="filter" :options="deviceList" label="Device" map-options filled dense>
          <q-icon name="cancel" @click.stop.prevent="filter = null" class="cursor-pointer text-blue"/>
        </q-select>
      </template>
      <template v-slot:top>
        <q-btn icon="add" color="positive" :disable="loading" label="Add port" @click="addRow"/>
        <q-space/>
        <q-input dense debounce="300" color="primary" v-model="filter">
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
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
import {DEVICE_DELETE, PORT_LIST_ALL} from "@/graphql/queries";
import _ from "lodash";

export default defineComponent({
  name: 'PortList',
  setup() {
    const uri = '/admin/ports'
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref([]);
    const deviceList = ref([]);
    const confirmDelete = ref(false);
    const selectedRow = ref(null);
    const filter = ref('')
    const filterDevice = ref('')
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'device', label: 'Device', field: 'device', align: 'left', sortable: true, filter_type: 'select'},
      {name: 'internalRef', label: 'RefId', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'type', label: 'Type', field: 'type', align: 'left', sortable: true, filter_type: 'select'},
      {name: 'value', label: 'Val', field: 'value'},
      {name: 'actions', label: 'Actions', field: 'actions'}
    ];
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    })
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PORT_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        deviceList.value = _.transform(response.data.deviceList,
          function (result, value, key) {
            result.push(value.code)
          })
        rows.value = _.transform(response.data.devicePortList,
          function (result, value, key) {
            let port = {
              id: value.id,
              internalRef: value.internalRef,
              name: value.name,
              description: value.description,
              value: value.value,
              type: value.type,
              device: (value.device.id + ' | ' + value.device.code)
            }
            result.push(port)
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
      deviceList,
      columns,
      pagination,
      loading,
      fetchData,
      filter,
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
          mutation: DEVICE_DELETE,
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
