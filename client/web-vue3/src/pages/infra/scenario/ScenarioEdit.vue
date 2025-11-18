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
        <EntityInfoPanel
          :entity="scenario"
          icon="mdi-script-text"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Connected Ports', value: selectedPorts.length }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/scenarios/${$route.params.idPrimary}/view`"
          save-label="Save Scenario"
        />
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
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import {SCENARIO_EDIT_GET_BY_ID, SCENARIO_UPDATE} from '@/graphql/queries';

export default defineComponent({
  name: 'ScenarioEdit',
  components: {
    CodeEditor,
    EntityInfoPanel,
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const route = useRoute();
    
    const portListRaw = ref([]);
    const selectedPorts = ref([]);

    // Use CRUD composable
    const {
      entity: scenario,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Scenario',
      entityPath: '/admin/scenarios',
      getQuery: SCENARIO_EDIT_GET_BY_ID,
      getQueryKey: 'scenario',
      updateMutation: SCENARIO_UPDATE,
      updateMutationKey: 'updateScenario',
      updateVariableName: 'scenario',
      excludeFields: ['__typename', 'id', 'tsCreated', 'tsUpdated', 'entityType'],
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Set ports to selected ports (only IDs)
        transformed.ports = selectedPorts.value.map(port => ({id: port.id}));
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

    /**
     * Fetch scenario data and port list
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      
      if (response) {
        portListRaw.value = response.devicePortList || [];
        
        // Pre-select ports that are already connected
        if (scenario.value.ports && scenario.value.ports.length > 0) {
          selectedPorts.value = scenario.value.ports.map(port => {
            return portList.value.find(p => p.id === port.id);
          }).filter(Boolean);
        }
      }
    };

    /**
     * Save scenario
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) return;
      
      // Validate required fields
      if (!validateRequired(scenario.value, ['name', 'body'])) {
        return;
      }

      await updateEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      scenario,
      loading,
      saving,
      portList,
      selectedPorts,
      groovyCompletions,
      onSave
    };
  }
});

</script>

