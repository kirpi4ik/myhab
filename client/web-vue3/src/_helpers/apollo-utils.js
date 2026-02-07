import _ from 'lodash';

/**
 * Recursively removes Apollo Client specific fields and other unwanted fields from an object
 * This is useful when preparing data for mutations to avoid Apollo cache issues
 * 
 * @param {*} obj - The object to clean (can be object, array, or primitive)
 * @param {string[]} fieldsToRemove - Array of field names to remove (defaults to ['__typename'])
 * @returns {*} - A new object with specified fields removed
 * 
 * @example
 * // Remove Apollo's __typename field
 * const clean = removeApolloFields(data);
 * 
 * @example
 * // Remove multiple fields including id and device
 * const clean = removeApolloFields(data, ['__typename', 'id', 'device']);
 */
export const removeApolloFields = (obj, fieldsToRemove = ['__typename']) => {
  if (Array.isArray(obj)) {
    return obj.map(item => removeApolloFields(item, fieldsToRemove));
  } else if (obj !== null && typeof obj === 'object') {
    return Object.keys(obj).reduce((acc, key) => {
      if (!fieldsToRemove.includes(key)) {
        acc[key] = removeApolloFields(obj[key], fieldsToRemove);
      }
      return acc;
    }, {});
  }
  return obj;
};

/**
 * Creates a clean copy of an object for Apollo mutations
 * Combines cloneDeep with removeApolloFields for convenience
 * 
 * @param {*} obj - The object to clone and clean
 * @param {string[]} fieldsToRemove - Array of field names to remove (defaults to ['__typename'])
 * @returns {*} - A deep cloned object with specified fields removed
 * 
 * @example
 * // Prepare an object for mutation
 * const cleanData = prepareForMutation(cable.value, ['__typename', 'id', 'device']);
 * 
 * client.mutate({
 *   mutation: CABLE_UPDATE,
 *   variables: { id: cableId, cable: cleanData }
 * });
 */
export const prepareForMutation = (obj, fieldsToRemove = ['__typename']) => {
  const cloned = _.cloneDeep(obj);
  return removeApolloFields(cloned, fieldsToRemove);
};

