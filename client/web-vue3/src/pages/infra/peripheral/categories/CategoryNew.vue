<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="category">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Peripheral Category
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="category.name" 
            label="Name" 
            hint="Unique category name"
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

          <q-input 
            v-model="category.title" 
            label="Title" 
            hint="Display title"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Title is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-format-title"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

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
import {PERIPHERAL_CATEGORY_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'PCategoryNew',
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
      entityName: 'Peripheral Category',
      entityPath: '/admin/pcategories',
      createMutation: PERIPHERAL_CATEGORY_CREATE,
      createMutationKey: 'peripheralCategoryCreate',
      createVariableName: 'peripheralCategory',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        title: ''
      }
    });

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(category.value, ['name', 'title'])) return;
      await createEntity();
    };

    return {
      category,
      saving,
      onSave
    };
  }
});

</script>
