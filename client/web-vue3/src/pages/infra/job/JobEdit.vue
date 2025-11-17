<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && job">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Job
          </div>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Job Configuration</div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-input v-model="job.name"
                   label="Name *"
                   hint="Name for the job"
                   clearable
                   clear-icon="close"
                   color="orange"
                   filled
                   dense
                   :rules="[val => !!val || 'Field is required']">
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>

          <q-input v-model="job.description"
                   label="Description"
                   hint="Job description"
                   type="textarea"
                   rows="3"
                   clearable
                   clear-icon="close"
                   color="green"
                   filled
                   dense>
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>

          <q-select v-model="selectedScenario"
                    :options="scenarioList"
                    option-label="name"
                    option-value="id"
                    label="Scenario *"
                    hint="Select scenario to execute"
                    clearable
                    filled
                    dense
                    :rules="[val => !!val || 'Field is required']">
            <template v-slot:prepend>
              <q-icon name="mdi-script-text"/>
            </template>
          </q-select>

          <q-select v-model="job.state"
                    :options="jobStates"
                    label="State"
                    hint="Job state"
                    clearable
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-state-machine"/>
            </template>
          </q-select>

          <div class="text-subtitle2 text-weight-medium q-mt-md q-mb-sm">Cron Triggers</div>
          <q-list bordered separator>
            <q-item v-for="(trigger, index) in cronTriggers" :key="index">
              <q-item-section>
                <div class="q-mb-sm">
                  <div class="text-caption text-grey-7 q-mb-xs">Cron Expression(UTC timezone)</div>
                  <cron-light 
                    v-model="trigger.expression" 
                    locale="en"
                    @error="(error) => cronErrors[index] = error"
                  />
                  <div v-if="cronErrors[index]" class="text-negative text-caption q-mt-xs">
                    {{ cronErrors[index] }}
                  </div>
                  <div v-else-if="trigger.expression" class="text-grey-6 text-caption q-mt-xs">
                    Expression: {{ trigger.expression }}
                    <br>
                    Next run: <span class="text-primary text-weight-medium">{{ getNextRunTime(trigger.expression) }}</span> (local time)
                  </div>
                </div>
              </q-item-section>
              <q-item-section side>
                <q-btn icon="mdi-delete" color="red" flat round @click="removeCronTrigger(index)">
                  <q-tooltip>Remove</q-tooltip>
                </q-btn>
              </q-item-section>
            </q-item>
          </q-list>
          <q-btn icon="mdi-plus" label="Add Cron Trigger" color="primary" flat @click="addCronTrigger" class="q-mt-sm"/>

          <div class="text-subtitle2 text-weight-medium q-mt-md q-mb-sm">Tags</div>
          <q-select v-model="selectedTags"
                    :options="tagListFiltered"
                    option-label="name"
                    option-value="id"
                    label="Tags"
                    hint="Select existing tags or type to create new ones"
                    multiple
                    clearable
                    use-chips
                    use-input
                    @filter="filterTagFn"
                    @new-value="createNewTag"
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-tag-multiple"/>
            </template>
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  <q-item-label>No matching tags</q-item-label>
                  <q-item-label caption>Press Enter to create a new tag</q-item-label>
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="job"
          icon="mdi-briefcase"
          :extra-info="[
            { icon: 'mdi-clock-outline', label: 'Cron Triggers', value: cronTriggers.length }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/jobs/${$route.params.idPrimary}/view`"
          save-label="Save Job"
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
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";
import {useQuasar} from 'quasar';
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {JOB_EDIT_GET_BY_ID, JOB_UPDATE} from '@/graphql/queries';
import _ from 'lodash';
import '@vue-js-cron/light/dist/light.css';
import { CronLight } from '@vue-js-cron/light';
import { Cron } from 'croner';

export default defineComponent({
  name: 'JobEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    CronLight
  },
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const route = useRoute();
    
    const scenarioList = ref([]);
    const selectedScenario = ref(null);
    const tagList = ref([]);
    const tagListFiltered = ref([]);
    const selectedTags = ref([]);
    const cronTriggers = ref([]);
    const cronErrors = ref({});
    const jobStates = ['DRAFT', 'ACTIVE', 'DISABLED'];

    // Use CRUD composable
    const {
      entity: job,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Job',
      entityPath: '/admin/jobs',
      getQuery: JOB_EDIT_GET_BY_ID,
      getQueryKey: 'job',
      updateMutation: JOB_UPDATE,
      updateMutationKey: 'jobUpdate',
      updateVariableName: 'job',
      excludeFields: ['__typename', 'id', 'uid', 'tsCreated', 'tsUpdated', 'entityType', 'cronTriggers', 'eventTriggers', 'tags', 'scenario'],
      transformBeforeSave: (data) => {
        const transformed = {...data};
        
        // Set scenario
        transformed.scenario = { id: selectedScenario.value.id };
        
        // Set cron triggers
        transformed.cronTriggers = cronTriggers.value.map(t => ({
          id: t.id,
          expression: t.expression
        }));
        
        // Set tags: handle both existing tags (with IDs) and new tags (without IDs)
        transformed.tags = selectedTags.value.map(tag => {
          if (tag.id) {
            return { id: tag.id };
          } else {
            // New tag - send only the name, server will create it
            return { name: tag.name };
          }
        });
        
        return transformed;
      }
    });

    const addCronTrigger = () => {
      cronTriggers.value.push({ expression: '* * * * *' });
      // Clear error for new trigger
      cronErrors.value[cronTriggers.value.length - 1] = null;
    };

    const removeCronTrigger = (index) => {
      cronTriggers.value.splice(index, 1);
      // Remove error for deleted trigger
      const newErrors = {};
      Object.keys(cronErrors.value).forEach(key => {
        const keyNum = parseInt(key);
        if (keyNum < index) {
          newErrors[key] = cronErrors.value[key];
        } else if (keyNum > index) {
          newErrors[keyNum - 1] = cronErrors.value[key];
        }
      });
      cronErrors.value = newErrors;
    };

    /**
     * Filter tags based on user input
     */
    const filterTagFn = (val, update) => {
      update(() => {
        if (val === '') {
          tagListFiltered.value = tagList.value;
        } else {
          const needle = val.toLowerCase();
          tagListFiltered.value = tagList.value.filter(
            tag => tag.name.toLowerCase().includes(needle)
          );
        }
      });
    };

    /**
     * Create a new tag on the fly
     */
    const createNewTag = (val, done) => {
      if (val.length > 0) {
        // Check if tag already exists
        const existingTag = tagList.value.find(
          tag => tag.name.toLowerCase() === val.toLowerCase()
        );
        
        if (existingTag) {
          // Tag exists, just add it
          done(existingTag, 'add-unique');
        } else {
          // Create new tag object (will be created on server when job is saved)
          const newTag = {
            name: val,
            id: null // Will be created on server
          };
          
          // Add to local list - create new array to avoid readonly issues
          tagList.value = [...tagList.value, newTag];
          tagListFiltered.value = [...tagList.value];
          
          // Add to selection
          done(newTag, 'add-unique');
          
          $q.notify({
            color: 'info',
            message: `New tag "${val}" will be created`,
            icon: 'mdi-tag-plus',
            position: 'top',
            timeout: 2000
          });
        }
      }
    };

    /**
     * Fetch job data, scenarios, and tags
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      
      if (response) {
        scenarioList.value = response.scenarioList || [];
        // Create mutable copies of tag lists to allow adding new tags
        tagList.value = [...(response.jobTagList || [])];
        tagListFiltered.value = [...(response.jobTagList || [])];
        
        // Pre-select scenario
        if (job.value.scenario) {
          selectedScenario.value = scenarioList.value.find(s => s.id === job.value.scenario.id);
        }
        
        // Pre-select tags
        if (job.value.tags && job.value.tags.length > 0) {
          selectedTags.value = job.value.tags.map(tag => {
            return tagList.value.find(t => t.id === tag.id);
          }).filter(Boolean);
        }
        
        // Load cron triggers
        if (job.value.cronTriggers && job.value.cronTriggers.length > 0) {
          cronTriggers.value = _.cloneDeep(job.value.cronTriggers);
          // Initialize errors object
          cronErrors.value = {};
          cronTriggers.value.forEach((_, index) => {
            cronErrors.value[index] = null;
          });
        }
      }
    };

    /**
     * Get next run time from cron expression
     */
    const getNextRunTime = (cronExpression) => {
      try {
        if (!cronExpression) return '-';
        
        // Quartz format has 6-7 fields: second minute hour day month weekday (year)
        // Standard cron has 5 fields: minute hour day month weekday
        // Check field count and convert if needed
        const fields = cronExpression.trim().split(/\s+/);
        
        let cronForCroner = cronExpression;
        
        // If it's a 5-field expression, add seconds field at the beginning for Quartz format
        if (fields.length === 5) {
          cronForCroner = '0 ' + cronExpression;
        }
        // If it's 7 fields (with year), remove the year field as croner doesn't support it
        else if (fields.length === 7) {
          cronForCroner = fields.slice(0, 6).join(' ');
        }
        
        // Parse cron expression in UTC timezone (cron expressions are in UTC)
        // The cron job will be scheduled based on UTC time
        const job = new Cron(cronForCroner, { timezone: 'UTC' });
        const nextRun = job.nextRun();
        
        if (!nextRun) return 'No upcoming runs';
        
        // Convert UTC time to browser local time for display
        // toLocaleString automatically converts from the Date object's UTC representation
        // to the user's browser local timezone
        return nextRun.toLocaleString('en-GB', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false
        });
      } catch (error) {
        console.error('Error parsing cron expression:', cronExpression, error);
        return 'Invalid cron expression';
      }
    };

    /**
     * Save job
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) return;
      
      // Validate required fields
      if (!validateRequired(job.value, ['name'])) {
        return;
      }

      if (!selectedScenario.value) {
        $q.notify({
          color: 'negative',
          message: 'Please select a scenario',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      await updateEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      job,
      loading,
      scenarioList,
      selectedScenario,
      tagList,
      tagListFiltered,
      selectedTags,
      cronTriggers,
      cronErrors,
      jobStates,
      addCronTrigger,
      removeCronTrigger,
      filterTagFn,
      createNewTag,
      getNextRunTime,
      onSave
    };
  }
});

</script>

