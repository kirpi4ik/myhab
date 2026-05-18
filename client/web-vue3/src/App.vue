<template>
  <q-ajax-bar color="accent"/>
  <router-view v-slot="{ Component }" :key="$route.fullPath">
    <transition name="route" mode="out-in" appear>
      <component :is="Component"/>
    </transition>
  </router-view>
</template>
<script>
import {defineComponent, onMounted} from 'vue';

import {useWebSocketStore} from '@/store/websocket.store';
import {useAppConfigStore} from 'src/store/app-config.store';
import {useWebSocketListener} from '@/composables';



export default defineComponent({
    name: 'App',
    setup() {
      const wsStore = useWebSocketStore();
      const appConfig = useAppConfigStore();

      // Keep the app-config Pinia store in sync with backend Configuration
      // edits. ConfigurationService publishes EVT_CFG_VALUE_CHANGED whenever
      // a row is updated (including BootStrap's upsert on restart and any
      // ConfigurationView UI edit). Filter to ui.* keys under
      // entityType=CONFIG, entityId=0 — that's the namespace served by
      // uiConfigList. Components that read via appConfig.getNumber(...) /
      // appConfig.get(...) reactively re-render on the next tick.
      useWebSocketListener('evt_cfg_value_changed', (payload) => {
        if (payload?.p2 === 'CONFIG' && String(payload?.p3) === '0'
            && typeof payload?.p4 === 'string' && payload.p4.startsWith('ui.')) {
          appConfig.updateOne(payload.p4, payload.p5);
        }
      });

      onMounted(() => {
        wsStore.connect();
      });

      return {};
    },
  });

</script>
<style>
  .route-enter-from {
    opacity: 0;
  }

  .route-enter-active,
  .route-leave-active {
    transition: all .5s ease-out
  }

  .route-leave-to {
    opacity: 0;
  }
</style>
