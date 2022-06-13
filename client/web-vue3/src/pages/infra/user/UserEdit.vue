<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-overline">Register new user</div>
          <q-input v-model="user.username" label="Username" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="user.password" label="Password" :type="isPwd ? 'password' : 'text'" color="orange"
                   :rules="[val => !!val || 'Field is required']">
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
          <q-input v-model="user.email" label="Email" clearable clear-icon="close" type="email" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="user.phoneNr" label="Telephone number" clearable clear-icon="close" type="tel"
                   color="orange"/>
        </q-card-section>

        <q-separator/>

        <q-card-actions>
          <q-btn flat color="secondary" type="submit">
            Save
          </q-btn>
          <q-btn flat color="secondary" :to="$route.matched[$route.matched.length-2]">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {useQuasar} from 'quasar'
import {defineComponent, ref} from 'vue';
import {USER_CREATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";

export default defineComponent({
  name: 'UserEdit',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const user = ref({
      passwordExpired: false,
      accountExpired: false,
      accountLocked: false,
      enabled: false
    })
    const isPwd = ref(true)
    const confirmPwd = ref(null)
    const confirmPwdRef = ref(null)
    const onSave = () => {
      confirmPwdRef.value.validate()
      if (confirmPwdRef.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: USER_CREATE,
          variables: {user: user.value},
        }).then(response => {
        });
      }
    }
    return {
      user,
      isPwd,
      confirmPwd,
      confirmPwdRef,
      onSave,
      pwdRules: [
        val => (val !== null && val !== '') || 'Field is required',
        val => (val === user.value.password) || 'Wrong confirmation password'
      ],

    }
  }
});
</script>
