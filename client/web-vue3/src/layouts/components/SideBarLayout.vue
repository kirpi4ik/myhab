<template>
  <q-drawer v-model="isSidebarOpen" :mini="!isSidebarOpen || miniState" @click.capture="drawerClick" show-if-above
            bordered persistent class="bg-grey-9 text-white">
    <template v-slot:mini>
      <q-scroll-area class="fit mini-slot cursor-pointer">
        <div class="q-py-lg">
          <div class="column items-center">
            <q-icon name="inbox" color="blue" class="mini-icon"/>
            <q-icon name="star" color="orange" class="mini-icon"/>
            <q-icon name="send" color="purple" class="mini-icon"/>
            <q-icon name="drafts" color="teal" class="mini-icon"/>
          </div>
        </div>
      </q-scroll-area>
    </template>
    <q-scroll-area style="height: calc(100% - 150px); margin-top: 50px; border-right: 1px solid #ddd">
      <q-list>
        <q-item to="/" active-class="q-item-no-link-highlighting">
          <q-item-section avatar>
            <q-icon name="dashboard"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>{{ $t('navigation.dashboard') }}</q-item-label>
          </q-item-section>
        </q-item>
        <q-expansion-item
          icon="mdi-clipboard-edit-outline"
          :label="$t('navigation.infrastructure')"
          class="text-weight-bolder text-white"
        >
          <q-list class="q-pl-lg text-weight-light">
            <q-item to="/admin/users" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-account-details"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.users') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/admin/devices" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-devices"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.devices') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/admin/peripherals" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-light-switch"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.peripherals') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/admin/cables" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-cable-data"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.cables') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item :href="graphiqlUrl" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-cable-data"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.graphiql') }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item-label header class="text-weight-bolder text-white">{{ $t('navigation.settings') }}</q-item-label>
            <q-item to="/maintenance" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="settings"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>{{ $t('navigation.maintenance') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-expansion-item>
      </q-list>
    </q-scroll-area>
    <q-img class="absolute-top bg-primary" src="~assets/layer-2.png" style="height: 50px">
      <div class="absolute-bottom dimmed text-h6">
        <div>{{ $t('project.name') }} v{{ prjVersion }}</div>
      </div>
    </q-img>
    <div class="q-mini-drawer-hide absolute" style="top: 150px; right: -17px">
      <q-btn
        dense
        round
        unelevated
        color="grey-8"
        icon="chevron_left"
        @click="drawerClick2"
      />
    </div>
  </q-drawer>
</template>
<script>
import {defineComponent, ref} from 'vue';
import {useUiState} from '@/composables';

export default defineComponent({
  name: 'SideBarLayout',
  setup() {
    const miniState = ref(true)
    const {isSidebarOpen} = useUiState();
    const prjVersion = process.env.PRJ_VERSION;
    const graphiqlUrl = process.env.BCK_SERVER_URL + '/graphql/browser';

    return {
      miniState,
      isSidebarOpen,
      prjVersion,
      graphiqlUrl,
      drawerClick(e) {
        if (miniState.value) {
          miniState.value = false
          e.stopPropagation()
        }
      },
      drawerClick2(e) {
        if (miniState.value) {
          miniState.value = false
          e.stopPropagation()
        }else {
          miniState.value = true
        }
      }
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
<style lang="sass" scoped>
.mini-slot
  transition: background-color .28s

  &:hover
    background-color: rgba(0, 0, 0, .04)

  .mini-icon
    font-size: 1.718em

    & + &
      margin-top: 18px
</style>
