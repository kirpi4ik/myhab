<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && scenario">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Scenario
          </div>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Scenario Configuration</div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-input v-model="scenario.name"
                   label="Name"
                   hint="Name for the scenario"
                   clearable
                   clear-icon="close"
                   color="orange"
                   filled
                   dense>
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>

          <CodeEditor
            v-model="scenario.body"
            label="Body / Script"
            hint="Scenario script or body content (Groovy syntax)"
            icon="mdi-script-text"
            height="500px"
            language="groovy"
            theme="dark"
            :custom-completions="groovyCompletions"
          />

          <q-select v-model="selectedPorts"
                    :options="portList"
                    option-label="label"
                    option-value="id"
                    label="Connected Ports"
                    hint="Select ports for this scenario"
                    multiple
                    clearable
                    use-chips
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-ethernet"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <q-card-section class="bg-blue-grey-1">
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Information</div>
          <div class="row q-gutter-md">
            <div class="col">
              <q-icon name="mdi-identifier" class="q-mr-xs"/>
              <strong>ID:</strong> {{ scenario.id }}
            </div>
            <div class="col" v-if="scenario.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ scenario.uid }}
            </div>
            <div class="col" v-if="selectedPorts.length > 0">
              <q-icon name="mdi-ethernet" class="q-mr-xs"/>
              <strong>Connected Ports:</strong> {{ selectedPorts.length }}
            </div>
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-actions>
          <q-btn color="primary" type="submit" icon="mdi-content-save">
            Save
          </q-btn>
          <q-btn color="grey" @click="$router.go(-1)" icon="mdi-cancel">
            Cancel
          </q-btn>
          <q-space/>
          <q-btn color="info" :to="`/admin/scenarios/${$route.params.idPrimary}/view`" icon="mdi-eye" outline>
            View
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref, computed} from 'vue';
import _ from 'lodash';

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {SCENARIO_EDIT_GET_BY_ID, SCENARIO_UPDATE} from '@/graphql/queries';
import {prepareForMutation} from "@/_helpers/apollo-utils";
import CodeEditor from '@/components/CodeEditor.vue';

export default defineComponent({
  name: 'ScenarioEdit',
  components: {
    CodeEditor
  },
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const scenario = ref(null);
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const portListRaw = ref([]);
    const selectedPorts = ref([]);

    const portList = computed(() => {
      return portListRaw.value.map(port => ({
        id: port.id,
        label: `${port.internalRef} - ${port.name} (${port.device ? port.device.name : 'No device'})`,
        value: port
      }));
    });

    // Custom Groovy completions for code editor
    const groovyCompletions = [
      { label: 'DevicePort', type: 'class', info: 'Device port class' },
      { label: 'Device', type: 'class', info: 'Device class' },
      { label: 'Scenario', type: 'class', info: 'Scenario class' },
      { label: 'log', type: 'variable', info: 'Logger instance' },
      { label: 'log.info', type: 'function', info: 'Log info message' },
      { label: 'log.debug', type: 'function', info: 'Log debug message' },
      { label: 'log.error', type: 'function', info: 'Log error message' },
      { label: 'log.warn', type: 'function', info: 'Log warning message' },
    ];

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: SCENARIO_EDIT_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        scenario.value = _.cloneDeep(response.data.scenario);
        portListRaw.value = response.data.devicePortList || [];
        
        // Pre-select ports that are already connected
        if (scenario.value.ports && scenario.value.ports.length > 0) {
          selectedPorts.value = scenario.value.ports.map(port => {
            return portList.value.find(p => p.id === port.id);
          }).filter(Boolean);
        }
        
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load scenario data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching scenario:', error);
      });
    };

    const onSave = () => {
      // Prepare mutation data - remove read-only fields
      const cleanScenario = prepareForMutation(scenario.value, [
        '__typename',
        'id',
        'uid',
        'tsCreated',
        'tsUpdated',
        'entityType'
      ]);

      // Set ports to selected ports
      cleanScenario.ports = selectedPorts.value.map(port => ({id: port.id}));

      client.mutate({
        mutation: SCENARIO_UPDATE,
        variables: {
          id: scenario.value.id,
          scenario: cleanScenario
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(() => {
        $q.notify({
          color: 'positive',
          message: 'Scenario updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/scenarios/${scenario.value.id}/view`});
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to update scenario',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating scenario:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      scenario,
      loading,
      portList,
      selectedPorts,
      groovyCompletions,
      onSave
    };
  }
});

</script>

