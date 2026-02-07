<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-script-text" size="xl"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="text-h4 text-secondary">{{ viewItem.name || 'Unnamed Scenario' }}</div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Scenario</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=SCENARIO'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- Scenario Information -->
      <q-list>
        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-identifier" class="q-mr-sm"/>
              ID
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.id }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-label" class="q-mr-sm"/>
              Name
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.name || 'Unnamed'"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-text" class="q-mr-sm"/>
              Body / Script
            </q-item-label>
            <q-item-label caption class="text-body2">
              <pre class="q-ma-none" style="white-space: pre-wrap; font-family: monospace;">{{ viewItem.body || '-' }}</pre>
            </q-item-label>
          </q-item-section>
        </q-item>


        <q-item v-if="viewItem.tsCreated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-plus" class="q-mr-sm"/>
              Created
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsCreated) }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.tsUpdated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-edit" class="q-mr-sm"/>
              Last Updated
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsUpdated) }}</q-item-label>
          </q-item-section>
        </q-item>
      </q-list>

      <q-separator/>

      <!-- Connected Ports Table -->
      <div class="q-pa-md" v-if="viewItem.ports && viewItem.ports.length > 0">
        <q-table
          title="Connected Ports"
          :rows="viewItem.ports"
          :columns="portColumns"
          @row-click="onPortRowClick"
          :pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="onPortRowClick(props.row)" style="cursor: pointer;">
              <q-td key="id">
                {{ props.row.id }}
              </q-td>
              <q-td key="internalRef">
                <q-badge color="primary" :label="props.row.internalRef"/>
              </q-td>
              <q-td key="name">
                {{ props.row.name }}
              </q-td>
              <q-td key="device">
                {{ props.row.device ? props.row.device.name : '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </div>
      <div v-else class="q-pa-md text-center text-grey-6">
        <q-icon name="mdi-ethernet" size="md"/>
        <div>No ports connected to this scenario</div>
      </div>

      <q-separator/>

      <!-- Actions -->
      <q-card-actions>
        <q-btn color="primary" :to="uri +'/'+ $route.params.idPrimary+'/edit'" icon="mdi-pencil">
          Edit
        </q-btn>
        <q-btn color="grey" @click="$router.go(-1)" icon="mdi-arrow-left">
          Back
        </q-btn>
      </q-card-actions>
    </q-card>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from "quasar";

import {SCENARIO_GET_BY_ID} from "@/graphql/queries";

export default defineComponent({
  name: 'ScenarioView',
  setup() {
    const uri = '/admin/scenarios';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });
    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Ref ID', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'device', label: 'Device', field: row => row.device ? row.device.name : '', align: 'left', sortable: true},
    ];

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: SCENARIO_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.scenario;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load scenario details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching scenario:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      pagination,
      portColumns,
      formatDate,
      onPortRowClick: (row) => {
        router.push({path: `/admin/ports/${row.id}/view`});
      }
    };
  }
});

</script>

