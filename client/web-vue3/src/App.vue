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



export default defineComponent({
    name: 'App',
    setup() {
      const wsStore = useWebSocketStore();
      
      onMounted(() => {
        console.log('App mounted, connecting WebSocket...');
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
