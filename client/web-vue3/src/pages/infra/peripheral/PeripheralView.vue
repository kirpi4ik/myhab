<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      <q-card-section>
        <div class="text-h4 text-secondary">Peripheral details</div>
      </q-card-section>
      <q-item>
        <q-item-section>
          <q-item-label>ID</q-item-label>
          <q-item-label caption>{{ viewItem.id }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Name</q-item-label>
          <q-item-label caption>{{ viewItem.name }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Description</q-item-label>
          <q-item-label caption>{{ viewItem.description }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Model</q-item-label>
          <q-item-label caption>{{ viewItem.model }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem.category">
        <q-item-section>
          <q-item-label>Category</q-item-label>
          <q-item-label caption>{{ viewItem.category.name }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section v-if="viewItem.connectedTo">
          <q-item-label>Port</q-item-label>
          <q-item-label caption v-for="port in viewItem.connectedTo" v-bind:key="port.id">{{ port.id }} |
            {{ port.internalRef }} | {{ port.name }}
            <q-btn icon="mdi-eye" :to="'/admin/ports/'+port.id+'/view'" size="xs"/>
          </q-item-label>
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
import {DEVICE_GET_BY_ID_CHILDS, PERIPHERAL_GET_BY_ID, PORT_GET_BY_ID, USER_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PeripheralView',
  setup() {
    const uri = '/admin/peripherals'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PERIPHERAL_GET_BY_ID,
        variables: {id: useRoute().params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.devicePeripheral
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
