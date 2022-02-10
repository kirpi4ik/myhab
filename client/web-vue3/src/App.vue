<template>
  <router-view/>
</template>
<script>
  import {defineComponent} from 'vue';
  import {mapActions} from 'vuex';

  export default defineComponent({
    name: 'App',
    methods: {
      ...mapActions('settings', ['getSavedSettings']),
      ...mapActions(["connect"]),
    },
    mounted() {
      this.connect();
      this.getSavedSettings();
    },
  });
</script>
