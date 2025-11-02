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
    // Check if this is an Apollo data loss warning
    const message = args.join(' ');
    if (message.includes('writeToStore') || 
        message.includes('warnAboutDataLoss') ||
        message.includes('Missing field')) {
      // Silently ignore Apollo cache warnings
      return;
    }
    // Call the original console.warn for other warnings
    originalWarn.apply(console, args);
  };
});

