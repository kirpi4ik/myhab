<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header -->
      <q-card-section>
        <div class="row items-center q-mb-md">
          <div class="col">
            <div class="text-h5">
              <q-icon name="mdi-cog" color="primary" size="sm" class="q-mr-sm"/>
              Configurations
            </div>
            <div class="text-subtitle2 text-grey-7">
              {{ entityType }} #{{ entityId }}
            </div>
          </div>
          <div class="col-auto">
            <q-btn 
              icon="mdi-plus-circle" 
              color="positive" 
              :disable="loading" 
              label="Add Configuration" 
              @click="openAddDialog"
              unelevated
            />
          </div>
        </div>
      </q-card-section>

      <q-separator/>

      <!-- Configuration List -->
      <q-card-section v-if="cfgList.length > 0">
        <div class="q-gutter-md">
          <q-card 
            v-for="cfg in cfgList" 
            :key="cfg.id" 
            bordered 
            flat
            class="config-card"
          >
            <q-card-section>
              <div class="row q-col-gutter-md">
                <!-- Key -->
                <div class="col-12 col-md-3">
                  <q-input 
                    v-model="cfg.key" 
                    label="Key *" 
                    dense
                    filled
                    readonly
                    hint="Configuration key (read-only)"
                  >
                    <template v-slot:prepend>
                      <q-icon name="mdi-key"/>
                    </template>
                  </q-input>
                </div>

                <!-- Value -->
                <div class="col-12 col-md-4">
                  <q-input 
                    v-model="cfg.value" 
                    label="Value *" 
                    dense
                    filled
                    clearable
                    hint="Configuration value"
                  >
                    <template v-slot:prepend>
                      <q-icon name="mdi-text-box"/>
                    </template>
                  </q-input>
                </div>

                <!-- Description -->
                <div class="col-12 col-md-4">
                  <q-input 
                    v-model="cfg.description" 
                    label="Description" 
                    dense
                    filled
                    clearable
                    hint="Optional description"
                  >
                    <template v-slot:prepend>
                      <q-icon name="mdi-text"/>
                    </template>
                  </q-input>
                </div>

                <!-- Actions -->
                <div class="col-12 col-md-1 flex items-center justify-end">
                  <q-btn-group flat>
                    <q-btn 
                      color="primary" 
                      icon="mdi-content-save" 
                      @click="onUpdate(cfg)"
                      flat
                      round
                    >
                      <q-tooltip>Save Changes</q-tooltip>
                    </q-btn>
                    <q-btn 
                      color="negative" 
                      icon="mdi-delete" 
                      @click="removeItem(cfg)"
                      flat
                      round
                    >
                      <q-tooltip>Delete</q-tooltip>
                    </q-btn>
                  </q-btn-group>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </q-card-section>

      <!-- Empty State -->
      <q-card-section v-else class="text-center q-pa-xl">
        <q-icon name="mdi-cog-outline" size="64px" color="grey-5"/>
        <div class="text-h6 text-grey-7 q-mt-md">No Configurations</div>
        <div class="text-body2 text-grey-6 q-mb-md">
          Add your first configuration to get started
        </div>
        <q-btn 
          icon="mdi-plus-circle" 
          color="primary" 
          label="Add Configuration" 
          @click="openAddDialog"
          unelevated
        />
      </q-card-section>

      <q-separator/>

      <!-- Footer Actions -->
      <q-card-actions>
        <q-btn 
          color="grey" 
          icon="mdi-arrow-left" 
          label="Back"
          @click="$router.go(-1)"
          flat
        />
        <q-space/>
        <div class="text-caption text-grey-7">
          {{ cfgList.length }} configuration{{ cfgList.length !== 1 ? 's' : '' }}
        </div>
      </q-card-actions>
    </q-card>

    <!-- Add Configuration Dialog -->
    <q-dialog 
      v-model="addConfig" 
      transition-show="slide-up" 
      transition-hide="slide-down"
      persistent
    >
      <q-card style="min-width: 600px">
        <q-card-section class="bg-primary text-white">
          <div class="text-h6">
            <q-icon name="mdi-plus-circle" class="q-mr-sm"/>
            Add New Configuration
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <!-- Key Select -->
          <q-select
            v-model="newCfgKey"
            :options="keyOptionsFiltered"
            label="Configuration Key *"
            use-input
            hide-selected
            fill-input
            input-debounce="300"
            @filter="filterFn"
            hint="Select or type a configuration key"
            filled
            dense
            :rules="[val => !!val || 'Key is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-key"/>
            </template>
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  <q-item-label>No results</q-item-label>
                  <q-item-label caption>Type to create a new key</q-item-label>
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <!-- Value Input -->
          <q-input 
            v-model="newCfgValue" 
            label="Value *" 
            filled
            dense
            clearable
            hint="Configuration value"
            :rules="[val => !!val || 'Value is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text-box"/>
            </template>
          </q-input>

          <!-- Description Input -->
          <q-input 
            v-model="newCfgDescription" 
            label="Description" 
            filled
            dense
            clearable
            type="textarea"
            rows="2"
            hint="Optional description"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <q-card-actions align="right">
          <q-btn 
            flat 
            label="Cancel" 
            color="grey" 
            v-close-popup
            icon="mdi-close"
          />
          <q-btn 
            unelevated
            label="Add Configuration" 
            color="positive" 
            @click="onAdd"
            icon="mdi-plus"
            :disable="!newCfgKey || !newCfgValue"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>
<script>
import {defineComponent, onMounted, ref, computed} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router";

import {useQuasar} from "quasar";

import {
  CONFIGURATION_ADDLIST_CONFIG_VALUE,
  CONFIGURATION_KEY_LIST,
  CONFIGURATION_LIST,
  CONFIGURATION_REMOVE_CONFIG,
  CONFIGURATION_UPDATE
} from "@/graphql/queries";

import _ from "lodash";

export default defineComponent({
  name: 'ConfigurationView',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const route = useRoute();
    
    // State
    const cfgList = ref([]);
    const loading = ref(false);
    const addConfig = ref(false);
    const newCfgKey = ref(null);
    const newCfgValue = ref(null);
    const newCfgDescription = ref('');
    const keyOptions = ref([]);
    const keyOptionsFiltered = ref([]);

    // Computed
    const entityType = computed(() => route.query.type || 'Unknown');
    const entityId = computed(() => route.params.idPrimary || 'N/A');

    /**
     * Fetch configuration list and available keys
     */
    const fetchData = () => {
      loading.value = true;
      
      // Fetch configurations
      client.query({
        query: CONFIGURATION_LIST,
        variables: {
          entityType: entityType.value.toUpperCase(),
          entityId: entityId.value
        },
        fetchPolicy: 'network-only',
      }).then(response => {
        // Create fully writable copies using JSON parse/stringify to ensure mutability
        // This breaks any Apollo Client object freezing
        const rawData = JSON.parse(JSON.stringify(response.data.configurationListByEntity));
        cfgList.value = rawData.map(cfg => ({
          ...cfg,
          // Ensure all properties are explicitly writable
          id: cfg.id,
          key: cfg.key,
          value: cfg.value || '',
          description: cfg.description || '',
          name: cfg.name || '',
          entityType: cfg.entityType,
          entityId: cfg.entityId
        }));
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load configurations',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching configurations:', error);
      });

      // Fetch available keys
      client.query({
        query: CONFIGURATION_KEY_LIST,
        variables: {entityType: entityType.value.toUpperCase()},
        fetchPolicy: 'network-only',
      }).then(response => {
        keyOptions.value = [...response.data.configKeysByEntity];
        keyOptionsFiltered.value = [...keyOptions.value];
      }).catch(error => {
        console.error('Error fetching configuration keys:', error);
      });
    };

    /**
     * Open add configuration dialog
     */
    const openAddDialog = () => {
      newCfgKey.value = null;
      newCfgValue.value = null;
      newCfgDescription.value = '';
      addConfig.value = true;
    };

    /**
     * Add new configuration
     */
    const onAdd = () => {
      if (!newCfgKey.value || !newCfgValue.value) {
        $q.notify({
          color: 'warning',
          message: 'Please fill in required fields',
          icon: 'mdi-alert',
          position: 'top'
        });
        return;
      }

      client.mutate({
        mutation: CONFIGURATION_ADDLIST_CONFIG_VALUE,
        variables: {
          key: newCfgKey.value,
          entityId: entityId.value,
          entityType: entityType.value.toUpperCase(),
          value: newCfgValue.value,
          description: newCfgDescription.value || ''
        },
        fetchPolicy: 'no-cache',
      }).then(() => {
        $q.notify({
          color: 'positive',
          message: 'Configuration added successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        addConfig.value = false;
        newCfgKey.value = null;
        newCfgValue.value = null;
        newCfgDescription.value = '';
        fetchData();
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to add configuration',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error adding configuration:', error);
      });
    };

    /**
     * Update existing configuration
     */
    const onUpdate = (cfg) => {
      if (!cfg.value) {
        $q.notify({
          color: 'warning',
          message: 'Value cannot be empty',
          icon: 'mdi-alert',
          position: 'top'
        });
        return;
      }

      const updatable = _.cloneDeep(cfg);
      delete updatable.id;
      delete updatable.__typename;

      client.mutate({
        mutation: CONFIGURATION_UPDATE,
        variables: {id: cfg.id, configuration: updatable},
        fetchPolicy: 'no-cache',
      }).then(() => {
        $q.notify({
          color: 'positive',
          message: 'Configuration updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        fetchData();
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to update configuration',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating configuration:', error);
      });
    };

    /**
     * Remove configuration
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete configuration "${toDelete.key}"?`,
        ok: {
          push: true,
          color: 'negative',
          label: "Delete",
          icon: 'mdi-delete'
        },
        cancel: {
          push: true,
          color: 'grey',
          label: 'Cancel'
        }
      }).onOk(() => {
        client.mutate({
          mutation: CONFIGURATION_REMOVE_CONFIG,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
        }).then(response => {
          if (response.data.removeConfig.success) {
            $q.notify({
              color: 'positive',
              message: 'Configuration deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.removeConfig.error || 'Failed to delete configuration',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete configuration',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting configuration:', error);
        });
      });
    };

    /**
     * Filter configuration keys
     */
    const filterFn = (val, update) => {
      update(() => {
        if (val === '') {
          keyOptionsFiltered.value = keyOptions.value;
        } else {
          const needle = val.toLowerCase();
          keyOptionsFiltered.value = keyOptions.value.filter(
            v => v.toLowerCase().includes(needle)
          );
        }
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      loading,
      cfgList,
      newCfgKey,
      newCfgValue,
      newCfgDescription,
      addConfig,
      keyOptionsFiltered,
      entityType,
      entityId,
      openAddDialog,
      onAdd,
      onUpdate,
      removeItem,
      filterFn
    };
  }
});

</script>

<style scoped>
.config-card {
  transition: all 0.3s ease;
}

.config-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
