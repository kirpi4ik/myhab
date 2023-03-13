<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-connection"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section class="row">
          <div class="text-h4 text-secondary">Port details</div>
          <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
                 style="margin-left: 5px"/>
      </q-card-section>
      <q-expansion-item v-if="viewItem.device" class="bg-green-2"
                        expand-separator
                        icon="computer"
                        label="Device"
                        :caption="viewItem.device.code">
        <q-item-section style="padding: 5px" class="bg-green-1">
          <q-item-label class="text-h6 text-grey-6">
            {{ viewItem.device.code }}
          </q-item-label>
          <q-item-label class="text-h6 text-grey-7">
            {{ viewItem.device.name }}
          </q-item-label>
          <q-item-label caption>
            {{ viewItem.device.description }}
          </q-item-label>
          <q-item-label class="text-h5">
            <q-btn icon="mdi-eye" :to="'/admin/devices/'+viewItem.device.id+'/view'" size="xs" label="Details"
                   color="blue-4"/>
          </q-item-label>
        </q-item-section>
      </q-expansion-item>
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

      <div class="q-pa-md">
        <q-table
          title="Connected cables"
          :rows="viewItem.cables"
          :columns="cableColumns"
          @row-click="onRowClick"
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
import {PORT_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PortView',
  setup() {
    const uri = '/admin/ports'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();
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
