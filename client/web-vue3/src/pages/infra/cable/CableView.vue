<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-cable-data"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section class="row">
        <div class="text-h4 text-secondary">{{viewItem.code}}</div>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-view-list" :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=CABLE'"
               style="margin-left: 5px"/>
      </q-card-section>
      <q-item>
        <q-item-section>
          <q-item-label class="text-h6">ID</q-item-label>
          <q-item-label caption>{{ viewItem.id }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label class="text-h6">Code</q-item-label>
          <q-item-label caption>{{ viewItem.code }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label class="text-h6">Code old</q-item-label>
          <q-item-label caption>{{ viewItem.codeOld }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label class="text-h6">Code new</q-item-label>
          <q-item-label caption>{{ viewItem.codeNew }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label class="text-h6">Description</q-item-label>
          <q-item-label caption>{{ viewItem.description }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem.rack != null">
        <q-item-section>
          <q-item-label class="text-h6">Rack</q-item-label>
          <div class="text-grey-6" v-if="viewItem.rack != null">{{ viewItem.rack.name }}</div>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem.rackRowNr != null || viewItem.orderInRow != null">
        <q-item-section>
          <q-item-label class="text-h6">Position in rack</q-item-label>
          <div class="row">
            <div class="text-grey-6" v-if="viewItem.rackRowNr != null">Row: {{ viewItem.rackRowNr }}</div>
            <div class="text-grey-6" v-if="viewItem.orderInRow != null"> | Order: {{ viewItem.orderInRow }}</div>
          </div>
        </q-item-section>
      </q-item>
      <q-card-section v-if="viewItem.patchPanel">
        <q-item-section>
          <q-item-label class="text-h6">Patch panel</q-item-label>
          <div class="row q-col-gutter-lg">
            <div class="row">
              <div>Name:</div>
              <div class="text-grey-8"> {{ viewItem.patchPanel.name }}</div>
            </div>
            <div class="row">
              <div>Port:</div>
              <div class="text-grey-8"> {{ viewItem.patchPanelPort }}/{{ viewItem.patchPanel.size }}</div>
            </div>
          </div>
          <div class="row text-grey-7">
            {{ viewItem.patchPanel.description }}
          </div>
        </q-item-section>
      </q-card-section>
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
          :pagination="pagination"
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
import {CABLE_BY_ID} from "@/graphql/queries";
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
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    })
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
      pagination,
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
