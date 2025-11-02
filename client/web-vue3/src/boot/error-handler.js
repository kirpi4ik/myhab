import { boot } from 'quasar/wrappers';

export default boot(() => {
  // Suppress ResizeObserver errors
  // These are benign errors that occur when ResizeObserver callbacks complete
  // with undelivered notifications, typically during rapid DOM updates
  window.addEventListener('error', (e) => {
    if (e.message === 'ResizeObserver loop completed with undelivered notifications.' ||
        e.message === 'ResizeObserver loop limit exceeded') {
      e.stopImmediatePropagation();
      e.stopPropagation();
      e.preventDefault();
    }
    // Suppress Apollo cache normalization warnings that cause "Cannot convert object to primitive value"
    if (e.message && e.message.includes('Cannot convert object to primitive value')) {
      e.stopImmediatePropagation();
      e.stopPropagation();
      e.preventDefault();
    }
  });

  // Also handle unhandled promise rejections with ResizeObserver
  window.addEventListener('unhandledrejection', (e) => {
    if (e.reason && e.reason.message && 
        (e.reason.message.includes('ResizeObserver') || 
         e.reason.message.includes('ResizeObserver loop'))) {
      e.stopImmediatePropagation();
      e.stopPropagation();
      e.preventDefault();
    }
  });

  // Override console.warn to suppress Apollo data loss warnings
  const originalWarn = console.warn;
  console.warn = function(...args) {
    try {
      // Safely check if this is an Apollo data loss warning
      // without trying to convert objects to strings
      const hasApolloWarning = args.some(arg => {
        if (typeof arg === 'string') {
          return arg.includes('writeToStore') || 
                 arg.includes('warnAboutDataLoss') ||
                 arg.includes('Missing field');
        }
        return false;
      });
      
      if (hasApolloWarning) {
        // Silently ignore Apollo cache warnings
        return;
      }
      
      // Call the original console.warn for other warnings
      // Wrap in try-catch in case args contain non-convertible objects
      try {
        originalWarn.apply(console, args);
      } catch (e) {
        // If we can't log the warning, just silently ignore it
        // This prevents "Cannot convert object to primitive value" errors
      }
    } catch (e) {
      // If there's any error in our error handler, suppress everything
      // Better to miss a warning than break the app
    }
  };
});

