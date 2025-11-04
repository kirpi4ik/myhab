<template>
  <div id="fullscreen">
    <!-- Swiper for SVG pages -->
    <swiper 
      v-if="!loading"
      :pagination="{ dynamicBullets: true }" 
      :modules="modules" 
      class="swiper"
      @slideChange="onSlideChange"
      @swiper="onSwiper"
    >
      <swiper-slide v-for="svgPage in svgPages" :key="svgPage.id">
        <inline-svg
          :src="svgPage.svgContent"
          :transform-source="(svg) => transformSvg(svg, svgPage.id)"
          :fill-opacity="svgPage.fillOpacity"
          :stroke-opacity="svgPage.strokeOpacity"
          :color="false"
        />
      </swiper-slide>
    </swiper>

    <!-- Unlock Confirmation Dialog -->
    <q-dialog 
      v-model="unlockDialog.show" 
      transition-show="jump-up" 
      transition-hide="jump-down"
    >
      <q-card class="bg-white">
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="mdi-lock"/>
          <div>{{ $t('mobile.unlock.title') }}</div>
          <q-space/>
          <q-btn dense flat icon="mdi-close" v-close-popup>
            <q-tooltip>{{ $t('common.close') }}</q-tooltip>
          </q-btn>
        </q-bar>

        <q-card-section>
          <div class="text-h6">
            {{ $t('mobile.unlock.message') }}
          </div>
        </q-card-section>

        <q-card-section class="q-pa-none" vertical align="center">
          <div class="q-pa-sm">
            <q-btn 
              flat 
              class="text-h6" 
              icon="mdi-lock-open" 
              :label="$t('mobile.unlock.button')" 
              no-caps 
              @click="handleUnlock"
              :loading="unlocking"
              :disable="unlocking"
            />
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Pagination } from 'swiper/modules';
import { Swiper, SwiperSlide } from 'swiper/vue';
import InlineSvg from 'vue-inline-svg';
import { useWebSocketStore } from '@/store/websocket.store';
import { 
  useNotifications, 
  useSvgInteraction, 
  usePeripheralState, 
  usePeripheralControl 
} from '@/composables';

// Import Swiper styles
import 'swiper/css';
import 'swiper/css/pagination';

// Router
const router = useRouter();

// Swiper modules
const modules = [Pagination];

// SVG pages configuration
const svgPages = ref([
  {
    id: 'screen-1',
    visible: true,
    svgContent: 'svg/screen-1.svg',
    fillOpacity: 0.3,
    strokeOpacity: 0.5,
  },
  {
    id: 'screen-2',
    visible: false,
    svgContent: 'svg/screen-2.svg',
    fillOpacity: 0.3,
    strokeOpacity: 0.5,
  },
]);

const currentPageIndex = ref(0);
const swiperInstance = ref(null);

// Composables
const { notifyError, notifySuccess } = useNotifications();
const { parseAssetId, findClosestNode, applyFocusEffect, transformSvg: svgTransform } = useSvgInteraction();
const { 
  peripherals, 
  loading, 
  error, 
  loadPeripherals, 
  updatePeripheralFromEvent,
  getPeripheral,
  hasPeripheral 
} = usePeripheralState();
const { 
  unlockDialog, 
  handlePeripheralAction, 
  unlockDoor,
  toggleLight,
  toggleHeat,
  showUnlockDialog
} = usePeripheralControl();

// WebSocket store
const wsStore = useWebSocketStore();

// Unlocking state
const unlocking = ref(false);

// Computed
const stompMessage = computed(() => wsStore.ws.message);

/**
 * Transform SVG with current peripheral state
 */
const transformSvg = (svg, pageId) => {
  return svgTransform(svg, peripherals.value, svgPages.value, pageId);
};

/**
 * Handle click events on SVG elements
 */
const handleClick = (event) => {
  
  const targetId = event.target.id;

  // Handle navigation clicks
  if (targetId.startsWith('nav-')) {
    handleNavigation(targetId);
    return;
  }

  // Handle asset clicks
  if (targetId.startsWith('asset-')) {
    handleAssetClick(event);
  }
};

/**
 * Handle navigation button clicks
 */
const handleNavigation = (targetId) => {
  const parsed = parseAssetId(targetId);
  const direction = parsed.info;

  if (direction === 'home') {
    router.push({ path: '/' }).catch(() => {});
  } else if (direction === 'back' && currentPageIndex.value > 0) {
    swiperInstance.value?.slidePrev();
  } else if (direction === 'forward' && currentPageIndex.value < svgPages.value.length - 1) {
    swiperInstance.value?.slideNext();
  }
};

/**
 * Store swiper instance reference
 */
const onSwiper = (swiper) => {
  swiperInstance.value = swiper;
};

/**
 * Handle asset (peripheral) clicks
 */
const handleAssetClick = (event) => {
  const closest = findClosestNode(event.target);
  if (!closest) return;

  // Apply visual feedback
  applyFocusEffect(closest);

  // Parse asset info
  const asset = parseAssetId(closest.id);
  const peripheralId = asset.id;

  if (!hasPeripheral(peripheralId)) {
    // Peripheral not found - silently return
    return;
  }

  // Handle peripheral action
  const peripheral = getPeripheral(peripheralId);
  handlePeripheralAction(peripheral).catch(err => {
    notifyError('Failed to control peripheral');
    console.error('Error handling peripheral action:', err);
  });
};

/**
 * Handle unlock button click
 */
const handleUnlock = async () => {
  unlocking.value = true;
  try {
    await unlockDoor();
    notifySuccess('Door unlocked successfully');
  } catch (err) {
    notifyError('Failed to unlock door');
    console.error('Error unlocking door:', err);
  } finally {
    unlocking.value = false;
  }
};

/**
 * Handle slide change
 */
const onSlideChange = (swiper) => {
  currentPageIndex.value = swiper.activeIndex;
  svgPages.value.forEach((page, index) => {
    page.visible = index === swiper.activeIndex;
  });
};

/**
 * Initialize component
 */
const initialize = async () => {
  try {
    await loadPeripherals();
  } catch (err) {
    notifyError('Failed to load peripherals');
    console.error('Error initializing:', err);
  }
};

// Watch for WebSocket messages
watch(stompMessage, (newVal) => {
  if (newVal?.eventName === 'evt_port_value_persisted') {
    updatePeripheralFromEvent(newVal.jsonPayload);
  }
});

// Watch for route changes
watch(() => router.currentRoute.value.path, () => {
  initialize();
});

// Lifecycle hooks
onMounted(() => {
  initialize();
  document.addEventListener('click', handleClick, false);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClick, false);
});
</script>

<style>

.hidden {
  display: none;
}

.back {
  opacity: 0.8;
  color: #000015;
}

.focus {
  fill: green;
  stroke: yellow;
  stroke-width: 1;
}

.no-focus {
  stroke: none;
}

rect.nav-button {
  stroke: #2b6095;
  stroke-width: 0.5;
  fill: rgb(115, 127, 129);
  fill-rule: nonzero;
  fill-opacity: 0.3;
  paint-order: stroke;
}

path.nav-button {
  stroke: #2b6095;
  stroke-width: 0.5;
  fill: rgb(115, 127, 129);
  fill-rule: nonzero;
}

.motion-off {
  fill: #5a99de;
  fill-opacity: 0.3;
  stroke: #70808e;
  stroke-width: 0.8;
}

.motion-on {
  fill: #e8b8bc;
  fill-opacity: 0.3;
  stroke: #ba1334;
  stroke-width: 0.8;
}

.bulb-on {
  fill: #d6d40f;
  fill-opacity: 0.7;
}

.bulb-off {
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #2b6095;
  stroke-width: 0.5;
}

.device-offline {
  color: red;
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);
  text-decoration: line-through;
}

circle.heat-on {
  fill: rgb(244, 194, 168);
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-on {
  paint-order: fill;
  stroke-width: 4.62558px;
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);
  stroke-opacity: 0.34;
}

circle.heat-off {
  fill: rgb(168, 193, 244);
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-off {
  paint-order: fill;
  stroke-width: 4.62558px;
  fill: rgb(88, 77, 77);
  stroke: rgb(9, 96, 226);
  stroke-opacity: 0.34;
}

.lock {
  cursor: pointer;
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #d3e5e5;
  stroke-width: 1.2;
}

circle.asset-lock-circle {
  stroke: #d3e5e5;
  stroke-width: 1.2;
  fill: rgb(98, 117, 129);
  paint-order: stroke;
  fill-opacity: 0.59;
}

circle#nav-home-1 {
  stroke: #d3e5e5;
  stroke-width: 1.2;
  fill: rgb(98, 117, 129);
  paint-order: stroke;
  fill-opacity: 0.59;
}

text.txt-light {
  fill: rgb(224, 227, 243);
  font-family: Arial, sans-serif;
  font-size: 113.1px;
  fill-opacity: 0.8;
}

text.luminosity-text {
  fill: #d3e5e5;
  fill-opacity: 0.9;
}

.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter,
.slide-fade-leave-to {
  transform: translateX(10px);
  opacity: 0;
}

.swiper {
  width: 100%;
  height: 100%;
}

.swiper-slide {
  text-align: center;
  font-size: 18px;
  background: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
}

.swiper-slide img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>

