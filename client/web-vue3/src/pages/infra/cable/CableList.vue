<template>
  <q-page padding>
    <q-btn icon="add" color="positive" :disable="loading" label="Add cable" @click="addRow"/>
    <q-grid :data="rows" :columns="columns" :columns_filter="true" :pagination="{rowsPerPage:10}"
            class="myhab-list-qgrid">
      <template v-slot:header="props">
        <q-tr :props="props">
          <q-th
            :key="col.name"
            v-for="col in props.cols">
            {{ col.label }}
          </q-th>
        </q-tr>
      </template>
      <template v-slot:body="props">
        <q-tr :props="props" @click="onRowClick(props.row)">
          <q-td key="name" style="max-width: 50px">
            {{ props.row.id }}
          </q-td>
          <q-td key="name">
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
            <q-btn-group>
              <q-btn icon="mdi-eye-outline" @click.stop="" :href="uri+'/'+props.row.id+'/view'" target="_blank"
                     color="blue-6"/>
              <q-btn icon="mdi-note-edit-outline" color="amber-9" @click.stop="onEdit(props.row)"/>
              <q-btn icon="delete" color="negative" @click.stop="removeItem(props.row)"/>
            </q-btn-group>
          </q-td>
        </q-tr>
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
import {useQuasar} from "quasar";
import {right} from "core-js/internals/array-reduce";

export default defineComponent({
  name: 'CableList',
  setup() {
    const $q = useQuasar()
    const uri = '/admin/cables'
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref([]);
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
      {name: 'actions', label: 'Actions', field: 'actions', sortable: false, filter_type: '', align: 'right'}
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
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Remove',
        message: `Doriti sa stergeti port : ${toDelete.code} ?`,
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
          mutation: CABLE_DELETE,
          variables: {id: selectedRow.value.id},
        }).then(response => {
          fetchData()
          if (response.data.cableDelete.success) {
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
      loading,
      fetchData,
      removeItem,
      categoryList,
      columns,
      uri,
      onRowClick: (row) => {
        router.push({path: `${uri}/${row.id}/view`})
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
<style>
.myhab-list-qgrid input:first-child {
  max-width: 40px;
}
</style>
