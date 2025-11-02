<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width" v-if="category">
          <div class="text-h5">Edit Peripheral Category: {{ category.name }}</div>
          
          <q-input 
            v-model="category.name" 
            label="Name" 
            clearable 
            clear-icon="close" 
            color="orange"
            :rules="[val => !!val || 'Field is required']"
            hint="Internal name for the category (e.g., 'light', 'sensor')"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-tag" />
            </template>
          </q-input>
          
          <q-input 
            v-model="category.title" 
            label="Title" 
            clearable 
            clear-icon="close" 
            color="orange"
            hint="Display title for the category (e.g., 'Lighting', 'Sensors')"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-format-title" />
            </template>
          </q-input>
          
          <q-separator class="q-my-md"/>
          
          <div class="text-subtitle2 text-grey-7">
            <q-icon name="info" color="blue" size="sm" class="q-mr-xs"/>
            Category Information
          </div>
          
          <q-list bordered separator class="rounded-borders q-mt-sm">
            <q-item>
              <q-item-section avatar>
                <q-icon name="mdi-identifier" color="primary" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Category ID</q-item-label>
                <q-item-label caption>{{ category.id }}</q-item-label>
              </q-item-section>
            </q-item>
            
            <q-item v-if="category.peripherals && category.peripherals.length > 0">
              <q-item-section avatar>
                <q-icon name="mdi-devices" color="green" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Peripherals</q-item-label>
                <q-item-label caption>
                  {{ category.peripherals.length }} peripheral(s) using this category
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
          
        </q-card-section>
        
        <q-separator/>
        
        <q-card-actions>
          <q-btn color="accent" type="submit" icon="save">
            Save
          </q-btn>
          <q-btn color="info" @click="$router.go(-1)" icon="mdi-arrow-left">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import { defineComponent, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import { prepareForMutation } from '@/_helpers';
import _ from 'lodash';

import { 
  PERIPHERAL_CATEGORY_GET_DETAILS,
  PERIPHERAL_CATEGORY_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'PCategoryEdit',
  setup() {
    const $q = useQuasar();
    const { client } = useApolloClient();
    const category = ref({ 
      name: '',
      title: '',
      peripherals: [],
      cables: []
    });
    const router = useRouter();
    const route = useRoute();
    
    const fetchData = () => {
      client.query({
        query: PERIPHERAL_CATEGORY_GET_DETAILS,
        variables: { id: route.params.idPrimary },
        fetchPolicy: 'network-only',
      }).then(response => {
        category.value = _.cloneDeep(response.data.peripheralCategory);
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to load category data',
          icon: 'error'
        });
      });
    };
    
    const onSave = () => {
      if (!category.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Name is required',
          icon: 'warning'
        });
        return;
      }
      
      // Create a clean copy for mutation
      const cleanCategory = prepareForMutation(category.value, [
        '__typename', 
        'peripherals', 
        'cables',
        'entityType',
        'uid'
      ]);
      
      // Remove id from top-level category object
      delete cleanCategory.id;
      
      client.mutate({
        mutation: PERIPHERAL_CATEGORY_UPDATE,
        variables: { 
          id: route.params.idPrimary, 
          peripheralCategory: cleanCategory 
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Category updated successfully',
          icon: 'check_circle'
        });
        // Navigate back to categories list or stay on edit page
        router.push({ path: '/admin/peripherals/categories' });
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'error'
        });
      });
    };
    
    onMounted(() => {
      fetchData();
    });
    
    return {
      category,
      onSave
    };
  }
});
</script>

<style scoped>
.q-my-md {
  margin-top: 16px;
  margin-bottom: 16px;
}
</style>
