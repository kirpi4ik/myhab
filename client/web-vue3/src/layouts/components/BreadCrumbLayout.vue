<template>
  <q-separator class="bg-blue-grey-2"/>
  <q-breadcrumbs class="text-blue-grey bg-blue-grey-1" style="padding-top: 2px; padding-left: 5px" v-if="breadcrumb && breadcrumb.navigation">
    <template v-slot:separator>
      <q-icon size="1.5em" name="chevron_right" color="primary"/>
    </template>
    <q-breadcrumbs-el :label="$t('navigation.home')" icon="home" to="/"/>
    <q-breadcrumbs-el :label="link.name" icon="widgets" :to="'/zones/'+link.zoneId+'?category='+route.query.category" v-for="link in breadcrumb.navigation.breadcrumb" v-bind:key="link.zoneId"/>
  </q-breadcrumbs>
</template>
<script>
  import {defineComponent, ref} from 'vue';
  import {useQuery} from "@vue/apollo-composable";
  import {NAV_BREADCRUMB} from "@/graphql/queries";
  import {useRoute} from "vue-router";

  export default defineComponent({
    name: 'BreadCrumbLayout',
    components: {},
    setup() {
      const route = useRoute();

      const {result: breadcrumb} = useQuery(NAV_BREADCRUMB, {zoneId: route.params.zoneId}, {
        fetchPolicy: "network-only",   // Used for first execution
        nextFetchPolicy: "cache-and-network" // Used for subsequent executions
      })
      console.log(breadcrumb)
      return {route, breadcrumb}
    },
    watch: {
      $route() {
        this.paths = this.$route.meta.breadcrumb;
      },
    },
  });
</script>
