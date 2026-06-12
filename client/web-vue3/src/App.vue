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

      // Keep the app-config Pinia store in sync with edits made via the
      // /admin/appconfig page. The Mutation.appConfigUpdate fetcher publishes
      // EVT_APP_CFG_VALUE_CHANGED after a successful commit to the git-backed
      // ConfigProvider. We mirror the backend's `uiConfigList` allowlist so
      // we don't accept secret keys (mqtt.password etc.) into the SPA store.
      // Components that read via appConfig.getNumber(...) / appConfig.get(...)
      // reactively re-render on the next tick.
      const UI_SAFE_PREFIXES = ['specialDevices.', 'specialZones.', 'grafana.', 'surveillance.', 'ui.'];
      useWebSocketListener('evt_app_cfg_value_changed', (payload) => {
        const key = payload?.p4;
        if (typeof key === 'string' && UI_SAFE_PREFIXES.some(p => key.startsWith(p))) {
          appConfig.updateOne(key, payload.p5);
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
