<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-account-circle" size="xl"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="column">
          <div class="text-h4 text-secondary">{{ viewItem.username || 'Unknown User' }}</div>
          <div class="text-subtitle2 text-grey-7">{{ fullName || 'No name provided' }}</div>
        </div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit User</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=USER'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- User Status Badge -->
      <q-card-section>
        <div class="row q-gutter-sm">
          <q-badge 
            :color="userStatus.color" 
            :label="userStatus.label"
            class="text-h6 q-pa-sm"
          >
            <q-icon :name="userStatus.icon" class="q-ml-xs"/>
          </q-badge>
        </div>
      </q-card-section>

      <q-separator/>

      <!-- User Information -->
      <q-list>
        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-identifier" class="q-mr-sm"/>
              ID
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.id }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-account" class="q-mr-sm"/>
              Username
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.username || 'Unknown'"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.firstName">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-account-box" class="q-mr-sm"/>
              First Name
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.firstName }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.lastName">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-account-box-outline" class="q-mr-sm"/>
              Last Name
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.lastName }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.email">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-email" class="q-mr-sm"/>
              Email
            </q-item-label>
            <q-item-label caption class="text-body2">
              <a :href="'mailto:' + viewItem.email">{{ viewItem.email }}</a>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.phoneNr">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-phone" class="q-mr-sm"/>
              Phone Number
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.phoneNr }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.telegramUsername">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-send" class="q-mr-sm"/>
              Telegram Username
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="secondary" :label="'@' + viewItem.telegramUsername"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Account Status Section -->
        <q-item-label header class="text-h6 text-grey-8">Account Status</q-item-label>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-check-circle" class="q-mr-sm"/>
              Enabled
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-icon 
                :name="viewItem.enabled ? 'mdi-check-circle' : 'mdi-close-circle'" 
                :color="viewItem.enabled ? 'positive' : 'negative'"
                size="sm"
              />
              {{ viewItem.enabled ? 'Yes' : 'No' }}
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-lock" class="q-mr-sm"/>
              Account Locked
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-icon 
                :name="viewItem.accountLocked ? 'mdi-lock' : 'mdi-lock-open'" 
                :color="viewItem.accountLocked ? 'negative' : 'positive'"
                size="sm"
              />
              {{ viewItem.accountLocked ? 'Yes' : 'No' }}
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-remove" class="q-mr-sm"/>
              Account Expired
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-icon 
                :name="viewItem.accountExpired ? 'mdi-alert-circle' : 'mdi-check-circle'" 
                :color="viewItem.accountExpired ? 'negative' : 'positive'"
                size="sm"
              />
              {{ viewItem.accountExpired ? 'Yes' : 'No' }}
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-key-remove" class="q-mr-sm"/>
              Password Expired
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-icon 
                :name="viewItem.passwordExpired ? 'mdi-alert-circle' : 'mdi-check-circle'" 
                :color="viewItem.passwordExpired ? 'negative' : 'positive'"
                size="sm"
              />
              {{ viewItem.passwordExpired ? 'Yes' : 'No' }}
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Timestamps Section -->
        <q-item-label header class="text-h6 text-grey-8">Timestamps</q-item-label>

        <q-item v-if="viewItem.tsCreated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-plus" class="q-mr-sm"/>
              Created
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsCreated) }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.tsUpdated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-edit" class="q-mr-sm"/>
              Last Updated
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsUpdated) }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.uid">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-key" class="q-mr-sm"/>
              UID
            </q-item-label>
            <q-item-label caption class="text-body2 text-grey-7">{{ viewItem.uid }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>

      <q-separator/>

      <!-- User Roles Section -->
      <div class="q-pa-md">
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-shield-account" class="q-mr-sm"/>
          Roles
        </div>
        
        <q-list v-if="allRoles.length > 0">
          <q-item 
            v-for="role in allRoles" 
            :key="role.id"
            :class="isRoleAssigned(role.id) ? 'bg-blue-grey-1' : ''"
          >
            <q-item-section avatar>
              <q-icon 
                :name="isRoleAssigned(role.id) ? 'mdi-checkbox-marked-circle' : 'mdi-checkbox-blank-circle-outline'" 
                :color="isRoleAssigned(role.id) ? 'positive' : 'grey-5'"
                size="md"
              />
            </q-item-section>
            <q-item-section>
              <q-item-label>
                <q-badge 
                  v-if="isRoleAssigned(role.id)"
                  color="secondary" 
                  :label="role.authority"
                  class="text-body1"
                >
                  <q-icon name="mdi-shield-check" class="q-ml-xs" size="xs"/>
                </q-badge>
                <span v-else class="text-grey-7">{{ role.authority }}</span>
              </q-item-label>
              <q-item-label caption>{{ getRoleDescription(role.authority) }}</q-item-label>
            </q-item-section>
            <q-item-section side v-if="isRoleAssigned(role.id)">
              <q-chip color="positive" text-color="white" size="sm" icon="mdi-check">
                Assigned
              </q-chip>
            </q-item-section>
          </q-item>
        </q-list>
        
        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-shield-alert" size="md"/>
          <div>No roles available</div>
        </div>
      </div>

      <q-separator/>

      <!-- Actions -->
      <q-card-actions>
        <q-btn color="primary" :to="uri +'/'+ $route.params.idPrimary+'/edit'" icon="mdi-pencil">
          Edit
        </q-btn>
        <q-btn color="grey" @click="$router.go(-1)" icon="mdi-arrow-left">
          Back
        </q-btn>
      </q-card-actions>
    </q-card>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {computed, defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";

import {useQuasar} from "quasar";

import {USER_GET_BY_ID_WITH_ROLES} from "@/graphql/queries";

import _ from 'lodash';

export default defineComponent({
  name: 'UserView',
  setup() {
    const uri = '/admin/users';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const route = useRoute();
    const allRoles = ref([]);
    const assignedRoleIds = ref([]);

    /**
     * Compute full name from first and last name
     */
    const fullName = computed(() => {
      if (!viewItem.value) return '';
      const parts = [];
      if (viewItem.value.firstName) parts.push(viewItem.value.firstName);
      if (viewItem.value.lastName) parts.push(viewItem.value.lastName);
      return parts.join(' ') || '';
    });

    /**
     * Compute user status based on account flags
     */
    const userStatus = computed(() => {
      if (!viewItem.value) {
        return {
          label: 'Unknown',
          color: 'grey',
          icon: 'mdi-help-circle'
        };
      }

      const isActive = viewItem.value.enabled && 
                      !viewItem.value.accountExpired && 
                      !viewItem.value.accountLocked && 
                      !viewItem.value.passwordExpired;

      if (isActive) {
        return {
          label: 'ACTIVE',
          color: 'positive',
          icon: 'mdi-check-circle'
        };
      } else if (viewItem.value.accountLocked) {
        return {
          label: 'LOCKED',
          color: 'negative',
          icon: 'mdi-lock'
        };
      } else if (viewItem.value.accountExpired) {
        return {
          label: 'EXPIRED',
          color: 'warning',
          icon: 'mdi-calendar-remove'
        };
      } else if (viewItem.value.passwordExpired) {
        return {
          label: 'PASSWORD EXPIRED',
          color: 'warning',
          icon: 'mdi-key-remove'
        };
      } else if (!viewItem.value.enabled) {
        return {
          label: 'DISABLED',
          color: 'grey',
          icon: 'mdi-cancel'
        };
      } else {
        return {
          label: 'INACTIVE',
          color: 'grey',
          icon: 'mdi-minus-circle'
        };
      }
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
     * Get role description
     */
    const getRoleDescription = (authority) => {
      const descriptions = {
        'ROLE_ADMIN': 'Full system access and management',
        'ROLE_USER': 'Standard user access',
        'ROLE_GUEST': 'Limited read-only access'
      };
      return descriptions[authority] || 'No description available';
    };

    /**
     * Check if a role is assigned to the user
     */
    const isRoleAssigned = (roleId) => {
      // Handle both string and number comparisons
      return assignedRoleIds.value.some(id => id == roleId);
    };

    /**
     * Fetch user data and roles from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USER_GET_BY_ID_WITH_ROLES,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.userById;
        allRoles.value = response.data.roleList || [];
        
        // Extract assigned role IDs
        assignedRoleIds.value = _.map(
          response.data.userRolesForUser, 
          (userRole) => userRole.roleId
        );
        
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load user details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching user:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      fullName,
      userStatus,
      formatDate,
      allRoles,
      assignedRoleIds,
      getRoleDescription,
      isRoleAssigned
    };
  }
});

</script>
