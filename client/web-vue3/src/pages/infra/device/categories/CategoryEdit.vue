<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && category">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Device Category
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ category.name }}
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

        <!-- Information Panel -->
        <q-card-section class="bg-blue-grey-1">
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Information</div>
          <div class="row q-gutter-md">
            <div class="col">
              <q-icon name="mdi-identifier" class="q-mr-xs"/>
              <strong>ID:</strong> {{ category.id }}
            </div>
            <div class="col" v-if="category.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ category.uid }}
            </div>
          </div>
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
            Save
          </q-btn>
          <q-btn 
            color="grey" 
            @click="$router.go(-1)" 
            icon="mdi-cancel"
            :disable="saving"
          >
            Cancel
          </q-btn>
          <q-space/>
          <q-btn 
            color="info" 
            :to="`/admin/dcategories/${$route.params.idPrimary}/view`" 
            icon="mdi-eye" 
            outline
            :disable="saving"
          >
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
import {useRoute, useRouter} from 'vue-router';
import {useQuasar} from 'quasar';
import {useApolloClient} from '@vue/apollo-composable';
import {prepareForMutation} from '@/_helpers';
import _ from 'lodash';

import {
  DEVICE_CATEGORY_BY_ID,
  DEVICE_CATEGORY_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'DCategoryEdit',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const category = ref({
      name: ''
    });
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const saving = ref(false);

    /**
     * Fetch category data from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_CATEGORY_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        category.value = _.cloneDeep(response.data.deviceCategory);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load category data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching device category:', error);
      });
    };

    /**
     * Update device category
     */
    const onSave = () => {
      // Validate required fields
      if (!category.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Category name is required',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      saving.value = true;

      // Create a clean copy for mutation
      const cleanCategory = prepareForMutation(category.value, [
        '__typename',
        'entityType',
        'id',
        'uid',
        'tsCreated',
        'tsUpdated'
      ]);

      client.mutate({
        mutation: DEVICE_CATEGORY_UPDATE,
        variables: {
          id: route.params.idPrimary,
          deviceCategory: cleanCategory
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues
        }
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Device category updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        // Refresh data to show updated values
        fetchData();
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating device category:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      category,
      onSave,
      loading,
      saving
    };
  }
});

</script>
