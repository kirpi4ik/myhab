<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Scenario
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

        <q-card-actions>
          <q-btn color="primary" type="submit" icon="mdi-content-save">
            Save
          </q-btn>
          <q-btn color="grey" @click="$router.go(-1)" icon="mdi-cancel">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref, computed} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {SCENARIO_LIST_ALL, SCENARIO_CREATE} from '@/graphql/queries';
import CodeEditor from '@/components/CodeEditor.vue';

export default defineComponent({
  name: 'ScenarioNew',
  components: {
    CodeEditor
  },
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const scenario = ref({
      name: '',
      body: ''
    });
    const router = useRouter();
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
      client.query({
        query: SCENARIO_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        portListRaw.value = response.data.devicePortList || [];
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to load port list',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching data:', error);
      });
    };

    const onSave = () => {
      // Prepare mutation data
      const scenarioData = {
        name: scenario.value.name,
        body: scenario.value.body,
        ports: selectedPorts.value.map(port => ({id: port.id}))
      };

      client.mutate({
        mutation: SCENARIO_CREATE,
        variables: {scenario: scenarioData},
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Scenario created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/scenarios/${response.data.scenarioCreate.id}/edit`});
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to create scenario',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating scenario:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      scenario,
      portList,
      selectedPorts,
      groovyCompletions,
      onSave
    };
  }
});

</script>

