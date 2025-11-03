<template>
  <q-page padding>
    <q-btn 
      icon="mdi-plus-circle" 
      color="positive" 
      :disable="loading" 
      label="Add User" 
      @click="addRow" 
      class="q-mb-md"
    />
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
        <q-input 
          dense 
          outlined 
          debounce="300" 
          color="primary" 
          v-model="filter" 
          placeholder="Search users..."
        >
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
          <q-td key="username">
            <q-badge color="primary" :label="props.row.username || 'Unknown'"/>
          </q-td>
          <q-td key="status">
            <q-badge 
              :label="props.row.status" 
              :color="props.row.status === 'ACTIVE' ? 'positive' : 'negative'"
            />
          </q-td>
          <q-td key="enabled">
            <q-icon 
              :name="props.row.enabled ? 'mdi-check-circle' : 'mdi-close-circle'" 
              :color="props.row.enabled ? 'positive' : 'negative'"
              size="sm"
            />
          </q-td>
          <q-td key="accountLocked">
            <q-icon 
              :name="props.row.accountLocked ? 'mdi-lock' : 'mdi-lock-open'" 
              :color="props.row.accountLocked ? 'negative' : 'positive'"
              size="sm"
            />
          </q-td>
          <q-td key="tsCreated">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
          <q-td key="tsUpdated">
            {{ formatDate(props.row.tsUpdated) }}
          </q-td>
          <q-td key="actions">
            <q-btn-group>
              <q-btn 
                icon="mdi-open-in-new" 
                @click.stop="" 
                :href="'/'+uri+'/'+props.row.id+'/view'" 
                target="_blank"
                color="blue-6" 
                flat
              >
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-note-edit-outline" 
                color="amber-7" 
                @click.stop="onEdit(props.row)" 
                flat
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-delete" 
                color="red-7" 
                @click.stop="removeItem(props.row)" 
                flat
              >
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
import {defineComponent, onMounted, ref} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router";

import {useQuasar} from "quasar";

import {USER_DELETE, USERS_GET_ALL} from '@/graphql/queries';

import _ from 'lodash';

export default defineComponent({
  name: 'UserList',
  setup() {
    const uri = '/admin/users';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'username', label: 'Username', field: 'username', align: 'left', sortable: true},
      {name: 'status', label: 'Status', field: 'status', align: 'left', sortable: true},
      {name: 'enabled', label: 'Enabled', field: 'enabled', align: 'center', sortable: true},
      {name: 'accountLocked', label: 'Locked', field: 'accountLocked', align: 'center', sortable: true},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true},
      {name: 'tsUpdated', label: 'Updated', field: 'tsUpdated', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
    ];
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    /**
     * Fetch users from the server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USERS_GET_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = _.transform(response.data.userList,
          function (result, value) {
            const isActive = value.enabled && 
                           !value.accountExpired && 
                           !value.accountLocked && 
                           !value.passwordExpired;
            
            let user = {
              id: value.id,
              username: value.username || 'Unknown',
              status: isActive ? 'ACTIVE' : 'INACTIVE',
              enabled: value.enabled || false,
              accountExpired: value.accountExpired || false,
              accountLocked: value.accountLocked || false,
              passwordExpired: value.passwordExpired || false,
              tsCreated: value.tsCreated,
              tsUpdated: value.tsUpdated
            };
            result.push(user);
          }, []);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load users',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching users:', error);
      });
    };

    /**
     * Remove a user with confirmation dialog
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete user "${toDelete.username}"?`,
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
          mutation: USER_DELETE,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
          update: () => {
            // Prevent Apollo from processing the mutation result
          }
        }).then(response => {
          if (response.data.userDeleteCascade.success) {
            $q.notify({
              color: 'positive',
              message: 'User deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.userDeleteCascade.error || 'Failed to delete user',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete user',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting user:', error);
        });
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      rows,
      columns,
      pagination,
      loading,
      fetchData,
      filter,
      removeItem,
      uri,
      formatDate,
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
