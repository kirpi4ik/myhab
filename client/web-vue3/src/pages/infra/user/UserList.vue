<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-account-multiple" class="q-mr-sm"/>
            User List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search users..."
            clearable
            class="q-mr-sm"
            style="min-width: 250px"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-magnify"/>
            </template>
          </q-input>
          <q-btn
            color="primary"
            icon="mdi-plus-circle"
            label="Add User"
            @click="createItem"
            :disable="loading"
          />
        </div>
      </q-card-section>

      <!-- Table Section -->
      <q-table
        :rows="filteredItems"
        :columns="columns"
        :loading="loading"
        v-model:pagination="pagination"
        row-key="id"
        flat
        @row-click="(evt, row) => viewItem(row)"
      >
        <template v-slot:body-cell-username="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.username || 'Unknown'"/>
          </q-td>
        </template>

        <template v-slot:body-cell-telegramUsername="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.telegramUsername" 
              color="secondary" 
              :label="'@' + props.row.telegramUsername"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-status="props">
          <q-td :props="props">
            <q-badge 
              :label="props.row.status" 
              :color="props.row.status === 'ACTIVE' ? 'positive' : 'negative'"
            />
          </q-td>
        </template>

        <template v-slot:body-cell-enabled="props">
          <q-td :props="props">
            <q-icon 
              :name="props.row.enabled ? 'mdi-check-circle' : 'mdi-close-circle'" 
              :color="props.row.enabled ? 'positive' : 'negative'"
              size="sm"
            />
          </q-td>
        </template>

        <template v-slot:body-cell-accountLocked="props">
          <q-td :props="props">
            <q-icon 
              :name="props.row.accountLocked ? 'mdi-lock' : 'mdi-lock-open'" 
              :color="props.row.accountLocked ? 'negative' : 'positive'"
              size="sm"
            />
          </q-td>
        </template>

        <template v-slot:body-cell-tsCreated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-tsUpdated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsUpdated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn-group>
              <q-btn 
                icon="mdi-eye" 
                color="blue-6" 
                @click.stop="viewItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-pencil" 
                color="amber-7" 
                @click.stop="editItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-delete" 
                color="red-7" 
                @click.stop="deleteItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, onMounted } from 'vue';
import { format } from 'date-fns';
import { useEntityList } from '@/composables';
import { USER_DELETE, USERS_GET_ALL } from '@/graphql/queries';

export default defineComponent({
  name: 'UserList',
  setup() {
    const columns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'username', label: 'Username', field: 'username', align: 'left', sortable: true },
      { name: 'telegramUsername', label: 'Telegram', field: 'telegramUsername', align: 'left', sortable: true },
      { name: 'status', label: 'Status', field: 'status', align: 'left', sortable: true },
      { name: 'enabled', label: 'Enabled', field: 'enabled', align: 'center', sortable: true },
      { name: 'accountLocked', label: 'Locked', field: 'accountLocked', align: 'center', sortable: true },
      { name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true },
      { name: 'tsUpdated', label: 'Updated', field: 'tsUpdated', align: 'left', sortable: true },
      { name: 'actions', label: 'Actions', field: () => '', align: 'right', sortable: false }
    ];

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    const {
      filteredItems,
      loading,
      filter,
      pagination,
      fetchList,
      viewItem,
      editItem,
      createItem,
      deleteItem
    } = useEntityList({
      entityName: 'User',
      entityPath: '/admin/users',
      listQuery: USERS_GET_ALL,
      deleteMutation: USER_DELETE,
      deleteKey: 'userDeleteCascade',
      columns,
      transformAfterLoad: (user) => {
        const isActive = user.enabled && 
                        !user.accountExpired && 
                        !user.accountLocked && 
                        !user.passwordExpired;
        
        return {
          id: user.id,
          username: user.username || 'Unknown',
          telegramUsername: user.telegramUsername || null,
          status: isActive ? 'ACTIVE' : 'INACTIVE',
          enabled: user.enabled || false,
          accountExpired: user.accountExpired || false,
          accountLocked: user.accountLocked || false,
          passwordExpired: user.passwordExpired || false,
          tsCreated: user.tsCreated,
          tsUpdated: user.tsUpdated
        };
      }
    });

    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      columns,
      pagination,
      loading,
      filter,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      formatDate
    };
  }
});
</script>

<style scoped>
.myhab-list-qgrid input:first-child {
  max-width: 40px;
}
</style>
