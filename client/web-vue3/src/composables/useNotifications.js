/**
 * Composable for standardized notifications across the application
 * Provides consistent notification patterns for success, error, warning, and info messages
 */
import { useQuasar } from 'quasar';

export function useNotifications() {
  const $q = useQuasar();

  /**
   * Show a success notification
   * @param {string} message - The success message to display
   * @param {Object} options - Additional Quasar notify options
   */
  const notifySuccess = (message, options = {}) => {
    $q.notify({
      color: 'positive',
      message,
      icon: 'mdi-check-circle',
      position: 'top',
      timeout: 3000,
      ...options
    });
  };

  /**
   * Show an error notification
   * @param {string} message - The error message to display
   * @param {Error|Object} error - Optional error object for logging
   * @param {Object} options - Additional Quasar notify options
   */
  const notifyError = (message, error = null, options = {}) => {
    // Log error to console if provided
    if (error) {
      console.error(message, error);
    }

    // Extract error message if it's an Error object
    const errorMessage = error?.message || error?.graphQLErrors?.[0]?.message || '';
    const fullMessage = errorMessage ? `${message}: ${errorMessage}` : message;

    $q.notify({
      color: 'negative',
      message: fullMessage,
      icon: 'mdi-alert-circle',
      position: 'top',
      timeout: 5000,
      ...options
    });
  };

  /**
   * Show a warning notification
   * @param {string} message - The warning message to display
   * @param {Object} options - Additional Quasar notify options
   */
  const notifyWarning = (message, options = {}) => {
    $q.notify({
      color: 'warning',
      message,
      icon: 'mdi-alert',
      position: 'top',
      timeout: 4000,
      ...options
    });
  };

  /**
   * Show an info notification
   * @param {string} message - The info message to display
   * @param {Object} options - Additional Quasar notify options
   */
  const notifyInfo = (message, options = {}) => {
    $q.notify({
      color: 'info',
      message,
      icon: 'mdi-information',
      position: 'top',
      timeout: 3000,
      ...options
    });
  };

  /**
   * Show a validation error notification
   * @param {string} message - The validation error message
   * @param {Object} options - Additional Quasar notify options
   */
  const notifyValidationError = (message = 'Please fill in all required fields', options = {}) => {
    $q.notify({
      color: 'negative',
      message,
      icon: 'mdi-alert-circle',
      position: 'top',
      timeout: 4000,
      ...options
    });
  };

  /**
   * Show a loading notification (returns a dismiss function)
   * @param {string} message - The loading message to display
   * @param {Object} options - Additional Quasar notify options
   * @returns {Function} dismiss - Function to dismiss the notification
   */
  const notifyLoading = (message = 'Loading...', options = {}) => {
    return $q.notify({
      color: 'info',
      message,
      icon: 'mdi-loading mdi-spin',
      position: 'top',
      timeout: 0, // Don't auto-dismiss
      ...options
    });
  };

  return {
    notifySuccess,
    notifyError,
    notifyWarning,
    notifyInfo,
    notifyValidationError,
    notifyLoading
  };
}

