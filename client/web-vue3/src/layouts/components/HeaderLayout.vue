<template>
  <q-header elevated class="bg-head">
    <q-toolbar>
      <q-btn flat dense round @click="toggleSideBar" icon="menu" aria-label="Menu" color="secondary"/>
      <q-separator vertical inset/>
      <q-toolbar-title></q-toolbar-title>
      <div class="q-gutter-sm row items-center no-wrap">
        <div>
          <clock-component v-if="$q.screen.gt.sm"/>
          <q-btn
            round
            dense
            flat
            color="secondary"
            icon="mdi-cctv"
            type="a"
            :href="result.config"
            v-if="result && result.config"
          ></q-btn>
          <q-icon name="mdi-wifi" class="float-right" color="positive" size="40px" v-if="wsConnection == 'ONLINE'"/>
          <q-icon name="mdi-wifi-off" class="float-right" color="negative" size="40px"
                  v-if="wsConnection == 'OFFLINE'"/>
        </div>
        <q-btn round dense flat color="secondary" icon="fas fa-mobile-alt" type="a" to="/wui"></q-btn>
        <q-btn
          round
          dense
          flat
          color="secondary"
          :icon="$q.fullscreen.isActive ? 'fullscreen_exit' : 'fullscreen'"
          @click="$q.fullscreen.toggle()"
          v-if="$q.screen.gt.sm"
        >
        </q-btn>
        <q-btn round dense flat color="secondary" icon="notifications">
          <q-badge v-if="unreadCount > 0" color="negative" text-color="white" floating>{{ unreadCount }}</q-badge>
          <q-menu>
            <q-list style="min-width: 100px">
              <user-messages></user-messages>
              <q-card class="text-center no-shadow no-border">
                <q-btn label="View All" style="max-width: 120px !important" flat dense class="text-indigo-8" @click="$router.push('/messages')"/>
              </q-card>
            </q-list>
          </q-menu>
        </q-btn>
        <q-btn round flat>
          <q-avatar size="42px">
            <img v-if="avatarBlobUrl" :src="avatarBlobUrl" alt="Avatar"/>
            <img v-else src="~assets/avatar.png" alt="Avatar"/>
          </q-avatar>
          <q-menu>
            <q-list style="min-width: 100px">
              <q-item style="max-width: 420px" clickable v-ripple @click="authzService.logout()">
                <q-item-section avatar>
                  <q-avatar>
                    <q-icon name="mdi-logout"/>
                  </q-avatar>
                </q-item-section>
                <q-item-section>
                  <q-item-label>Logout</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </div>
    </q-toolbar>
    <bread-crumb-layout/>
  </q-header>
</template>
<script>
import { computed, defineComponent, ref, onMounted, onUnmounted } from 'vue';

import { useQuery, useApolloClient } from '@vue/apollo-composable';
import { useWebSocketStore } from '@/store/websocket.store';

import { authzService, fetchAvatarBlobUrl } from '@/_services';
import { CONFIG_GLOBAL_GET_STRING_VAL, MY_UNREAD_COUNT } from '@/graphql/queries';
import { useUiState } from '@/composables';
import UserMessages from './UserMessages';

import BreadCrumbLayout from 'layouts/components/BreadCrumbLayout.vue';
import ClockComponent from 'components/ClockComponent';

export default defineComponent({
  name: 'HeaderLayout',
  components: {
    UserMessages,
    ClockComponent,
    BreadCrumbLayout,
  },
  setup() {
    const wsStore = useWebSocketStore();
    const { toggleSideBar } = useUiState();
    const { result } = useQuery(CONFIG_GLOBAL_GET_STRING_VAL, { key: 'surveillance.url' });
    const { client } = useApolloClient();

    const avatarBlobUrl = ref(null);
    const unreadCount = ref(0);

    const wsConnection = computed(() => {
      return wsStore.ws.connection || 'OFFLINE';
    });

    const fetchUnreadCount = async () => {
      try {
        const response = await client.query({
          query: MY_UNREAD_COUNT,
          fetchPolicy: 'network-only'
        });
        unreadCount.value = response.data.myUnreadCount || 0;
      } catch {
        unreadCount.value = 0;
      }
    };

    onMounted(async () => {
      const userId = authzService.currentUserValue?.id;
      if (userId) {
        const url = await fetchAvatarBlobUrl(userId);
        avatarBlobUrl.value = url;
      }
      fetchUnreadCount();
    });

    onUnmounted(() => {
      if (avatarBlobUrl.value) {
        URL.revokeObjectURL(avatarBlobUrl.value);
        avatarBlobUrl.value = null;
      }
    });

    return {
      toggleSideBar,
      result,
      authzService,
      wsConnection,
      avatarBlobUrl,
      unreadCount,
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
    },
    onResize() {
    },
  },
});

</script>
<style lang="scss">
div.q-toolbar {
  background-color: $header;
}
</style>
