<template>
  <q-page padding>
    <q-table
      :dense="$q.screen.lt.md"
      title="Users"
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      :filter="filter"
      row-key="id"
      @row-click="onRowClick"
      @request="fetchData"
      binary-state-sort
    >
      <template v-slot:loading>
        <q-inner-loading showing color="primary"/>
      </template>
      <template v-slot:top>
        <q-btn icon="add" color="positive" :disable="loading" label="Add user" @click="addRow"/>
        <q-space/>
        <q-input dense outlined debounce="300" color="primary" v-model="filter">
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>
      <template v-slot:body-cell-username="props">
        <q-td :props="props">
          <q-badge :label="props.value" class="text-weight-bold text-blue-6 text-h6 bg-transparent"/>
        </q-td>
      </template>
      <template v-slot:body-cell-status="props">
        <q-td :props="props">
          <q-badge :label="props.value" class="bg-positive" v-if="props.value === 'ON'"/>
          <q-badge :label="props.value" class="bg-negative" v-if="props.value === 'OFF'"/>
        </q-td>
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
import {USER_DELETE, USERS_GET_ALL} from '@/graphql/queries';
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import _ from 'lodash';
import {useRouter} from "vue-router";
import {useQuasar} from "quasar";

export default defineComponent({
  name: 'UserList',
  setup() {
    const uri = '/admin/users'
    const $q = useQuasar()
    const filter = ref('')
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref();
    const columns = [
      {name: 'username', label: 'Username', field: 'username', align: 'left', sortable: true},
      {name: 'status', label: 'Status', field: 'status', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: 'actions'},
    ];
    const pagination = ref({
      sortBy: 'username',
      descending: false,
      page: 1,
      rowsPerPage: 10
    })
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USERS_GET_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = [];
        rows.value = _.transform(response.data.userList,
          function (result, value, key) {
            let user = {
              id: value.id,
              username: value.username,
              status: (value.enabled && !value.accountExpired && !value.accountLocked && !value.passwordExpired ? 'ON' : 'OFF')
            }
            result.push(user)
          }
        );
        loading.value = false;
      });
    }
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Remove',
        message: `Doriti sa stergeti utilizatorul : ${toDelete.username} ?`,
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
          mutation: USER_DELETE,
          variables: {id: toDelete.id},
        }).then(response => {
          fetchData()
          if (response.data.userDeleteCascade.success) {
            $q.notify({
              color: 'positive',
              message: 'User removed'
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
