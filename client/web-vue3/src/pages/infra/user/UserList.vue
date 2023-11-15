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
          <q-btn icon="mode_edit" @click="onEdit(props.row)"></q-btn>
          <q-btn icon="delete" @click="confirmDelete = true; selectedRow=props.row"></q-btn>
        </q-td>
      </template>
    </q-table>
    <q-dialog v-model="confirmDelete" transition-show="jump-up" transition-hide="jump-down">
      <q-card>
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="lock"/>
          <div>Warning</div>
          <q-space/>
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip class="bg-primary">Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-card-section class="row items-center">
          <q-avatar icon="delete" color="red" text-color="white"/>
          <span class="q-ml-sm">Do you want to remove the user  <b class="text-h6 text-red">{{
              selectedRow.username
            }}</b> ?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Delete" color="red" v-close-popup
                 @click="onDelete();confirmDelete=false"/>
          <q-btn flat label="Cancel" color="positive" v-close-popup/>
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script>
import {USER_DELETE, USERS_GET_ALL} from '@/graphql/queries';
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import _ from 'lodash';
import {useRouter} from "vue-router";

export default defineComponent({
  name: 'UserList',
  setup() {
    const uri = '/admin/users'
    const filter = ref('')
    const {client} = useApolloClient();
    const loading = ref(false)
    const router = useRouter();
    const rows = ref();
    const confirmDelete = ref(false);
    const selectedRow = ref(null);
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
          mutation: USER_DELETE,
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
