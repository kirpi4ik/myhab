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
        <q-input dense outlined debounce="300" color="primary" v-model="filter">
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn-group>
            <q-btn icon="mdi-open-in-new" @click.stop="" :href="'/nx'+uri+'/'+props.row.id+'/view'" target="_blank"
                   color="blue-6"/>
            <q-btn icon="mdi-note-edit-outline" color="amber-7" @click.stop="onEdit(props.row)"/>
            <q-btn icon="delete" color="red-7" @click.stop="removeItem(props.row)"/>
          </q-btn-group>
        </q-td>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from "quasar";

import {PORT_DELETE_BY_ID, PORT_LIST_ALL} from "@/graphql/queries";

import _ from "lodash";



export default defineComponent({
  name: 'PortList',
  setup() {
    const uri = '/admin/ports'
    const $q = useQuasar()
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref([]);
    const deviceList = ref([]);
    const filter = ref('')
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
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Remove',
        message: `Doriti sa stergeti port : ${toDelete.name} ?`,
        ok: {
          push: true,
          color: 'negative',
          label: "Remove"
        },
        cancel: {
          push: true,
          color: 'green'
        }
      }).onOk(() => {
        client.mutate({
          mutation: PORT_DELETE_BY_ID,
          variables: {id: toDelete.id},
        }).then(response => {
          fetchData()
          if (response.data.devicePortDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Port removed'
            })
          } else {
            $q.notify({
              color: 'negative',
              message: 'Failed to remove'
            })
          }
        });
      })
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
      removeItem,
      uri,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `${uri}/${row.id}/view`})
        }
      },
      onEdit: (row) => {
        router.push({path: `${uri}/${row.id}/edit`})
      },
      addRow: () => {
        router.push({path: `${uri}/new`})
      },
    }
  }
});

</script>
