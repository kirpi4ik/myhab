<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Device Category
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="category.name" 
            label="Name" 
            hint="Unique category name (e.g., SWITCH, ROUTER)"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Name is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Actions -->
        <q-card-actions>
          <q-btn 
            color="primary" 
            type="submit" 
            icon="mdi-content-save"
            :loading="saving"
          >
            Create Category
          </q-btn>
          <q-btn 
            color="grey" 
            @click="$router.go(-1)" 
            icon="mdi-cancel"
            :disable="saving"
          >
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, ref} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {DEVICE_CATEGORY_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'DCategoryNew',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const category = ref({
      name: ''
    });
    const router = useRouter();
    const saving = ref(false);

    /**
     * Create new device category
     */
    const onSave = () => {
      // Validate required fields
      if (!category.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Please fill in the category name',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      saving.value = true;

      client.mutate({
        mutation: DEVICE_CATEGORY_CREATE,
        variables: {deviceCategory: category.value},
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Device category created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/dcategories/${response.data.deviceCategoryCreate.id}/edit`});
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to create category',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating device category:', error);
      });
    };

    return {
      category,
      onSave,
      saving
    };
  }
});

</script>
