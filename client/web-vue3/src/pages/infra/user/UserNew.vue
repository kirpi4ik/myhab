<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="user">
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
          <q-input v-model="user.telegramUsername" label="Telegram Username" clearable clear-icon="close"
                   color="orange" hint="Telegram username for bot access (without @)"/>
        </q-card-section>

        <q-separator/>

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create User"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, ref} from 'vue';
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {USER_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'UserNew',
  components: {
    EntityFormActions
  },
  setup() {
    const isPwd = ref(true);
    const confirmPwd = ref(null);
    const confirmPwdRef = ref(null);

    const {
      entity: user,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'User',
      entityPath: '/admin/users',
      createMutation: USER_CREATE,
      createMutationKey: 'userCreate',
      createVariableName: 'user',
      excludeFields: ['__typename'],
      initialData: {
        username: '',
        password: '',
        email: '',
        phoneNr: '',
        telegramUsername: '',
        passwordExpired: false,
        accountExpired: false,
        accountLocked: false,
        enabled: true
      }
    });

    const onSave = async () => {
      if (saving.value) return;
      
      // Validate password confirmation
      confirmPwdRef.value.validate();
      if (confirmPwdRef.value.hasError) {
        return;
      }

      if (!validateRequired(user.value, ['username', 'password', 'email'])) return;
      await createEntity();
    };

    return {
      user,
      isPwd,
      confirmPwd,
      confirmPwdRef,
      saving,
      onSave,
      pwdRules: [
        val => (val !== null && val !== '') || 'Field is required',
        val => (val === user.value.password) || 'Wrong confirmation password'
      ]
    };
  }
});

</script>
