<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="scenario">
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

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Scenario"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref, computed} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import {SCENARIO_LIST_ALL, SCENARIO_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'ScenarioNew',
  components: {
    CodeEditor,
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const portListRaw = ref([]);
    const selectedPorts = ref([]);

    const {
      entity: scenario,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Scenario',
      entityPath: '/admin/scenarios',
      createMutation: SCENARIO_CREATE,
      createMutationKey: 'scenarioCreate',
      createVariableName: 'scenario',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        body: ''
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Add ports to the payload
        transformed.ports = selectedPorts.value
          .filter(port => port && port.id)
          .map(port => ({ id: port.id }));
        return transformed;
      }
    });

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
        console.error('Error fetching data:', error);
      });
    };

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(scenario.value, ['name'])) return;
      await createEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      scenario,
      portList,
      selectedPorts,
      groovyCompletions,
      saving,
      onSave
    };
  }
});

</script>

