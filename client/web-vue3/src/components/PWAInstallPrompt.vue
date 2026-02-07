<template>
  <div v-if="showInstallButton" class="pwa-install-prompt">
    <q-btn
      :label="label"
      :icon="icon"
      :color="color"
      :flat="flat"
      :unelevated="unelevated"
      :outline="outline"
      @click="installPWA"
      class="pwa-install-btn"
    >
      <q-tooltip v-if="tooltip">{{ tooltip }}</q-tooltip>
    </q-btn>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';

// Props
const props = defineProps({
  label: {
    type: String,
    default: 'Install App'
  },
  icon: {
    type: String,
    default: 'mdi-download'
  },
  color: {
    type: String,
    default: 'primary'
  },
  flat: {
    type: Boolean,
    default: false
  },
  unelevated: {
    type: Boolean,
    default: true
  },
  outline: {
    type: Boolean,
    default: false
  },
  tooltip: {
    type: String,
    default: 'Install myHAB on your device'
  }
});

// Composables
const $q = useQuasar();

// State
const showInstallButton = ref(false);
let deferredPrompt = null;

/**
 * Check if app is installable
 */
const checkInstallability = () => {
  // Hide button if already installed (running as PWA)
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches 
    || window.navigator.standalone === true;
  
  if (isStandalone) {
    showInstallButton.value = false;
    return;
  }

  // Show button if prompt is available
  if (deferredPrompt) {
    showInstallButton.value = true;
  }
};

/**
 * Install PWA
 */
const installPWA = async () => {
  if (!deferredPrompt) {
    $q.notify({
      message: 'Installation not available',
      caption: 'Please use your browser menu to install the app',
      icon: 'mdi-information',
      color: 'info',
      position: 'top'
    });
    return;
  }

  // Show the install prompt
  deferredPrompt.prompt();

  // Wait for the user to respond to the prompt
  const { outcome } = await deferredPrompt.userChoice;

  if (outcome === 'accepted') {
    $q.notify({
      message: 'Installing myHAB...',
      icon: 'mdi-check-circle',
      color: 'positive',
      position: 'top',
      timeout: 2000
    });
  }

  // Clear the deferred prompt
  deferredPrompt = null;
  showInstallButton.value = false;
};

// Lifecycle
onMounted(() => {
  // Listen for the beforeinstallprompt event
  window.addEventListener('beforeinstallprompt', (e) => {
    // Prevent the mini-infobar from appearing
    e.preventDefault();
    // Stash the event so it can be triggered later
    deferredPrompt = e;
    // Update UI
    checkInstallability();
  });

  // Listen for app installed event
  window.addEventListener('appinstalled', () => {
    showInstallButton.value = false;
    deferredPrompt = null;
  });

  // Initial check
  checkInstallability();
});
</script>

<style scoped lang="scss">
.pwa-install-prompt {
  .pwa-install-btn {
    font-weight: 600;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }
}
</style>

