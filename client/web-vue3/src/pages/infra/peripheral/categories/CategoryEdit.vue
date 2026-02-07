<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && category">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-shape" color="primary" size="sm" class="q-mr-sm"/>
            Edit Peripheral Category
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
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Name is required']"
            hint="Internal name for the category (e.g., 'light', 'sensor')"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-tag"/>
            </template>
          </q-input>
          
          <q-input 
            v-model="category.title" 
            label="Title" 
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            hint="Display title for the category (e.g., 'Lighting', 'Sensors')"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-format-title"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="category"
          icon="mdi-shape"
          :extra-info="[
            { icon: 'mdi-devices', label: 'Peripherals', value: category.peripherals?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
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
  PERIPHERAL_CATEGORY_GET_DETAILS,
  PERIPHERAL_CATEGORY_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'PCategoryEdit',
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
      entityName: 'Peripheral Category',
      entityPath: '/admin/peripherals/categories',
      getQuery: PERIPHERAL_CATEGORY_GET_DETAILS,
      getQueryKey: 'peripheralCategory',
      updateMutation: PERIPHERAL_CATEGORY_UPDATE,
      updateMutationKey: 'peripheralCategoryUpdate',
      excludeFields: ['__typename', 'id', 'peripherals', 'cables', 'entityType', 'tsCreated', 'tsUpdated']
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
