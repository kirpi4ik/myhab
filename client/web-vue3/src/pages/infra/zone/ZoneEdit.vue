<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && zone">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Zone
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ zone.name }}
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="zone.name" 
            label="Name" 
            hint="Zone name or identifier"
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
            hint="Zone description or purpose"
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
        </q-card-section>

        <q-separator/>

        <!-- Zone Hierarchy -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-file-tree" class="q-mr-xs"/>
            Zone Hierarchy
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-select 
            v-model="zone.parent"
            :options="zoneList"
            option-label="name"
            label="Parent Zone" 
            hint="Select parent zone (optional)"
            map-options 
            filled 
            dense 
            color="orange"
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-map-marker-multiple"/>
            </template>
          </q-select>
          
          <q-select 
            v-model="zone.zones"
            :options="availableSubZones"
            option-label="name"
            label="Sub-zones" 
            hint="Select child zones (optional, excludes current zone and parent)"
            map-options 
            filled 
            dense 
            color="orange"
            multiple
            use-chips
            use-input
            stack-label
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-map-marker-radius"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="zone"
          icon="mdi-map-marker"
          :extra-info="[
            { icon: 'mdi-map-marker-radius', label: 'Sub-zones', value: zone.zones?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/zones/${$route.params.idPrimary}/view`"
          save-label="Save Zone"
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
import { defineComponent, onMounted, ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import { useEntityCRUD } from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';

import { 
  ZONE_GET_BY_ID_MINIMAL,
  ZONES_GET_ALL,
  ZONE_VALUE_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'ZoneEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions
  },
  setup() {
    const route = useRoute();
    const $q = useQuasar();
    const { client } = useApolloClient();
    
    // Additional data
    const zoneList = ref([]);
    
    // Use CRUD composable
    const {
      entity: zone,
      loading,
      saving,
      fetchEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Zone',
      entityPath: '/admin/zones',
      getQuery: ZONE_GET_BY_ID_MINIMAL,
      updateMutation: ZONE_VALUE_UPDATE,
      getQueryKey: 'zoneById',
      excludeFields: ['__typename', 'tsCreated', 'tsUpdated']
    });
    
    // Compute available sub-zones (exclude current zone, parent zone, and already selected zones)
    const availableSubZones = computed(() => {
      if (!zone.value.id) return zoneList.value;
      
      // Get IDs of currently selected sub-zones
      const selectedZoneIds = (zone.value.zones || []).map(z => z.id);
      
      return zoneList.value.filter(z => {
        // Exclude the current zone itself (prevent self-reference)
        if (z.id === zone.value.id) return false;
        
        // Exclude the parent zone (prevent direct cyclic reference)
        if (zone.value.parent && z.id === zone.value.parent.id) return false;
        
        // Exclude already selected sub-zones
        if (selectedZoneIds.includes(z.id)) return false;
        
        return true;
      });
    });
    
    /**
     * Fetch zone data and all zones list
     */
    const fetchData = async () => {
      // Fetch zone data
      await fetchEntity();
      
      // Fetch all zones for parent/sub-zone selection
      client.query({
        query: ZONES_GET_ALL,
        fetchPolicy: 'network-only',
      }).then(response => {
        zoneList.value = response.data.zoneList;
      }).catch(error => {
        console.error('Error fetching zones list:', error);
      });
    };
    
    /**
     * Save zone
     */
    const onSave = async () => {
      // Prevent double execution
      if (saving.value) {
        return;
      }
      
      // Validate required fields
      if (!validateRequired(zone.value, ['name'])) {
        return;
      }
      
      // We need to bypass the default updateEntity because prepareForMutation
      // removes 'id' from nested objects, which breaks our parent and zones
      // So we'll call the mutation directly
      saving.value = true;
      
      try {
        // Apply our custom transformation
        const transformed = {
          name: zone.value.name,
          description: zone.value.description,
          categories: zone.value.categories || [],
          parent: null,
          zones: []
        };
        
        // Clean parent - keep only id
        if (zone.value.parent && zone.value.parent.id) {
          transformed.parent = { id: zone.value.parent.id };
        }
        
        // Clean zones array - keep only id
        if (zone.value.zones && Array.isArray(zone.value.zones)) {
          transformed.zones = zone.value.zones
            .filter(z => z && z.id)
            .map(z => ({ id: z.id }));
        }
        
        const response = await client.mutate({
          mutation: ZONE_VALUE_UPDATE,
          variables: { 
            id: route.params.idPrimary, 
            zone: transformed
          },
          fetchPolicy: 'no-cache',
          update: () => {
            // Skip cache update
          }
        });
        
        saving.value = false;
        
        $q.notify({
          color: 'positive',
          message: 'Zone updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        
        // Refresh data
        await fetchData();
        
      } catch (error) {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating zone:', error);
      }
    };
    
    onMounted(() => {
      fetchData();
    });
    
    return {
      zone,
      zoneList,
      availableSubZones,
      loading,
      saving,
      onSave
    };
  }
});
</script>
