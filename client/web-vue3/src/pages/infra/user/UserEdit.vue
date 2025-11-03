<template>
  <q-page padding>
    <form @submit.prevent.stop="onUpdate" class="q-gutter-md" v-if="!loading && user">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit User
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ user.firstName }} {{ user.lastName }} ({{ user.username }})
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="user.username" 
            label="Username" 
            hint="Unique username for login"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Username is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-account"/>
            </template>
          </q-input>

          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-input 
                v-model="user.firstName" 
                label="First Name" 
                hint="User's first name"
                clearable 
                clear-icon="close" 
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-account-box"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-6">
              <q-input 
                v-model="user.lastName" 
                label="Last Name" 
                hint="User's last name"
                clearable 
                clear-icon="close" 
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-account-box-outline"/>
                </template>
              </q-input>
            </div>
          </div>

          <q-input 
            v-model="user.email" 
            label="Email" 
            hint="User's email address"
            clearable 
            clear-icon="close" 
            type="email" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Email is required', val => isValidEmail(val) || 'Invalid email format']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-email"/>
            </template>
          </q-input>

          <q-input 
            v-model="user.phoneNr" 
            label="Phone Number" 
            hint="Contact phone number"
            clearable 
            clear-icon="close" 
            type="tel"
            color="orange"
            filled
            dense
          >
            <template v-slot:prepend>
              <q-icon name="mdi-phone"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Password Section -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-key" class="q-mr-xs"/>
            Change Password (Optional)
          </div>
          <div class="text-caption text-grey-7 q-mb-md">
            Leave blank to keep current password
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            ref="pwdRef" 
            v-model="user.password" 
            label="New Password" 
            hint="Enter new password or leave blank"
            type="password" 
            color="orange"
            filled
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-lock"/>
            </template>
          </q-input>

          <q-input 
            ref="confirmPwdRef" 
            label="Confirm Password" 
            v-model="confirmPwd" 
            hint="Re-enter the new password"
            type="password" 
            color="orange"
            filled
            dense
            clearable
            lazy-rules 
            :rules="pwdRules"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-lock-check"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Account Status -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-shield-account" class="q-mr-xs"/>
            Account Status
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-sm">
          <q-checkbox 
            v-model="user.enabled" 
            color="positive" 
            label="Account Enabled"
          >
            <q-tooltip>Allow user to log in</q-tooltip>
          </q-checkbox>
          
          <q-checkbox 
            v-model="user.accountLocked" 
            color="negative" 
            label="Account Locked"
          >
            <q-tooltip>Prevent user from logging in</q-tooltip>
          </q-checkbox>
          
          <q-checkbox 
            v-model="user.accountExpired" 
            color="warning" 
            label="Account Expired"
          >
            <q-tooltip>Mark account as expired</q-tooltip>
          </q-checkbox>
          
          <q-checkbox 
            v-model="user.passwordExpired" 
            color="warning" 
            label="Password Expired"
          >
            <q-tooltip>Force password change on next login</q-tooltip>
          </q-checkbox>
        </q-card-section>

        <q-separator/>

        <!-- User Roles -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-shield-check" class="q-mr-xs"/>
            User Roles
          </div>
          <div class="text-caption text-grey-7 q-mb-md">
            Select roles to assign to this user
          </div>
        </q-card-section>

        <q-card-section>
          <q-list>
            <q-item 
              tag="label" 
              v-for="item in roles" 
              :key="item.id"
              clickable
              :class="isRoleSelected(item.id) ? 'bg-blue-grey-1' : ''"
            >
              <q-item-section avatar>
                <q-checkbox 
                  :model-value="isRoleSelected(item.id)"
                  @update:model-value="toggleRole(item)"
                  color="secondary"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ item.authority }}</q-item-label>
                <q-item-label caption>{{ getRoleDescription(item.authority) }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <q-card-section class="bg-blue-grey-1">
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Information</div>
          <div class="row q-gutter-md">
            <div class="col">
              <q-icon name="mdi-identifier" class="q-mr-xs"/>
              <strong>ID:</strong> {{ user.id }}
            </div>
            <div class="col" v-if="user.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ user.uid }}
            </div>
            <div class="col">
              <q-icon name="mdi-shield-account" class="q-mr-xs"/>
              <strong>Roles:</strong> {{ selectedRoles.length }}
            </div>
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Actions -->
        <q-card-actions>
          <q-btn 
            color="primary" 
            type="submit" 
            icon="mdi-content-save"
            :loading="saving"
          >
            Save
          </q-btn>
          <q-btn 
            color="grey" 
            @click="$router.go(-1)" 
            icon="mdi-cancel"
            :disable="saving"
          >
            Cancel
          </q-btn>
          <q-space/>
          <q-btn 
            color="info" 
            :to="`/admin/users/${$route.params.idPrimary}/view`" 
            icon="mdi-eye" 
            outline
            :disable="saving"
          >
            View
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {prepareForMutation} from '@/_helpers';
import {USER_GET_BY_ID_WITH_ROLES, USER_VALUE_UPDATE, ROLES_SAVE} from '@/graphql/queries';

import _ from "lodash";

export default defineComponent({
  name: 'UserEdit',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    
    const user = ref(null);
    const loading = ref(false);
    const saving = ref(false);
    const confirmPwd = ref(null);
    const confirmPwdRef = ref(null);
    const pwdRef = ref(null);
    const roles = ref([]);
    const selectedRoles = ref([]);

    /**
     * Validate email format
     */
    const isValidEmail = (email) => {
      if (!email) return false;
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
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
     * Check if a role is selected
     */
    const isRoleSelected = (roleId) => {
      // Handle both string and number comparisons
      return selectedRoles.value.some(role => role.id == roleId);
    };

    /**
     * Toggle role selection
     */
    const toggleRole = (role) => {
      // Handle both string and number comparisons
      const index = selectedRoles.value.findIndex(r => r.id == role.id);
      if (index > -1) {
        // Remove role
        selectedRoles.value.splice(index, 1);
      } else {
        // Add role
        selectedRoles.value.push(role);
      }
    };

    /**
     * Password validation rules
     */
    const pwdRules = [
      val => (!user.value?.password || val === user.value.password) || 'Passwords do not match'
    ];

    /**
     * Fetch user data and roles
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USER_GET_BY_ID_WITH_ROLES,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        user.value = _.cloneDeep(response.data.userById);
        roles.value = response.data.roleList || [];
        
        // Map user roles to selected roles
        selectedRoles.value = _.transform(
          response.data.userRolesForUser, 
          (result, value) => {
            const role = _.find(response.data.roleList, (item) => {
              // Use loose equality to handle type mismatches
              return item.id == value.roleId;
            });
            if (role) {
              result.push(role);
            }
          }, 
          []
        );
        
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load user data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching user:', error);
      });
    };

    /**
     * Update user and roles
     */
    const onUpdate = () => {
      // Validate password confirmation
      if (confirmPwdRef.value) {
        confirmPwdRef.value.validate();
        if (confirmPwdRef.value.hasError) {
          $q.notify({
            color: 'negative',
            message: 'Password confirmation does not match',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          return;
        }
      }

      saving.value = true;

      // Create a clean copy for mutation, removing Apollo-specific fields
      const cleanUser = prepareForMutation(user.value, ['__typename', 'id', 'name', 'uid', 'tsCreated', 'tsUpdated', 'roles']);
      
      // Remove password if empty (keep existing password)
      if (!cleanUser.password || cleanUser.password.trim() === '') {
        delete cleanUser.password;
      }

      // First, update the user details
      client.mutate({
        mutation: USER_VALUE_UPDATE,
        variables: {id: route.params.idPrimary, user: cleanUser},
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(response => {
        // After user update succeeds, update the roles
        const userRoles = selectedRoles.value.map(role => ({
          userId: route.params.idPrimary,
          roleId: role.id
        }));
        
        return client.mutate({
          mutation: ROLES_SAVE,
          variables: { 
            input: {
              userId: route.params.idPrimary,
              userRoles: userRoles
            }
          },
          fetchPolicy: 'no-cache',
          update: () => {
            // Prevent Apollo from processing the mutation result
          }
        });
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'User and roles updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        
        // Clear password fields
        user.value.password = null;
        confirmPwd.value = null;
        
        // Refresh the data
        fetchData();
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating user:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      user,
      loading,
      saving,
      confirmPwd,
      confirmPwdRef,
      pwdRef,
      onUpdate,
      roles,
      selectedRoles,
      pwdRules,
      isValidEmail,
      getRoleDescription,
      isRoleSelected,
      toggleRole
    };
  }
});

</script>
