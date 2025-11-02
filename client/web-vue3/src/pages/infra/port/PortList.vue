<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Port" @click="addRow" class="q-mb-md"/>
    <q-table
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :filter="filter"
      v-model:pagination="pagination"
      row-key="id"
      flat
      bordered
      class="myhab-list-qgrid"
    >
      <template v-slot:top-right>
        <q-input dense outlined debounce="300" color="primary" v-model="filter" placeholder="Search...">
          <template v-slot:prepend>
            <q-icon name="mdi-magnify"/>
          </template>
          <template v-slot:append v-if="filter">
            <q-icon name="mdi-close-circle" @click="filter = ''" class="cursor-pointer"/>
          </template>
        </q-input>
      </template>
      <template v-slot:body="props">
        <q-tr :props="props" @click="onRowClick(props.row)" style="cursor: pointer;">
          <q-td key="id" style="max-width: 50px">
            {{ props.row.id }}
          </q-td>
          <q-td key="device">
            {{ props.row.deviceName }}
          </q-td>
          <q-td key="internalRef">
            <q-badge color="primary" :label="props.row.internalRef"/>
          </q-td>
          <q-td key="name">
            {{ props.row.name }}
          </q-td>
          <q-td key="description">
            {{ props.row.description }}
          </q-td>
          <q-td key="type">
            <q-badge v-if="props.row.type" color="info" :label="props.row.type"/>
            <span v-else class="text-grey-6">-</span>
          </q-td>
          <q-td key="state">
            <q-badge v-if="props.row.state" :color="getStateColor(props.row.state)" :label="props.row.state"/>
            <span v-else class="text-grey-6">-</span>
          </q-td>
          <q-td key="value">
            {{ props.row.value || '-' }}
          </q-td>
          <q-td key="actions">
            <q-btn-group>
              <q-btn icon="mdi-open-in-new" @click.stop="" :href="'/'+uri+'/'+props.row.id+'/view'" target="_blank"
                     color="blue-6" flat>
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn icon="mdi-note-edit-outline" color="amber-7" @click.stop="onEdit(props.row)" flat>
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn icon="mdi-delete" color="red-7" @click.stop="removeItem(props.row)" flat>
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </q-tr>
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
    const uri = '/admin/ports';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const deviceList = ref([]);
    const filter = ref('');
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'device', label: 'Device', field: 'deviceName', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Ref ID', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'type', label: 'Type', field: 'type', align: 'left', sortable: true},
      {name: 'state', label: 'State', field: 'state', align: 'left', sortable: true},
      {name: 'value', label: 'Value', field: 'value', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
    ];
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    const getStateColor = (state) => {
      const stateColors = {
        'ACTIVE': 'positive',
        'INACTIVE': 'grey',
        'ERROR': 'negative',
        'WARNING': 'warning',
        'UNKNOW': 'grey-5'
      };
      return stateColors[state] || 'grey';
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PORT_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        deviceList.value = _.transform(response.data.deviceList,
          function (result, value) {
            result.push(value.code);
          }, []);
        
        rows.value = _.transform(response.data.devicePortList,
          function (result, value) {
            let port = {
              id: value.id,
              internalRef: value.internalRef,
              name: value.name,
              description: value.description || '',
              value: value.value || '',
              type: value.type,
              state: value.state,
              deviceId: value.device.id,
              deviceCode: value.device.code,
              deviceName: value.device.name
            };
            result.push(port);
          }, []);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load ports',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching ports:', error);
      });
    };

    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete port "${toDelete.name}" (${toDelete.internalRef})?`,
        ok: {
          push: true,
          color: 'negative',
          label: "Delete",
          icon: 'mdi-delete'
        },
        cancel: {
          push: true,
          color: 'grey',
          label: 'Cancel'
        }
      }).onOk(() => {
        client.mutate({
          mutation: PORT_DELETE_BY_ID,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
          update: () => {
            // Prevent Apollo from processing the mutation result
          }
        }).then(response => {
          if (response.data.devicePortDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Port deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.devicePortDelete.error || 'Failed to delete port',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete port',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting port:', error);
        });
      });
    };

    onMounted(() => {
      fetchData();
    });

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
      getStateColor,
      onRowClick: (row) => {
        router.push({path: `${uri}/${row.id}/view`});
      },
      onEdit: (row) => {
        router.push({path: `${uri}/${row.id}/edit`});
      },
      addRow: () => {
        router.push({path: `${uri}/new`});
      },
    };
  }
});

</script>

<style scoped>
.myhab-list-qgrid input:first-child {
  max-width: 40px;
}
</style>
