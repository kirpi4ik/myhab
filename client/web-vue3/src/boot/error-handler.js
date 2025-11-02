import { boot } from 'quasar/wrappers';

export default boot(() => {
  // Suppress ResizeObserver errors
  // These are benign errors that occur when ResizeObserver callbacks complete
  // with undelivered notifications, typically during rapid DOM updates
  const resizeObserverError = window.addEventListener('error', (e) => {
    if (e.message === 'ResizeObserver loop completed with undelivered notifications.' ||
        e.message === 'ResizeObserver loop limit exceeded') {
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
});

