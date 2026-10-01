<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-map-marker-multiple"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section class="row">
        <div class="text-h4 text-secondary">Zone details</div>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-view-list" :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=ZONE'"
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
          <q-item-label>Voice aliases</q-item-label>
          <q-item-label caption>
            <template v-if="voiceAliases.length > 0">
              <q-chip v-for="alias in voiceAliases" :key="alias" dense square color="blue-grey-1" text-color="blue-grey-9">
                {{ alias }}
              </q-chip>
            </template>
            <span v-else>No voice aliases</span>
          </q-item-label>
        </q-item-section>
      </q-item>
      <div class="q-pa-md">
        <q-table
          title="List of Devices"
          :rows="viewItem.devices"
          :columns="zoneColumns"
          @row-click="viewDevice"
          :pagination="pagination"
          row-key="id"
        >
        </q-table>
      </div>
      <div class="q-pa-md">
        <q-table
          title="List of Peripherals"
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

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {CONFIGURATION_GET_LIST_VALUE, ZONE_GET_BY_ID_MINIMAL} from "@/graphql/queries";
import {VOICE_ALIAS_KEY, parseVoiceAliases} from "@/_helpers";



export default defineComponent({
  name: 'ZoneView',
  setup() {
    const uri = '/admin/zones'
    const viewItem = ref()
    const voiceAliases = ref([])
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
    const peripheralColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ]
    const route = useRoute();
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: ZONE_GET_BY_ID_MINIMAL,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.zoneById
        loading.value = false;
      });
      client.query({
        query: CONFIGURATION_GET_LIST_VALUE,
        variables: {key: VOICE_ALIAS_KEY, entityId: Number(route.params.idPrimary), entityType: 'ZONE'},
        fetchPolicy: 'network-only',
      }).then(response => {
        voiceAliases.value = parseVoiceAliases(response.data.configListByKey)
      }).catch(() => { /* no aliases yet */ });
    }
    onMounted(() => {
      fetchData()
    })
    return {
      uri,
      fetchData,
      viewItem,
      voiceAliases,
      pagination,
      zoneColumns,
      peripheralColumns,
      viewDevice: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/devices/${row.id}/view`})
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
