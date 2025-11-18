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

          <PortSelector
            v-model="selectedPorts"
            label="Connected Ports"
            hint="Select ports for this scenario"
          />
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="scenario"
          icon="mdi-script-text"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Connected Ports', value: selectedPorts?.length || 0 }
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
import {defineComponent, onMounted, ref} from 'vue';
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import PortSelector from '@/components/selectors/PortSelector.vue';
import {SCENARIO_EDIT_GET_BY_ID, SCENARIO_UPDATE} from '@/graphql/queries';

export default defineComponent({
  name: 'ScenarioEdit',
  components: {
    CodeEditor,
    EntityInfoPanel,
    EntityFormActions,
    PortSelector
  },
  setup() {
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
        transformed.ports = (selectedPorts.value || []).map(port => ({id: port.id}));
        return transformed;
      }
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
     * Fetch scenario data and pre-select ports
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      
      if (response && scenario.value.ports && scenario.value.ports.length > 0) {
        // Pre-select ports that are already connected
        // The PortSelector will load all ports, so we just need to set the IDs
        selectedPorts.value = scenario.value.ports.map(port => ({id: port.id}));
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
      selectedPorts,
      groovyCompletions,
      onSave
    };
  }
});

</script>

