/**
 * Composable for standardized CRUD operations
 * Provides common functionality for Create, Read, Update, Delete operations
 */
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useApolloClient } from '@vue/apollo-composable';
import { useNotifications } from './useNotifications';
import { prepareForMutation } from '@/_helpers/apollo-utils';
import _ from 'lodash';

/**
 * @typedef {Object} EntityCRUDOptions
 * @property {string} entityName - Display name of the entity (e.g., 'Cable', 'Device')
 * @property {string} entityPath - Base path for routing (e.g., '/admin/cables')
 * @property {Object} getQuery - GraphQL query for fetching single entity
 * @property {string} getQueryKey - Key to extract entity from query response (optional)
 * @property {Object} createMutation - GraphQL mutation for creating entity
 * @property {string} createMutationKey - Key to extract result from create mutation (optional)
 * @property {string} createVariableName - Variable name for create mutation (optional, auto-derived if not provided)
 * @property {Object} updateMutation - GraphQL mutation for updating entity
 * @property {string} updateMutationKey - Key to extract result from update mutation (optional)
 * @property {string} updateVariableName - Variable name for update mutation (optional, auto-derived if not provided)
 * @property {Object} deleteMutation - GraphQL mutation for deleting entity
 * @property {string} deleteMutationKey - Key to extract result from delete mutation (optional)
 * @property {Array<string>} excludeFields - Fields to exclude from mutations (default: common fields)
 * @property {Function} transformBeforeSave - Optional function to transform data before saving
 * @property {Function} transformAfterLoad - Optional function to transform data after loading
 * @property {Object} initialData - Initial data for new entities (optional, for create mode)
 */

export function useEntityCRUD(options) {
  const {
    entityName,
    entityPath,
    getQuery,
    getQueryKey,
    createMutation,
    createMutationKey,
    createVariableName,
    updateMutation,
    updateMutationKey,
    updateVariableName,
    deleteMutation,
    deleteMutationKey,
    excludeFields = ['__typename', 'entityType', 'tsCreated', 'tsUpdated', 'version'],
    transformBeforeSave,
    transformAfterLoad,
    initialData = null
  } = options;

  const router = useRouter();
  const route = useRoute();
  const { client } = useApolloClient();
  const { notifySuccess, notifyError, notifyValidationError } = useNotifications();

  const entity = ref(initialData);
  const loading = ref(false);
  const saving = ref(false);

  /**
   * Fetch entity by ID
   * @param {string|number} id - Entity ID (defaults to route.params.idPrimary)
   * @param {Object} additionalVariables - Additional variables for the query
   * @returns {Promise<Object>} The fetched entity
   */
  const fetchEntity = async (id = null, additionalVariables = {}) => {
    const entityId = id || route.params.idPrimary;
    if (!entityId) {
      notifyError(`${entityName} ID is required`);
      return null;
    }

    loading.value = true;
    try {
      const response = await client.query({
        query: getQuery,
        variables: { id: entityId, ...additionalVariables },
        fetchPolicy: 'network-only',
      });

      // Extract entity from response (handle different response structures)
      const entityKey = getQueryKey || Object.keys(response.data).find(key => 
        !key.endsWith('List') && response.data[key] && typeof response.data[key] === 'object'
      );
      
      entity.value = _.cloneDeep(response.data[entityKey] || response.data);

      // Apply transformation if provided
      if (transformAfterLoad) {
        entity.value = transformAfterLoad(entity.value);
      }

      return response.data;
    } catch (error) {
      notifyError(`Failed to load ${entityName.toLowerCase()} data`, error);
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * Create a new entity
   * @param {Object} data - Entity data to create
   * @param {Object} additionalVariables - Additional variables for the mutation
   * @returns {Promise<Object|null>} The created entity or null on failure
   */
  const createEntity = async (data = null, additionalVariables = {}) => {
    if (!createMutation) {
      notifyError('Create operation is not configured');
      return null;
    }

    saving.value = true;
    try {
      // Use provided data or fall back to entity.value
      const sourceData = data || entity.value;
      
      // Apply transformation if provided
      let cleanData = transformBeforeSave ? transformBeforeSave(sourceData) : sourceData;
      
      // Clean data for mutation
      cleanData = prepareForMutation(cleanData, excludeFields);

      // Determine the variable name for the mutation
      // Priority: explicit createVariableName > derive from createMutationKey > use entityName
      let variableName = createVariableName;
      if (!variableName && createMutationKey) {
        // Try to derive: 'deviceCategoryCreate' -> 'deviceCategory'
        variableName = createMutationKey.replace(/Create$/, '');
      }
      if (!variableName) {
        variableName = entityName;
      }

      const response = await client.mutate({
        mutation: createMutation,
        variables: { [variableName]: cleanData, ...additionalVariables },
        fetchPolicy: 'no-cache',
      });

      // Extract created entity from response
      const mutationKey = createMutationKey || Object.keys(response.data)[0];
      const createdEntity = response.data[mutationKey];

      if (createdEntity && createdEntity.id) {
        notifySuccess(`${entityName} created successfully`);
        
        // Navigate to view or edit page
        if (entityPath) {
          router.push({ path: `${entityPath}/${createdEntity.id}/view` });
        }
        
        return createdEntity;
      } else {
        notifyError(createdEntity?.error || `Failed to create ${entityName.toLowerCase()}`);
        return null;
      }
    } catch (error) {
      notifyError(`Failed to create ${entityName.toLowerCase()}`, error);
      return null;
    } finally {
      saving.value = false;
    }
  };

  /**
   * Update an existing entity
   * @param {string|number} id - Entity ID (defaults to route.params.idPrimary)
   * @param {Object} data - Entity data to update
   * @param {Object} additionalVariables - Additional variables for the mutation
   * @returns {Promise<Object|null>} The updated entity or null on failure
   */
  const updateEntity = async (id = null, data = null, additionalVariables = {}) => {
    if (!updateMutation) {
      notifyError('Update operation is not configured');
      return null;
    }

    const entityId = id || route.params.idPrimary;
    const entityData = data || entity.value;

    if (!entityId) {
      notifyError(`${entityName} ID is required`);
      return null;
    }

    // Prevent duplicate calls
    if (saving.value) {
      return null;
    }

    saving.value = true;
    try {
      // Clean data for mutation FIRST (remove Apollo fields and excluded fields)
      let cleanData = prepareForMutation(entityData, excludeFields);
      
      // Apply transformation AFTER cleaning (so transform can add back necessary IDs)
      if (transformBeforeSave) {
        cleanData = transformBeforeSave(cleanData);
      }
      
      // Remove top-level id (but keep nested IDs that transform may have added)
      delete cleanData.id;
      
      // Determine the variable name for the mutation
      // Priority: explicit updateVariableName > derive from updateMutationKey > use entityName
      let variableName = updateVariableName;
      if (!variableName && updateMutationKey) {
        // Try to derive: 'deviceCategoryUpdate' -> 'deviceCategory', 'updatePort' -> 'port'
        variableName = updateMutationKey
          .replace(/Update$/, '')  // Remove trailing 'Update'
          .replace(/^update/, '');  // Remove leading 'update'
        // Lowercase first character if needed
        if (variableName) {
          variableName = variableName.charAt(0).toLowerCase() + variableName.slice(1);
        }
      }
      if (!variableName) {
        variableName = entityName;
      }

      const response = await client.mutate({
        mutation: updateMutation,
        variables: { 
          id: entityId, 
          [variableName]: cleanData, 
          ...additionalVariables 
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues
        }
      });

      // Extract updated entity from response
      const mutationKey = updateMutationKey || Object.keys(response.data)[0];
      const updatedEntity = response.data[mutationKey];

      // Check for success - either has id or explicit success flag
      const isSuccess = updatedEntity && (
        updatedEntity.id || 
        (updatedEntity.success === true)
      );

      if (isSuccess) {
        notifySuccess(`${entityName} updated successfully`);
        
        // Refresh data if we're on the same page
        if (route.params.idPrimary === entityId) {
          await fetchEntity(entityId);
        }
        
        return updatedEntity;
      } else {
        // Show error message if provided, otherwise generic message
        const errorMsg = updatedEntity?.error || `Failed to update ${entityName.toLowerCase()}`;
        notifyError(errorMsg);
        return null;
      }
    } catch (error) {
      notifyError(`Failed to update ${entityName.toLowerCase()}`, error);
      return null;
    } finally {
      saving.value = false;
    }
  };

  /**
   * Delete an entity
   * @param {string|number} id - Entity ID to delete
   * @param {Function} confirmCallback - Optional callback for confirmation
   * @returns {Promise<boolean>} True if deleted successfully
   */
  const deleteEntity = async (id, confirmCallback = null) => {
    if (!deleteMutation) {
      notifyError('Delete operation is not configured');
      return false;
    }

    if (!id) {
      notifyError(`${entityName} ID is required`);
      return false;
    }

    // Call confirmation callback if provided
    if (confirmCallback && !(await confirmCallback())) {
      return false;
    }

    try {
      const response = await client.mutate({
        mutation: deleteMutation,
        variables: { id },
        fetchPolicy: 'no-cache',
      });

      const mutationKey = Object.keys(response.data)[0];
      const result = response.data[mutationKey];

      if (result && result.success) {
        notifySuccess(`${entityName} deleted successfully`);
        
        // Navigate to list page if we're on a detail page
        if (entityPath && route.params.idPrimary) {
          router.push({ path: entityPath });
        }
        
        return true;
      } else {
        notifyError(result?.error || `Failed to delete ${entityName.toLowerCase()}`);
        return false;
      }
    } catch (error) {
      notifyError(`Failed to delete ${entityName.toLowerCase()}`, error);
      return false;
    }
  };

  /**
   * Validate required fields
   * @param {Object} data - Data to validate
   * @param {Array<string>} requiredFields - List of required field names
   * @returns {boolean} True if all required fields are present
   */
  const validateRequired = (data, requiredFields) => {
    const missingFields = requiredFields.filter(field => !data[field]);
    
    if (missingFields.length > 0) {
      notifyValidationError(
        `Please fill in all required fields: ${missingFields.join(', ')}`
      );
      return false;
    }
    
    return true;
  };

  return {
    entity,
    loading,
    saving,
    fetchEntity,
    createEntity,
    updateEntity,
    deleteEntity,
    validateRequired
  };
}

