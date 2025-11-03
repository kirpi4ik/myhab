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
        <EntityInfoPanel
          :entity="category"
          icon="mdi-shape"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/dcategories/${$route.params.idPrimary}/view`"
          save-label="Save Category"
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
import { defineComponent, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useEntityCRUD } from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';

import {
  DEVICE_CATEGORY_BY_ID,
  DEVICE_CATEGORY_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'DCategoryEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions
  },
  setup() {
    const route = useRoute();

    const {
      entity: category,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Device Category',
      entityPath: '/admin/dcategories',
      getQuery: DEVICE_CATEGORY_BY_ID,
      getQueryKey: 'deviceCategory',
      updateMutation: DEVICE_CATEGORY_UPDATE,
      updateMutationKey: 'deviceCategoryUpdate',
      excludeFields: ['__typename', 'entityType', 'id', 'uid', 'tsCreated', 'tsUpdated']
    });

    /**
     * Save category
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) {
        return;
      }
      
      if (!validateRequired(category.value, ['name'])) {
        return;
      }
      
      await updateEntity();
    };

    onMounted(() => {
      fetchEntity();
    });

    return {
      category,
      loading,
      saving,
      onSave
    };
  }
});
</script>
