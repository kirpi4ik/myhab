<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      <q-item>
        <q-item-section>
          <q-item-label>ID</q-item-label>
          <q-item-label caption>{{ viewItem.id }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Internal Ref</q-item-label>
          <q-item-label caption>{{ viewItem.internalRef }}</q-item-label>
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
      <q-item v-if="viewItem">
        <q-item-section>
          <q-item-label>Type</q-item-label>
          <q-item-label caption>{{ viewItem.type }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem">
        <q-item-section>
          <q-item-label>State</q-item-label>
          <q-item-label caption>{{ viewItem.state }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem">
        <q-item-section>
          <q-item-label>Value</q-item-label>
          <q-item-label caption>{{ viewItem.value }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem.device">
        <q-item-section>
          <q-item-label>Device</q-item-label>
          <q-item-label caption>{{ viewItem.device.code }} | {{ viewItem.device.name }}
            <q-btn icon="mdi-eye" :to="'/admin/devices/'+viewItem.device.id+'/view'" size="xs"/>
          </q-item-label>
        </q-item-section>
      </q-item>
      <q-table
        title="Connected cables"
        :rows="viewItem.cables"
        :columns="cableColumns"
        @row-click="onRowClick"
        row-key="id"
      >
      </q-table>
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
import {DEVICE_GET_BY_ID_CHILDS, PORT_GET_BY_ID, USER_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PortView',
  setup() {
    const uri = '/admin/ports'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const cableColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'code', label: 'Code', field: 'code', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ]

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PORT_GET_BY_ID,
        variables: {id: useRoute().params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.devicePort
        loading.value = false;
      });
    }
    onMounted(() => {
      fetchData()
    })
    return {
      uri,
      fetchData,
      viewItem,
      cableColumns,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/cables/${row.id}/view`})
        }
      }
    }
  }
});
</script>
