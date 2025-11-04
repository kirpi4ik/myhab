<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-briefcase-clock" size="xl"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="text-h4 text-secondary">{{ viewItem.name || 'Unnamed Job' }}</div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Job</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=JOB'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- Job Information -->
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
              Description
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.description || '-' }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-state-machine" class="q-mr-sm"/>
              State
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge v-if="viewItem.state" :color="getStateColor(viewItem.state)" :label="viewItem.state"/>
              <span v-else class="text-grey-6">Not specified</span>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.scenario">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-script-text" class="q-mr-sm"/>
              Scenario
            </q-item-label>
            <q-item-label caption class="text-body2">
              {{ viewItem.scenario.name }}
              <q-btn icon="mdi-eye" :to="'/admin/scenarios/'+viewItem.scenario.id+'/view'" size="xs" flat color="blue-6" class="q-ml-sm">
                <q-tooltip>View Scenario</q-tooltip>
              </q-btn>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.uid">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-key" class="q-mr-sm"/>
              UID
            </q-item-label>
            <q-item-label caption class="text-body2 text-grey-7">{{ viewItem.uid }}</q-item-label>
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

      <!-- Cron Triggers -->
      <div class="q-pa-md" v-if="viewItem.cronTriggers && viewItem.cronTriggers.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-clock-outline" class="q-mr-sm"/>
          Cron Triggers
        </div>
        <q-list bordered separator>
          <q-item v-for="trigger in viewItem.cronTriggers" :key="trigger.id">
            <q-item-section>
              <q-item-label>
                <q-badge color="info" :label="trigger.expression"/>
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Event Triggers -->
      <div class="q-pa-md" v-if="viewItem.eventTriggers && viewItem.eventTriggers.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-bell-ring" class="q-mr-sm"/>
          Event Triggers
        </div>
        <q-list bordered separator>
          <q-item v-for="trigger in viewItem.eventTriggers" :key="trigger.id">
            <q-item-section>
              <q-item-label>
                <q-badge color="warning" :label="trigger.eventDefinition ? trigger.eventDefinition.name : 'Unknown Event'"/>
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Tags -->
      <div class="q-pa-md" v-if="viewItem.tags && viewItem.tags.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-tag-multiple" class="q-mr-sm"/>
          Tags
        </div>
        <div class="q-gutter-sm">
          <q-chip v-for="tag in viewItem.tags" :key="tag.id" color="primary" text-color="white" icon="mdi-tag">
            {{ tag.name }}
          </q-chip>
        </div>
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

import {JOB_GET_BY_ID} from "@/graphql/queries";

export default defineComponent({
  name: 'JobView',
  setup() {
    const uri = '/admin/jobs';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

    const getStateColor = (state) => {
      const stateColors = {
        'ACTIVE': 'positive',
        'INACTIVE': 'grey',
        'PAUSED': 'warning',
        'ERROR': 'negative',
        'DISABLED': 'grey-5'
      };
      return stateColors[state] || 'grey';
    };

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: JOB_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.job;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load job details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching job:', error);
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
      getStateColor,
      formatDate
    };
  }
});

</script>

