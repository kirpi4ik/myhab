<template>
  <q-card-section class="bg-blue-grey-1">
    <div class="text-subtitle2 text-weight-medium q-mb-sm">
      <q-icon v-if="icon" :name="icon" class="q-mr-xs"/>
      {{ title }}
    </div>
    <div class="row q-col-gutter-md">
      <!-- ID -->
      <div class="col-12 col-sm-6 col-md-4" v-if="entity.id">
        <q-icon name="mdi-identifier" class="q-mr-xs"/>
        <strong>ID:</strong> {{ entity.id }}
      </div>

      <!-- UID -->
      <div class="col-12 col-sm-6 col-md-4" v-if="entity.uid">
        <q-icon name="mdi-key" class="q-mr-xs"/>
        <strong>UID:</strong> {{ entity.uid }}
      </div>

      <!-- Created Date -->
      <div class="col-12 col-sm-6 col-md-4" v-if="entity.tsCreated && showTimestamps">
        <q-icon name="mdi-calendar-plus" class="q-mr-xs"/>
        <strong>Created:</strong> {{ formatDate(entity.tsCreated) }}
      </div>

      <!-- Updated Date -->
      <div class="col-12 col-sm-6 col-md-4" v-if="entity.tsUpdated && showTimestamps">
        <q-icon name="mdi-calendar-edit" class="q-mr-xs"/>
        <strong>Updated:</strong> {{ formatDate(entity.tsUpdated) }}
      </div>

      <!-- Version -->
      <div class="col-12 col-sm-6 col-md-4" v-if="entity.version && showVersion">
        <q-icon name="mdi-counter" class="q-mr-xs"/>
        <strong>Version:</strong> {{ entity.version }}
      </div>

      <!-- Custom Extra Info -->
      <div 
        v-for="(info, index) in extraInfo" 
        :key="index"
        class="col-12 col-sm-6 col-md-4"
      >
        <q-icon v-if="info.icon" :name="info.icon" class="q-mr-xs"/>
        <strong>{{ info.label }}:</strong> {{ info.value }}
      </div>
    </div>
  </q-card-section>
</template>

<script>
import { defineComponent } from 'vue';
import { format } from 'date-fns';

export default defineComponent({
  name: 'EntityInfoPanel',
  props: {
    /**
     * The entity object containing id, uid, timestamps, etc.
     */
    entity: {
      type: Object,
      required: true
    },
    /**
     * Panel title
     */
    title: {
      type: String,
      default: 'Information'
    },
    /**
     * Icon for the panel title
     */
    icon: {
      type: String,
      default: 'mdi-information'
    },
    /**
     * Show timestamp fields (tsCreated, tsUpdated)
     */
    showTimestamps: {
      type: Boolean,
      default: true
    },
    /**
     * Show version field
     */
    showVersion: {
      type: Boolean,
      default: false
    },
    /**
     * Additional custom information to display
     * Array of objects: { icon: string, label: string, value: any }
     */
    extraInfo: {
      type: Array,
      default: () => []
    }
  },
  setup() {
    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
      } catch (e) {
        return dateString;
      }
    };

    return {
      formatDate
    };
  }
});
</script>

<style scoped>
.q-card-section strong {
  font-weight: 600;
}
</style>

