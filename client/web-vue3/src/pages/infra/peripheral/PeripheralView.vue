<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-power-socket-au"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section class="row">
        <div class="text-h4 text-secondary">Peripheral details</div>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-view-list" :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=PERIPHERAL'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-delete-outline" :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=PERIPHERAL'"
               style="margin-left: 5px"/>
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
          <div>
            <q-item-label class="text-blue-grey-6 text-weight-bold">{{ viewItem.category.name }}
            <q-btn icon="mdi-eye" :to="'/admin/pcategories/'+viewItem.category.id+'/view'" size="xs"/>
            </q-item-label>
          </div>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section v-if="viewItem.connectedTo">
          <q-item-label>Port</q-item-label>
          <div>
            <q-item-label class="text-blue-grey-6 text-weight-bold" v-for="port in viewItem.connectedTo"
                          v-bind:key="port.id">{{ port.id }} |
              {{ port.internalRef }} | {{ port.name }} &nbsp;
              <q-btn icon="mdi-eye" :to="'/admin/ports/'+port.id+'/view'" size="xs"/>
            </q-item-label>
          </div>
        </q-item-section>
      </q-item>
      <div class="q-pa-md">
        <q-table
          title="Located in zones"
          :rows="viewItem.zones"
          :columns="zoneColumns"
          @row-click="viewZone"
          :pagination="pagination"
          row-key="id"
        >
        </q-table>
      </div>
      <q-card-actions>
        <q-btn color="accent" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          Edit
        </q-btn>
        <q-btn color="info" @click="$router.go(-1)">
          Cancel
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {PERIPHERAL_GET_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PeripheralView',
  setup() {
    const uri = '/admin/peripherals'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();

    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    })
    const zoneColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ]
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
      viewItem,
      pagination,
      zoneColumns,
      viewZone: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/zones/${row.id}/view`})
        }
      }
    }
  }
});
</script>
