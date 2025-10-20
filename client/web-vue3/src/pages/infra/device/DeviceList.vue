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
          <q-btn icon="add" color="positive" :disable="loading" label="Add device" @click="addRow"/>
          <q-space />
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
          <q-btn-group>
            <q-btn icon="mdi-open-in-new" @click.stop="" :href="'/'+uri+'/'+props.row.id+'/view'" target="_blank"
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

import {DEVICE_DELETE, DEVICE_LIST_ALL, PORT_DELETE_BY_ID} from "@/graphql/queries";

import _ from "lodash";



export default defineComponent({
  name: 'DeviceList',
  setup() {
    const uri = '/admin/devices'
    const $q = useQuasar()
    const filter = ref('')
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref([]);
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'code', label: 'Code', field: 'code', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
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
        query: DEVICE_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = [];
        rows.value = _.transform(response.data.deviceList,
          function (result, value, key) {
            let device = {
              id: value.id,
              code: value.code,
              name: value.name
            }
            result.push(device)
          }
        );
        loading.value = false;
      });
    }
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Remove',
        message: `Doriti sa stergeti dispozitivul : ${toDelete.name} ?`,
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
          mutation: DEVICE_DELETE,
          variables: {id: toDelete.id},
        }).then(response => {
          fetchData()
          if (response.data.deviceDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Device removed'
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
      columns,
      filter,
      pagination,
      loading,
      fetchData,
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
