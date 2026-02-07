<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header -->
      <q-card-section>
        <div class="row items-center q-mb-md">
          <div class="col">
            <div class="text-h5">
              <q-icon name="mdi-cog-sync" color="primary" size="sm" class="q-mr-sm"/>
              Application Configuration
            </div>
            <div class="text-subtitle2 text-grey-7">
              Manage application settings stored in GIT repository
            </div>
          </div>
          <div class="col-auto q-gutter-sm">
            <q-btn 
              icon="mdi-refresh" 
              color="info" 
              :loading="refreshing" 
              label="Refresh from GIT" 
              @click="onRefresh"
              unelevated
            >
              <q-tooltip>Pull latest configuration from GIT</q-tooltip>
            </q-btn>
          </div>
        </div>
        
        <!-- Search/Filter -->
        <q-input 
          v-model="searchFilter" 
          label="Search configurations" 
          dense 
          filled
          clearable
          class="q-mt-md"
        >
          <template v-slot:prepend>
            <q-icon name="mdi-magnify"/>
          </template>
        </q-input>
      </q-card-section>

      <q-separator/>

      <!-- Configuration Table -->
      <q-card-section>
        <q-table
          :rows="filteredConfigs"
          :columns="columns"
          row-key="key"
          :loading="loading"
          :pagination="pagination"
          dense
          flat
          bordered
          :rows-per-page-options="[15, 30, 50, 100]"
        >
          <!-- Key column -->
          <template v-slot:body-cell-key="props">
            <q-td :props="props">
              <code class="text-primary">{{ props.row.key }}</code>
              <q-tooltip>{{ props.row.key }}</q-tooltip>
            </q-td>
          </template>
          
          <!-- Value column -->
          <template v-slot:body-cell-value="props">
            <q-td :props="props">
              <div class="row items-center no-wrap">
                <span class="ellipsis" style="max-width: 400px;">
                  {{ props.row.value }}
                </span>
                <q-btn 
                  icon="mdi-pencil" 
                  size="sm" 
                  flat 
                  round 
                  color="primary"
                  class="q-ml-sm"
                  @click="openEditDialog(props.row)"
                >
                  <q-tooltip>Edit value</q-tooltip>
                </q-btn>
              </div>
              <q-tooltip v-if="props.row.value && props.row.value.length > 50">
                {{ props.row.value }}
              </q-tooltip>
            </q-td>
          </template>
          
          <!-- Type column -->
          <template v-slot:body-cell-type="props">
            <q-td :props="props">
              <q-badge 
                :color="getTypeColor(props.row.type)" 
                :label="props.row.type"
                outline
              />
            </q-td>
          </template>
          
          <!-- No data message -->
          <template v-slot:no-data>
            <div class="full-width row flex-center text-grey-6 q-pa-md">
              <q-icon name="mdi-information-outline" size="sm" class="q-mr-sm"/>
              No configuration entries found
            </div>
          </template>
        </q-table>
      </q-card-section>

      <q-separator/>

      <!-- Footer -->
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
          {{ filteredConfigs.length }} of {{ configList.length }} configuration{{ configList.length !== 1 ? 's' : '' }}
        </div>
      </q-card-actions>
    </q-card>

    <!-- Edit Configuration Dialog -->
    <q-dialog 
      v-model="editDialog" 
      transition-show="slide-up" 
      transition-hide="slide-down"
      persistent
    >
      <q-card style="min-width: 600px">
        <q-card-section class="bg-primary text-white">
          <div class="text-h6">
            <q-icon name="mdi-pencil" class="q-mr-sm"/>
            Edit Configuration
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <!-- Key (read-only) -->
          <q-input
            v-model="editKey"
            label="Configuration Key"
            readonly
            filled
            dense
          >
            <template v-slot:prepend>
              <q-icon name="mdi-key"/>
            </template>
          </q-input>

          <!-- Current Value (read-only) -->
          <q-input
            v-model="editOriginalValue"
            label="Current Value"
            readonly
            filled
            dense
            type="textarea"
            rows="2"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text-box-check"/>
            </template>
          </q-input>

          <!-- New Value -->
          <q-input 
            v-model="editValue" 
            label="New Value *" 
            filled
            dense
            clearable
            type="textarea"
            rows="3"
            hint="Enter the new value for this configuration"
            :rules="[val => !!val || 'Value is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text-box"/>
            </template>
          </q-input>

          <!-- Commit Message -->
          <q-input 
            v-model="commitMessage" 
            label="Commit Message" 
            filled
            dense
            clearable
            hint="Optional message for the GIT commit"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-source-commit"/>
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
            label="Save & Commit" 
            color="positive" 
            @click="onUpdate"
            icon="mdi-content-save"
            :loading="saving"
            :disable="!editValue"
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
import {useQuasar} from "quasar";
import {
  APP_CONFIG_LIST,
  APP_CONFIG_UPDATE,
  APP_CONFIG_REFRESH
} from "@/graphql/queries";

export default defineComponent({
  name: 'AppConfigView',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    
    // State
    const configList = ref([]);
    const loading = ref(false);
    const refreshing = ref(false);
    const saving = ref(false);
    const searchFilter = ref('');
    
    // Edit dialog state
    const editDialog = ref(false);
    const editKey = ref('');
    const editValue = ref('');
    const editOriginalValue = ref('');
    const commitMessage = ref('');
    
    // Table pagination
    const pagination = ref({
      sortBy: 'key',
      descending: false,
      page: 1,
      rowsPerPage: 30
    });
    
    // Table columns
    const columns = [
      {
        name: 'key',
        label: 'Key',
        field: 'key',
        align: 'left',
        sortable: true,
        style: 'width: 40%'
      },
      {
        name: 'value',
        label: 'Value',
        field: 'value',
        align: 'left',
        sortable: true,
        style: 'width: 50%'
      },
      {
        name: 'type',
        label: 'Type',
        field: 'type',
        align: 'center',
        sortable: true,
        style: 'width: 10%'
      }
    ];
    
    // Computed: filtered configurations
    const filteredConfigs = computed(() => {
      if (!searchFilter.value) {
        return configList.value;
      }
      const needle = searchFilter.value.toLowerCase();
      return configList.value.filter(cfg => 
        cfg.key.toLowerCase().includes(needle) || 
        (cfg.value && cfg.value.toLowerCase().includes(needle))
      );
    });
    
    /**
     * Get color for type badge
     */
    const getTypeColor = (type) => {
      const typeColors = {
        'String': 'primary',
        'Integer': 'orange',
        'Long': 'orange',
        'Double': 'orange',
        'Boolean': 'teal',
        'ArrayList': 'purple',
        'LinkedHashMap': 'indigo',
        'null': 'grey'
      };
      return typeColors[type] || 'grey';
    };
    
    /**
     * Fetch configuration list
     */
    const fetchData = () => {
      loading.value = true;
      
      client.query({
        query: APP_CONFIG_LIST,
        fetchPolicy: 'network-only',
      }).then(response => {
        configList.value = response.data.appConfigList || [];
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load configurations',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching app configs:', error);
      });
    };
    
    /**
     * Refresh configuration from GIT
     */
    const onRefresh = () => {
      refreshing.value = true;
      
      client.mutate({
        mutation: APP_CONFIG_REFRESH,
        fetchPolicy: 'no-cache',
      }).then(response => {
        refreshing.value = false;
        if (response.data.appConfigRefresh.success) {
          $q.notify({
            color: 'positive',
            message: 'Configuration refreshed from GIT',
            icon: 'mdi-check-circle',
            position: 'top'
          });
          fetchData();
        } else {
          $q.notify({
            color: 'negative',
            message: response.data.appConfigRefresh.error || 'Failed to refresh configuration',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
        }
      }).catch(error => {
        refreshing.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to refresh configuration',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error refreshing app config:', error);
      });
    };
    
    /**
     * Open edit dialog
     */
    const openEditDialog = (config) => {
      editKey.value = config.key;
      editOriginalValue.value = config.value || '';
      editValue.value = config.value || '';
      commitMessage.value = '';
      editDialog.value = true;
    };
    
    /**
     * Update configuration
     */
    const onUpdate = () => {
      if (!editValue.value) {
        $q.notify({
          color: 'warning',
          message: 'Please enter a value',
          icon: 'mdi-alert',
          position: 'top'
        });
        return;
      }
      
      saving.value = true;
      
      client.mutate({
        mutation: APP_CONFIG_UPDATE,
        variables: {
          key: editKey.value,
          value: editValue.value,
          commitMessage: commitMessage.value || null
        },
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        if (response.data.appConfigUpdate.success) {
          $q.notify({
            color: 'positive',
            message: 'Configuration updated and committed to GIT',
            icon: 'mdi-check-circle',
            position: 'top'
          });
          editDialog.value = false;
          fetchData();
        } else {
          $q.notify({
            color: 'negative',
            message: response.data.appConfigUpdate.error || 'Failed to update configuration',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
        }
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to update configuration',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating app config:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      // State
      configList,
      loading,
      refreshing,
      saving,
      searchFilter,
      filteredConfigs,
      pagination,
      columns,
      
      // Edit dialog
      editDialog,
      editKey,
      editValue,
      editOriginalValue,
      commitMessage,
      
      // Methods
      getTypeColor,
      fetchData,
      onRefresh,
      openEditDialog,
      onUpdate
    };
  }
});
</script>

<style scoped>
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
