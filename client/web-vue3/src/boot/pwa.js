/**
 * PWA Service Worker Registration and Update Handler
 */

import { Notify } from 'quasar';

export default ({ app }) => {
  // Check if the app is running in PWA mode
  if (process.env.MODE === 'pwa') {
    // Listen for service worker updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.ready.then((registration) => {
        // Check for updates periodically (every hour)
        setInterval(() => {
          registration.update();
        }, 1000 * 60 * 60);

        // Listen for new service worker installation
        registration.addEventListener('updatefound', () => {
          const newWorker = registration.installing;

          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                // New service worker is ready
                Notify.create({
                  message: 'A new version is available!',
                  caption: 'Click to reload and update',
                  icon: 'mdi-update',
                  color: 'primary',
                  position: 'top',
                  timeout: 0,
                  actions: [
                    {
                      label: 'Update',
                      color: 'white',
                      handler: () => {
                        newWorker.postMessage({ type: 'SKIP_WAITING' });
                        window.location.reload();
                      }
                    },
                    {
                      label: 'Dismiss',
                      color: 'white',
                      handler: () => {}
                    }
                  ]
                });
              }
            });
          }
        });
      });

      // Handle service worker controller change
      let refreshing = false;
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (!refreshing) {
          refreshing = true;
          window.location.reload();
        }
      });
    }

    // Add install prompt event handler for Chrome
    let deferredPrompt;
    window.addEventListener('beforeinstallprompt', (e) => {
      // Prevent the mini-infobar from appearing on mobile
      e.preventDefault();
      // Stash the event so it can be triggered later
      deferredPrompt = e;
      
      // Show install notification after 30 seconds if not already installed
      setTimeout(() => {
        if (deferredPrompt && !window.matchMedia('(display-mode: standalone)').matches) {
          Notify.create({
            message: 'Install myHAB App',
            caption: 'Install myHAB on your device for a better experience',
            icon: 'mdi-download',
            color: 'green-6',
            position: 'bottom',
            timeout: 10000,
            actions: [
              {
                label: 'Install',
                color: 'white',
                handler: async () => {
                  if (deferredPrompt) {
                    deferredPrompt.prompt();
                    const { outcome } = await deferredPrompt.userChoice;
                    console.log(`User response to the install prompt: ${outcome}`);
                    deferredPrompt = null;
                  }
                }
              },
              {
                label: 'Later',
                color: 'white',
                handler: () => {}
              }
            ]
          });
        }
      }, 30000);
    });

    // Log when app is installed
    window.addEventListener('appinstalled', () => {
      console.log('myHAB PWA was installed');
      Notify.create({
        message: 'myHAB installed successfully!',
        icon: 'mdi-check-circle',
        color: 'positive',
        position: 'top',
        timeout: 3000
      });
    });

    // Detect if running as PWA
    if (window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true) {
      console.log('Running as PWA');
      app.config.globalProperties.$isPWA = true;
    } else {
      console.log('Running in browser');
      app.config.globalProperties.$isPWA = false;
    }
  }
};

