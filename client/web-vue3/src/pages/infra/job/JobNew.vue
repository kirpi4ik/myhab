<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
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
import {defineComponent, onMounted, ref} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {JOB_LIST_ALL, JOB_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'JobNew',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const job = ref({
      name: '',
      description: '',
      state: 'ACTIVE'
    });
    const router = useRouter();
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
      client.query({
        query: JOB_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        scenarioList.value = response.data.scenarioList || [];
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to load data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching data:', error);
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

      // Prepare mutation data
      const jobData = {
        name: job.value.name,
        description: job.value.description,
        state: job.value.state,
        scenario: { id: selectedScenario.value.id },
        cronTriggers: cronTriggers.value.map(t => ({ expression: t.expression })),
        tags: selectedTags.value.map(tag => ({ id: tag.id }))
      };

      client.mutate({
        mutation: JOB_CREATE,
        variables: {job: jobData},
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Job created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/jobs/${response.data.jobCreate.id}/edit`});
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to create job',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating job:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      job,
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

