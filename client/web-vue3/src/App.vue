<template>
  <transition
    appear
    enter-active-class="animated fadeIn"
    leave-active-class="animated fadeOut"
    mode="out-in"
    :duration="200">
    <router-view :key="$route.fullPath"/>
  </transition>
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
