<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-cable-data"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section>
        <div class="text-h4 text-secondary">Cable details</div>
      </q-card-section>
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
      <div class="q-pa-md">
        <q-table
          title="Connected to ports"
          :rows="viewItem.connectedTo"
          :columns="portColumns"
          @row-click="viewPort"
          row-key="id"
        >
        </q-table>
      </div>
      <div class="q-pa-md">
        <q-table
          title="Connected to peripheral"
          :rows="viewItem.peripherals"
          :columns="peripheralColumns"
          @row-click="viewPeripheral"
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
import {CABLE_BY_ID, DEVICE_GET_BY_ID_CHILDS, PORT_GET_BY_ID, USER_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'CableView',
  setup() {
    const uri = '/admin/cables'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();
    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Int Ref', field: 'internalRef', align: 'left', sortable: true},
    ]
    const peripheralColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
    ]

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: CABLE_BY_ID,
        variables: {id: useRoute().params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.cable
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
      peripheralColumns,
      viewPort: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/ports/${row.id}/view`})
        }
      },
      viewPeripheral: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/peripherals/${row.id}/view`})
        }
      }
    }
  }
});
</script>
