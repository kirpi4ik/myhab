<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width" v-if="zone">
          <div class="text-h5">Edit zone: {{ zone.name }}</div>
          
          <q-input 
            v-model="zone.name" 
            label="Name" 
            clearable 
            clear-icon="close" 
            color="orange"
            :rules="[val => !!val || 'Field is required']"
          />
          
          <q-input 
            v-model="zone.description" 
            label="Description" 
            clearable 
            clear-icon="close" 
            color="orange"
            type="textarea"
            rows="3"
          />
          
          <q-separator class="q-my-md"/>
          
          <div class="text-h6">Parent Zone</div>
          <q-select 
            v-model="zone.parent"
            :options="zoneList"
            option-label="name"
            label="Parent Zone" 
            map-options 
            filled 
            dense 
            color="green"
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-map-marker-multiple" />
            </template>
          </q-select>
          
          <q-separator class="q-my-md"/>
          
          <div class="text-h6">Sub-zones</div>
          <q-select 
            v-model="zone.zones"
            :options="availableSubZones"
            option-label="name"
            label="Sub-zones" 
            map-options 
            filled 
            dense 
            color="green"
            multiple
            use-chips
            stack-label
          >
            <template v-slot:prepend>
              <q-icon name="mdi-map-marker-radius" />
            </template>
            <q-icon 
              name="cancel" 
              @click.stop.prevent="zone.zones = []" 
              class="cursor-pointer text-blue"
            />
          </q-select>
          
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
import { defineComponent, onMounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import { prepareForMutation } from '@/_helpers';
import _ from 'lodash';

import { 
  ZONE_GET_BY_ID_MINIMAL,
  ZONES_GET_ALL,
  ZONE_VALUE_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'ZoneEdit',
  setup() {
    const $q = useQuasar();
    const { client } = useApolloClient();
    const zone = ref({ 
      zones: [], 
      parent: null,
      name: '',
      description: ''
    });
    const zoneList = ref([]);
    const router = useRouter();
    const route = useRoute();
    
    // Compute available sub-zones (exclude current zone and its descendants)
    const availableSubZones = computed(() => {
      if (!zone.value.id) return zoneList.value;
      
      return zoneList.value.filter(z => {
        // Exclude the current zone itself
        if (z.id === zone.value.id) return false;
        
        // Exclude if this zone is already a parent of the candidate
        // (to prevent circular references)
        return true;
      });
    });
    
    const fetchData = () => {
      // Fetch the zone data
      client.query({
        query: ZONE_GET_BY_ID_MINIMAL,
        variables: { id: route.params.idPrimary },
        fetchPolicy: 'network-only',
      }).then(response => {
        zone.value = _.cloneDeep(response.data.zoneById);
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to load zone data'
        });
      });
      
      // Fetch all zones for parent/sub-zone selection
      client.query({
        query: ZONES_GET_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        zoneList.value = response.data.zoneList;
      });
    };
    
    const onSave = () => {
      if (!zone.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Name is required'
        });
        return;
      }
      
      // Create a clean copy for mutation
      const cleanZone = prepareForMutation(zone.value, [
        '__typename', 
        'devices', 
        'peripherals', 
        'cables',
        'categories'
      ]);
      
      // Remove id from top-level zone object
      delete cleanZone.id;
      
      // Clean parent - keep only id
      if (cleanZone.parent) {
        cleanZone.parent = {
          id: cleanZone.parent.id
        };
      }
      
      // Clean zones array - keep only id
      if (cleanZone.zones && Array.isArray(cleanZone.zones)) {
        cleanZone.zones = cleanZone.zones.map(z => ({
          id: z.id
        }));
      }
      
      client.mutate({
        mutation: ZONE_VALUE_UPDATE,
        variables: { 
          id: route.params.idPrimary, 
          zone: cleanZone 
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Zone updated successfully',
          icon: 'check_circle'
        });
        router.push({ path: `/admin/zones/${response.data.zoneUpdate.id}/view` });
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
      zone,
      zoneList,
      availableSubZones,
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
