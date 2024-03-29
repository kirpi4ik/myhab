<template>
  <q-page padding>
    <form @submit.prevent.stop="onUpdate" class="q-gutter-md" v-if="user">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-overline">Edit user <span class="text-blue">{{ user.firstName }} {{ user.lastName }}</span>
          </div>
          <q-input v-model="user.username"
                   label="Username"
                   clearable clear-icon="close"
                   color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="user.firstName"
                   label="First name"
                   clearable clear-icon="close"
                   color="orange"/>
          <q-input v-model="user.lastName"
                   label="Last name"
                   clearable clear-icon="close"
                   color="orange"/>
          <q-checkbox v-model="user.enabled" color="orange" label="Account enabled"/>
          <q-checkbox v-model="user.accountExpired" color="orange" label="Account expired"/>
          <q-checkbox v-model="user.accountLocked" color="orange" label="Account locked"/>
          <q-checkbox v-model="user.passwordExpired" color="orange" label="Password expired"/>

          <q-input v-model="user.email" label="Email" clearable clear-icon="close" type="email" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="user.phoneNr" label="Telephone number" clearable clear-icon="close" type="tel"
                   color="orange"/>
          <q-input ref="pwdRef" v-model="user.password"
                   label="Password"
                   :type="isPwd ? 'password' : 'text'"
                   color="orange" :rules="pwdRules">
            <template v-slot:append>
              <q-icon
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>
          <q-input ref="confirmPwdRef"
                   label="Confirm password"
                   v-model="confirmPwd"
                   :type="isPwd ? 'password' : 'text'"
                   color="orange"
                   lazy-rules
                   :rules="pwdRules">
            <template v-slot:append>
              <q-icon
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>
          <q-separator/>
          <q-list>
            <q-item tag="label" v-for="item in roles" v-bind:key="item.id">
              <q-checkbox v-model="selectedRoles" :val="item" :label="item.authority" color="teal"/>
            </q-item>
          </q-list>
        </q-card-section>

        <q-separator/>

        <q-card-actions>
          <q-btn color="accent" type="submit">
            Save
          </q-btn>
          <q-btn color="info" @click="$router.go(-1)">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {useQuasar} from 'quasar'
import {defineComponent, onMounted, ref} from 'vue';
import {USER_GET_BY_ID_WITH_ROLES, USER_VALUE_UPDATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router";
import _ from "lodash";

export default defineComponent({
  name: 'UserEdit',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const user = ref(null)
    const loading = ref(false)
    const isPwd = ref(true)
    const confirmPwd = ref(null)
    const confirmPwdRef = ref(null)
    const pwdRef = ref(null)
    const route = useRoute();
    const roles = ref([{id: 1, name: 'ROLE_ADMIN'}, {id: 2, name: 'ROLE_USER'}]);
    const selectedRoles = ref([])

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USER_GET_BY_ID_WITH_ROLES,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        user.value = _.clone(response.data.userById);
        roles.value = response.data.roleList;
        selectedRoles.value = _.transform(response.data.userRolesForUser, (result, value, key) => {
          result.push(_.find(response.data.roleList, (item) => {
            return item.id == value.roleId
          }))
        });
      });
    }
    const onUpdate = () => {
      confirmPwdRef.value.validate()
      if (confirmPwdRef.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: USER_VALUE_UPDATE,
          variables: {id: user.value.id, user: user.value},
        }).then(response => {
        });
      }
    }
    onMounted(() => {
      fetchData()
    })
    return {
      user,
      isPwd,
      confirmPwd,
      confirmPwdRef,
      pwdRef,
      onUpdate,
      roles,
      selectedRoles,
      pwdRules: [
        val => (user.value.password == '' || val === user.value.password) || 'Wrong confirmation password'
      ],

    }
  }
});
</script>
