<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-devices"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section>
        <div class="text-h4 text-secondary">Device details</div>
      </q-card-section>
      <q-item>
        <q-item-section>
          <q-item-label>Code</q-item-label>
          <q-item-label caption>{{ viewItem.code }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Model</q-item-label>
          <q-item-label caption>{{ viewItem.model }}</q-item-label>
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
      <q-item v-if="viewItem.type">
        <q-item-section>
          <q-item-label>Type</q-item-label>
          <q-item-label caption>{{ viewItem.type.name }}</q-item-label>
        </q-item-section>
      </q-item>
      <div class="q-pa-md">
        <q-table
          title="Connected ports"
          :rows="viewItem.ports"
          :columns="portColumns"
          @row-click="onRowClick"
          :pagination="pagination"
          row-key="id"
        >
        </q-table>
      </div>
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
import {DEVICE_GET_BY_ID_CHILDS, USER_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'DeviceView',
  setup() {
    const uri = '/admin/devices'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();

    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'RefId', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ]


    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_GET_BY_ID_CHILDS,
        variables: {id: useRoute().params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.device
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
      portColumns,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/ports/${row.id}/view`})
        }
      }
    }
  }
});
</script>
