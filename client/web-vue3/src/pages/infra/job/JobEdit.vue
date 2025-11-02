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
                <q-input v-model="trigger.expression" label="Cron Expression" dense hint="e.g., 0 0 * * * (every hour)">
                  <template v-slot:prepend>
                    <q-icon name="mdi-clock-outline"/>
                  </template>
                </q-input>
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
                    :options="tagList"
                    option-label="name"
                    option-value="id"
                    label="Tags"
                    hint="Select tags for organization"
                    multiple
                    clearable
                    use-chips
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-tag-multiple"/>
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
              <strong>ID:</strong> {{ job.id }}
            </div>
            <div class="col" v-if="job.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ job.uid }}
            </div>
            <div class="col" v-if="cronTriggers.length > 0">
              <q-icon name="mdi-clock-outline" class="q-mr-xs"/>
              <strong>Cron Triggers:</strong> {{ cronTriggers.length }}
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
          <q-btn color="info" :to="`/admin/jobs/${$route.params.idPrimary}/view`" icon="mdi-eye" outline>
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
import {defineComponent, onMounted, ref} from 'vue';
import _ from 'lodash';

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {JOB_EDIT_GET_BY_ID, JOB_UPDATE} from '@/graphql/queries';
import {prepareForMutation} from "@/_helpers/apollo-utils";

export default defineComponent({
  name: 'JobEdit',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const job = ref(null);
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const scenarioList = ref([]);
    const selectedScenario = ref(null);
    const tagList = ref([]);
    const selectedTags = ref([]);
    const cronTriggers = ref([]);
    const jobStates = ['ACTIVE', 'INACTIVE', 'PAUSED', 'DISABLED'];

    const addCronTrigger = () => {
      cronTriggers.value.push({ expression: '0 0 * * *' });
    };

    const removeCronTrigger = (index) => {
      cronTriggers.value.splice(index, 1);
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: JOB_EDIT_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        job.value = _.cloneDeep(response.data.job);
        scenarioList.value = response.data.scenarioList || [];
        tagList.value = response.data.jobTagList || [];
        
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
        }
        
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load job data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching job:', error);
      });
    };

    const onSave = () => {
      // Validate required fields
      if (!job.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Please enter a job name',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
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

      // Prepare mutation data - remove read-only fields
      const cleanJob = prepareForMutation(job.value, [
        '__typename',
        'id',
        'uid',
        'tsCreated',
        'tsUpdated',
        'entityType',
        'cronTriggers',
        'eventTriggers',
        'tags',
        'scenario'
      ]);

      // Set scenario
      cleanJob.scenario = { id: selectedScenario.value.id };
      
      // Set cron triggers
      cleanJob.cronTriggers = cronTriggers.value.map(t => ({
        id: t.id,
        expression: t.expression
      }));
      
      // Set tags
      cleanJob.tags = selectedTags.value.map(tag => ({ id: tag.id }));

      client.mutate({
        mutation: JOB_UPDATE,
        variables: {
          id: job.value.id,
          job: cleanJob
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(() => {
        $q.notify({
          color: 'positive',
          message: 'Job updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/jobs/${job.value.id}/view`});
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to update job',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating job:', error);
      });
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
      selectedTags,
      cronTriggers,
      jobStates,
      addCronTrigger,
      removeCronTrigger,
      onSave
    };
  }
});

</script>

