<template>
  <q-ajax-bar color="accent"/>
  <router-view v-slot="{ Component }" :key="$route.fullPath">
    <transition name="route" mode="out-in" appear>
      <component :is="Component"/>
    </transition>
  </router-view>
</template>
<script>
  import {defineComponent} from 'vue';
  import {mapActions} from 'vuex';

  export default defineComponent({
    name: 'App',
    methods: {
      ...mapActions(['connect']),
    },
    mounted() {
      this.connect();
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
