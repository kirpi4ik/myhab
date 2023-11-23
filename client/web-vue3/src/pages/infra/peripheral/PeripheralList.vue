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
        <q-btn icon="add" color="positive" :disable="loading" label="Add peripheral" @click="addRow"/>
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
          <q-btn icon="delete" @click="removeItem(props.row)"></q-btn>
        </q-td>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {useQuasar} from 'quasar'
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";
import {PERIPHERAL_DELETE, PERIPHERAL_LIST_ALL} from "@/graphql/queries";
import _ from "lodash";

export default defineComponent({
  name: 'PeripheralList',
  setup() {
    const $q = useQuasar()
    const uri = '/admin/peripherals'
    const filter = ref('')
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref();
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Denumire', field: 'name', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
      {name: 'description', label: 'Description', align: 'left', field: 'description', sortable: true},
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
        query: PERIPHERAL_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = [];
        rows.value = _.transform(response.data.devicePeripheralList,
          function (result, value, key) {
            let device = {
              id: value.id,
              model: value.model,
              name: value.name,
              description: value.description
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
        message: `Doriti sa stergeti peripheral: ${toDelete.name} ?`,
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
          mutation: PERIPHERAL_DELETE,
          variables: {id: toDelete.id},
        }).then(response => {
          fetchData()
          if (response.data.devicePeripheralDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Peripheral deleted'
            })
          } else {
            $q.notify({
              color: 'negative',
              message: 'Failed to delete'
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
