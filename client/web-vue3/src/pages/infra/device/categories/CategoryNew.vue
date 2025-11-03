<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="category">
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
        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Category"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent} from 'vue';
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {DEVICE_CATEGORY_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'DCategoryNew',
  components: {
    EntityFormActions
  },
  setup() {
    // Use CRUD composable for create
    const {
      entity: category,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Device Category',
      entityPath: '/admin/dcategories',
      createMutation: DEVICE_CATEGORY_CREATE,
      createMutationKey: 'deviceCategoryCreate',
      createVariableName: 'deviceCategory',
      initialData: {
        name: ''
      }
    });

    /**
     * Create new device category
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) return;
      
      // Validate required fields
      if (!validateRequired(category.value, ['name'])) {
        return;
      }

      await createEntity();
    };

    return {
      category,
      onSave,
      saving
    };
  }
});

</script>
