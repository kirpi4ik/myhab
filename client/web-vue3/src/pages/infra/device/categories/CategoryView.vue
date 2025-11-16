<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10" color="purple-7">
          <q-icon name="mdi-shape" size="xl" color="white"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="column">
          <div class="text-h4 text-secondary">{{ viewItem.name || 'Unnamed Category' }}</div>
          <div class="text-subtitle2 text-grey-7">{{ viewItem.title || 'Device Category' }}</div>
        </div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Category</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- Basic Information -->
      <q-list>
        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-identifier" class="q-mr-sm"/>
              ID
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.id }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-label" class="q-mr-sm"/>
              Name
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.name || 'Unnamed'"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.title">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-format-title" class="q-mr-sm"/>
              Title
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.title }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.description">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-text" class="q-mr-sm"/>
              Description
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.description }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Timestamps Section -->
        <q-item-label header class="text-h6 text-grey-8">Timestamps</q-item-label>

        <q-item v-if="viewItem.tsCreated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-plus" class="q-mr-sm"/>
              Created
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsCreated) }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.tsUpdated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-edit" class="q-mr-sm"/>
              Last Updated
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsUpdated) }}</q-item-label>
          </q-item-section>
        </q-item>

      </q-list>

      <q-separator/>

      <!-- Related Devices Section -->
      <div class="q-pa-md">
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-devices" class="q-mr-sm"/>
          Devices in this Category
        </div>
        
        <div v-if="viewItem.devices && viewItem.devices.length > 0">
          <q-list bordered separator>
            <q-item 
              v-for="device in viewItem.devices" 
              :key="device.id"
              clickable
              @click="viewDevice(device)"
            >
              <q-item-section avatar>
                <q-icon name="mdi-devices" color="indigo-7"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>
                  <q-badge color="primary" :label="device.code" class="q-mr-sm"/>
                  {{ device.name }}
                </q-item-label>
                <q-item-label caption v-if="device.model">{{ device.model }}</q-item-label>
              </q-item-section>
              <q-item-section side>
                <q-btn 
                  icon="mdi-eye" 
                  flat 
                  dense 
                  color="blue-6"
                  @click.stop="viewDevice(device)"
                >
                  <q-tooltip>View Device</q-tooltip>
                </q-btn>
              </q-item-section>
            </q-item>
          </q-list>
        </div>

        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-devices-off" size="md"/>
          <div>No devices in this category</div>
        </div>
      </div>

      <q-separator/>

      <!-- Actions -->
      <q-card-actions>
        <q-btn color="primary" :to="uri +'/'+ $route.params.idPrimary+'/edit'" icon="mdi-pencil">
          Edit
        </q-btn>
        <q-btn color="grey" @click="$router.go(-1)" icon="mdi-arrow-left">
          Back
        </q-btn>
      </q-card-actions>
    </q-card>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from "quasar";

import {DEVICE_CATEGORY_BY_ID} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'DCategoryView',
  setup() {
    const uri = '/admin/dcategories';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        const date = new Date(dateString);
        return format(date, 'dd/MM/yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    /**
     * Fetch category data from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_CATEGORY_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.deviceCategory;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load category details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching category:', error);
      });
    };

    /**
     * Navigate to device view
     */
    const viewDevice = (device) => {
      router.push({path: `/admin/devices/${device.id}/view`});
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      formatDate,
      viewDevice
    };
  }
});

</script>
