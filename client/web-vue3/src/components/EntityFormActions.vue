<template>
  <q-card-actions>
    <!-- Save Button -->
    <q-btn 
      v-if="showSave"
      color="primary" 
      type="submit" 
      :icon="saveIcon"
      :loading="saving"
      :disable="saving || disabled"
    >
      {{ saveLabel }}
    </q-btn>

    <!-- Cancel Button -->
    <q-btn 
      v-if="showCancel"
      color="grey" 
      :icon="cancelIcon"
      :disable="saving || disabled"
      @click="handleCancel"
    >
      {{ cancelLabel }}
    </q-btn>

    <q-space v-if="showView || showDelete || $slots.actions"/>

    <!-- View Button -->
    <q-btn 
      v-if="showView && viewRoute"
      color="info" 
      :to="viewRoute" 
      :icon="viewIcon" 
      outline
      :disable="saving || disabled"
    >
      {{ viewLabel }}
      <q-tooltip v-if="viewTooltip">{{ viewTooltip }}</q-tooltip>
    </q-btn>

    <!-- Delete Button -->
    <q-btn 
      v-if="showDelete"
      color="negative" 
      :icon="deleteIcon" 
      outline
      :disable="saving || disabled"
      @click="handleDelete"
    >
      {{ deleteLabel }}
      <q-tooltip v-if="deleteTooltip">{{ deleteTooltip }}</q-tooltip>
    </q-btn>

    <!-- Custom Actions Slot -->
    <slot name="actions"></slot>
  </q-card-actions>
</template>

<script>
import { defineComponent } from 'vue';
import { useRouter } from 'vue-router';

export default defineComponent({
  name: 'EntityFormActions',
  props: {
    /**
     * Show save button
     */
    showSave: {
      type: Boolean,
      default: true
    },
    /**
     * Show cancel button
     */
    showCancel: {
      type: Boolean,
      default: true
    },
    /**
     * Show view button
     */
    showView: {
      type: Boolean,
      default: false
    },
    /**
     * Show delete button
     */
    showDelete: {
      type: Boolean,
      default: false
    },
    /**
     * Saving state
     */
    saving: {
      type: Boolean,
      default: false
    },
    /**
     * Disable all buttons
     */
    disabled: {
      type: Boolean,
      default: false
    },
    /**
     * Save button label
     */
    saveLabel: {
      type: String,
      default: 'Save'
    },
    /**
     * Cancel button label
     */
    cancelLabel: {
      type: String,
      default: 'Cancel'
    },
    /**
     * View button label
     */
    viewLabel: {
      type: String,
      default: 'View'
    },
    /**
     * Delete button label
     */
    deleteLabel: {
      type: String,
      default: 'Delete'
    },
    /**
     * Save button icon
     */
    saveIcon: {
      type: String,
      default: 'mdi-content-save'
    },
    /**
     * Cancel button icon
     */
    cancelIcon: {
      type: String,
      default: 'mdi-cancel'
    },
    /**
     * View button icon
     */
    viewIcon: {
      type: String,
      default: 'mdi-eye'
    },
    /**
     * Delete button icon
     */
    deleteIcon: {
      type: String,
      default: 'mdi-delete'
    },
    /**
     * Route for view button
     */
    viewRoute: {
      type: [String, Object],
      default: null
    },
    /**
     * View button tooltip
     */
    viewTooltip: {
      type: String,
      default: ''
    },
    /**
     * Delete button tooltip
     */
    deleteTooltip: {
      type: String,
      default: 'Delete this item'
    },
    /**
     * Custom cancel behavior (if not provided, goes back)
     */
    onCancel: {
      type: Function,
      default: null
    },
    /**
     * Custom delete behavior
     */
    onDelete: {
      type: Function,
      default: null
    }
  },
  emits: ['cancel', 'delete'],
  setup(props, { emit }) {
    const router = useRouter();

    /**
     * Handle cancel action
     */
    const handleCancel = () => {
      if (props.onCancel) {
        props.onCancel();
      } else {
        emit('cancel');
        router.go(-1);
      }
    };

    /**
     * Handle delete action
     */
    const handleDelete = () => {
      if (props.onDelete) {
        props.onDelete();
      } else {
        emit('delete');
      }
    };

    return {
      handleCancel,
      handleDelete
    };
  }
});
</script>

