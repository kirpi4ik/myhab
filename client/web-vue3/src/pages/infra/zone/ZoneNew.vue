<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" v-if="zone">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Zone
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="zone.name" 
            label="Name" 
            hint="Zone name"
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
            v-model="zone.description" 
            label="Description" 
            hint="Zone description"
            clearable 
            clear-icon="close"
            color="orange"
            filled
            dense
            type="textarea"
            rows="3"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>

          <q-select
            v-model="zone.parent"
            :options="parentZoneList"
            option-label="name"
            label="Parent Zone"
            hint="Optional parent zone"
            clearable
            filled
            dense
            color="orange"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-home-group"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Zone"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {ZONE_CREATE, ZONES_GET_ALL} from '@/graphql/queries';

export default defineComponent({
  name: 'ZoneNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const parentZoneList = ref([]);

    const {
      entity: zone,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Zone',
      entityPath: '/admin/zones',
      createMutation: ZONE_CREATE,
      createMutationKey: 'zoneCreate',
      createVariableName: 'zone',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        description: '',
        parent: null
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Clean parent - only send ID if selected
        if (transformed.parent) {
          if (transformed.parent.id) {
            transformed.parent = { id: transformed.parent.id };
          } else {
            delete transformed.parent;
          }
        }
        return transformed;
      }
    });

    const fetchParentZones = async () => {
      try {
        const response = await client.query({
          query: ZONES_GET_ALL,
          fetchPolicy: 'network-only',
        });
        parentZoneList.value = response.data.zoneList || [];
      } catch (error) {
        console.error('Error fetching zones:', error);
      }
    };

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(zone.value, ['name'])) return;
      await createEntity();
    };

    onMounted(() => {
      fetchParentZones();
    });

    return {
      zone,
      saving,
      onSave,
      parentZoneList
    };
  }
});
</script>
