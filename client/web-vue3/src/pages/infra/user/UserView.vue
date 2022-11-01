<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-item>
        <q-item-section avatar>
          <q-avatar>
            <img src="https://cdn.quasar.dev/img/avatar2.jpg">
          </q-avatar>
        </q-item-section>

        <q-item-section>
          <q-item-label>{{ viewItem.name }}</q-item-label>
          <q-item-label caption>{{ viewItem.username }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>First name</q-item-label>
          <q-item-label caption>{{ viewItem.firstName }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Last name</q-item-label>
          <q-item-label caption>{{ viewItem.lastName }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Email</q-item-label>
          <q-item-label caption>{{ viewItem.email }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Account Expired</q-item-label>
          <q-item-label caption>{{ viewItem.accountExpired }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Account Locked</q-item-label>
          <q-item-label caption>{{ viewItem.accountLocked }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Account Enabled</q-item-label>
          <q-item-label caption>{{ viewItem.enabled }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Password expired</q-item-label>
          <q-item-label caption>{{ viewItem.passwordExpired }}</q-item-label>
        </q-item-section>
      </q-item>


      <q-card-actions>
        <q-btn flat color="secondary" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          Edit
        </q-btn>
        <q-btn flat color="secondary" :to="$route.matched[$route.matched.length-2]">
          Cancel
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {USER_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'UserView',
  setup() {
    const uri = '/admin/users'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: USER_GET_BY_ID,
        variables: {id: useRoute().params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.userById
        loading.value = false;
      });
    }
    onMounted(() => {
      fetchData()
    })
    return {
      uri,
      fetchData,
      viewItem
    }
  }
});
</script>
