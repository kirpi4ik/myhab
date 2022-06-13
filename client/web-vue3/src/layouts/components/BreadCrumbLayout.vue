<template>
  <q-separator class="bg-blue-grey-2"/>
  <q-breadcrumbs
    class="text-blue-grey bg-blue-grey-1"
    style="padding-top: 2px; padding-left: 5px"
    v-if="breadcrumb.srvNav"
  >
    <template v-slot:separator>
      <q-icon size="1.5em" name="chevron_right" color="text-blue-grey"/>
    </template>
    <q-breadcrumbs-el :label="$t('navigation.home')" icon="home" to="/"/>
    <q-breadcrumbs-el v-for="link in breadcrumb.srvNav"
                      :label="link.name"
                      :to="'/zones/' + link.id + '?category=' + route.query.category"
                      v-bind:key="link.zoneId"
    />

  </q-breadcrumbs>
  <q-breadcrumbs
    class="text-blue-grey bg-blue-grey-1"
    style="padding-top: 2px; padding-left: 5px"
    v-else-if="breadcrumb.nativeNav !=null"
  >
    <template v-slot:separator>
      <q-icon size="1.5em" name="chevron_right" color="text-blue-grey"/>
    </template>
    <q-breadcrumbs-el :label="$t('navigation.home')" icon="home" to="/"/>
    <q-breadcrumbs-el v-for="link in breadcrumb.nativeNav"
                      :label="link.name"
                      :to="link.url"
                      v-bind:key="link.name"
    />
  </q-breadcrumbs>
</template>
<script>
import {defineComponent, ref} from 'vue';
import {useApolloClient, useQuery, useQueryLoading} from '@vue/apollo-composable';
import {NAV_BREADCRUMB, USERS_GET_ALL} from '@/graphql/queries';
import {useRoute} from 'vue-router';
import _ from "lodash";

export default defineComponent({
  name: 'BreadCrumbLayout',
  components: {},
  setup() {
    const {client} = useApolloClient();
    const route = useRoute();
    const breadcrumb = ref({});

    if (route.meta.navigation) {
      let type = route.meta.navigation.type;
      let id = route.params[route.meta.navigation.id];
      client.query({
        query: NAV_BREADCRUMB,
        variables: {id: id, type: type},
        fetchPolicy: 'network-only',
      }).then(response => {
        breadcrumb.value['srvNav'] = response.data.navigation.breadcrumb
      });
    } else {
      //native
      breadcrumb.value['nativeNav'] = _.transform(route.matched, (result, value, key) => {

        if (key > 0) {
          let url = value.path;
          if (key > route.matched.length - 2) {
            url = '';
          }
          result.push({
            name: value.meta.name,
            url: url
          })
        }
      })
    }
    return {route, breadcrumb};

  },
  watch: {
    $route() {
      this.paths = this.$route.meta.breadcrumb;
    },
  },
});
</script>
