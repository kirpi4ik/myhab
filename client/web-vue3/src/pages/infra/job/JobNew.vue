<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="job">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Job
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
                  <q-input 
                    v-model="trigger.description"
                    label="Trigger Description"
                    hint="Optional description explaining why/when this trigger runs"
                    clearable
                    clear-icon="close"
                    color="blue"
                    filled
                    dense
                    class="q-mb-md"
                  >
                    <template v-slot:prepend>
                      <q-icon name="mdi-text-short"/>
                    </template>
                  </q-input>
                  <div class="text-caption text-grey-7 q-mb-xs">Cron Expression (UTC timezone)</div>
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

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Job"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from 'quasar';
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {JOB_LIST_ALL, JOB_CREATE} from '@/graphql/queries';
import '@vue-js-cron/light/dist/light.css';
import { CronLight } from '@vue-js-cron/light';
import { Cron } from 'croner';

export default defineComponent({
  name: 'JobNew',
  components: {
    EntityFormActions,
    CronLight
  },
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const scenarioList = ref([]);
    const selectedScenario = ref(null);
    const tagList = ref([]);
    const tagListFiltered = ref([]);
    const selectedTags = ref([]);
    const cronTriggers = ref([]);
    const cronErrors = ref({});
    const jobStates = ['DRAFT', 'ACTIVE', 'DISABLED'];

    const {
      entity: job,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Job',
      entityPath: '/admin/jobs',
      createMutation: JOB_CREATE,
      createMutationKey: 'jobCreate',
      createVariableName: 'job',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        description: '',
        state: 'DRAFT'
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Add scenario
        if (selectedScenario.value && selectedScenario.value.id) {
          transformed.scenario = { id: selectedScenario.value.id };
        }
        // Add cron triggers
        transformed.cronTriggers = cronTriggers.value.map(t => ({ 
          expression: t.expression,
          description: t.description || null
        }));
        // Add tags (existing and new)
        transformed.tags = selectedTags.value.map(tag => {
          if (tag.id) {
            return { id: tag.id };
          } else {
            return { name: tag.name };
          }
        });
        return transformed;
      }
    });

    const addCronTrigger = () => {
      cronTriggers.value.push({ expression: '0 0 * * *', description: '' });
    };

    const removeCronTrigger = (index) => {
      cronTriggers.value.splice(index, 1);
    };

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

    const createNewTag = (val, done) => {
      if (val.length > 0) {
        const existingTag = tagList.value.find(
          tag => tag.name.toLowerCase() === val.toLowerCase()
        );
        
        if (existingTag) {
          done(existingTag, 'add-unique');
        } else {
          const newTag = {
            name: val,
            id: null
          };
          
          tagList.value = [...tagList.value, newTag];
          tagListFiltered.value = [...tagList.value];
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

    const fetchData = () => {
      client.query({
        query: JOB_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        scenarioList.value = response.data.scenarioList || [];
        tagList.value = [...(response.data.jobTagList || [])];
        tagListFiltered.value = [...(response.data.jobTagList || [])];
      }).catch(error => {
        console.error('Error fetching data:', error);
      });
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

    const onSave = async () => {
      if (saving.value) return;
      
      // Custom validation for scenario
      if (!selectedScenario.value) {
        $q.notify({
          color: 'negative',
          message: 'Please select a scenario',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      if (!validateRequired(job.value, ['name'])) return;
      await createEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      job,
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
      saving,
      onSave
    };
  }
});

</script>

