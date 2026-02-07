/**
 * Composable for standardized list operations
 * Provides common functionality for listing, searching, filtering, and deleting entities
 */
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { useNotifications } from './useNotifications';

/**
 * @typedef {Object} EntityListOptions
 * @property {string} entityName - Display name of the entity (e.g., 'Cable', 'Device')
 * @property {string} entityPath - Base path for routing (e.g., '/admin/cables')
 * @property {Object} listQuery - GraphQL query for fetching list of entities
 * @property {Object} deleteMutation - GraphQL mutation for deleting entity
 * @property {string} listKey - Key in response data containing the list (defaults to entityName + 'List')
 * @property {Array<Object>} columns - Table columns configuration for q-table
 * @property {Function} transformAfterLoad - Optional function to transform data after loading
 */

export function useEntityList(options) {
  const {
    entityName,
    entityPath,
    listQuery,
    deleteMutation,
    listKey,
    columns = [],
    transformAfterLoad
  } = options;

  const router = useRouter();
  const { client } = useApolloClient();
  const $q = useQuasar();
  const { notifySuccess, notifyError } = useNotifications();

  const items = ref([]);
  const loading = ref(false);
  const filter = ref('');
  const pagination = ref({
    sortBy: 'id',
    descending: false,
    page: 1,
    rowsPerPage: 10,
    rowsNumber: 0
  });

  /**
   * Fetch list of entities
   * @param {Object} additionalVariables - Additional variables for the query
   * @returns {Promise<Object>} The query response data
   */
  const fetchList = async (additionalVariables = {}) => {
    loading.value = true;
    try {
      const response = await client.query({
        query: listQuery,
        variables: additionalVariables,
        fetchPolicy: 'network-only',
      });

      // Extract list from response
      const key = listKey || `${entityName.toLowerCase()}List`;
      items.value = response.data[key] || [];

      // Apply transformation if provided
      if (transformAfterLoad) {
        items.value = items.value.map(transformAfterLoad);
      }

      // Update pagination
      pagination.value.rowsNumber = items.value.length;

      return response.data;
    } catch (error) {
      notifyError(`Failed to load ${entityName.toLowerCase()} list`, error);
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * Filtered items based on search filter
   */
  const filteredItems = computed(() => {
    if (!filter.value) {
      return items.value;
    }

    const searchTerm = filter.value.toLowerCase();
    return items.value.filter(item => {
      // Search across all string fields
      return Object.values(item).some(value => {
        if (typeof value === 'string') {
          return value.toLowerCase().includes(searchTerm);
        }
        if (typeof value === 'object' && value !== null) {
          // Search in nested objects (e.g., category.name)
          return Object.values(value).some(nestedValue =>
            typeof nestedValue === 'string' && nestedValue.toLowerCase().includes(searchTerm)
          );
        }
        return false;
      });
    });
  });

  /**
   * Navigate to entity view page
   * @param {Object} item - The entity to view
   */
  const viewItem = (item) => {
    if (entityPath && item.id) {
      router.push({ path: `${entityPath}/${item.id}/view` });
    }
  };

  /**
   * Navigate to entity edit page
   * @param {Object} item - The entity to edit
   */
  const editItem = (item) => {
    if (entityPath && item.id) {
      router.push({ path: `${entityPath}/${item.id}/edit` });
    }
  };

  /**
   * Navigate to entity create page
   */
  const createItem = () => {
    if (entityPath) {
      router.push({ path: `${entityPath}/new` });
    }
  };

  /**
   * Delete an entity with confirmation dialog
   * @param {Object} item - The entity to delete
   * @returns {Promise<boolean>} True if deleted successfully
   */
  const deleteItem = async (item) => {
    if (!deleteMutation) {
      notifyError('Delete operation is not configured');
      return false;
    }

    if (!item || !item.id) {
      notifyError(`${entityName} ID is required`);
      return false;
    }

    // Show confirmation dialog
    return new Promise((resolve) => {
      $q.dialog({
        title: `Delete ${entityName}`,
        message: `Are you sure you want to delete "${item.name || item.code || item.id}"?`,
        cancel: true,
        persistent: true,
        color: 'negative',
        ok: {
          label: 'Delete',
          color: 'negative',
          icon: 'mdi-delete'
        },
        cancel: {
          label: 'Cancel',
          flat: true
        }
      }).onOk(async () => {
        try {
          const response = await client.mutate({
            mutation: deleteMutation,
            variables: { id: item.id },
            fetchPolicy: 'no-cache',
          });

          const mutationKey = Object.keys(response.data)[0];
          const result = response.data[mutationKey];

          if (result && result.success) {
            notifySuccess(`${entityName} deleted successfully`);
            
            // Remove item from list
            items.value = items.value.filter(i => i.id !== item.id);
            pagination.value.rowsNumber = items.value.length;
            
            resolve(true);
          } else {
            notifyError(result?.error || `Failed to delete ${entityName.toLowerCase()}`);
            resolve(false);
          }
        } catch (error) {
          notifyError(`Failed to delete ${entityName.toLowerCase()}`, error);
          resolve(false);
        }
      }).onCancel(() => {
        resolve(false);
      });
    });
  };

  /**
   * Handle table request (for server-side pagination if needed)
   * @param {Object} props - Request props from q-table
   */
  const onRequest = async (props) => {
    const { page, rowsPerPage, sortBy, descending } = props.pagination;

    pagination.value.page = page;
    pagination.value.rowsPerPage = rowsPerPage;
    pagination.value.sortBy = sortBy;
    pagination.value.descending = descending;

    // For now, we're doing client-side pagination
    // In the future, this can be extended to support server-side pagination
  };

  /**
   * Refresh the list
   */
  const refresh = async () => {
    await fetchList();
  };

  /**
   * Export list to CSV
   * @param {Array<string>} columnKeys - Keys of columns to export
   */
  const exportToCSV = (columnKeys = null) => {
    const keys = columnKeys || columns.map(col => col.field);
    const headers = columns.filter(col => keys.includes(col.field)).map(col => col.label);
    
    // Build CSV content
    const csvContent = [
      headers.join(','),
      ...filteredItems.value.map(item => {
        return keys.map(key => {
          const value = key.includes('.') 
            ? key.split('.').reduce((obj, k) => obj?.[k], item)
            : item[key];
          
          // Escape commas and quotes
          const escaped = String(value || '').replace(/"/g, '""');
          return `"${escaped}"`;
        }).join(',');
      })
    ].join('\n');

    // Create download link
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `${entityName.toLowerCase()}-list-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    notifySuccess('List exported to CSV');
  };

  return {
    items,
    filteredItems,
    loading,
    filter,
    pagination,
    fetchList,
    viewItem,
    editItem,
    createItem,
    deleteItem,
    onRequest,
    refresh,
    exportToCSV
  };
}

