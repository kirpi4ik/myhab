<template>
  <q-drawer v-model="isSidebarOpen" show-if-above bordered class="bg-grey-9 text-white">
    <q-scroll-area style="height: calc(100% - 150px); margin-top: 50px; border-right: 1px solid #ddd">
      <q-list>
        <q-item to="/" active-class="q-item-no-link-highlighting">
          <q-item-section avatar>
            <q-icon name="dashboard"/>
          </q-item-section>
          <q-item-section>
            <q-item-label>Dashboard</q-item-label>
          </q-item-section>
        </q-item>
        <q-expansion-item icon="mdi-clipboard-edit-outline" label="Infrastructure" class="text-weight-bolder text-white">
          <q-list class="q-pl-lg text-weight-light">
            <q-item to="/users" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-account-details"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>Users</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/devices" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-devices"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>Devices</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/peripherals" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-light-switch"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>Peripherals</q-item-label>
              </q-item-section>
            </q-item>
            <q-item to="/cables" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="mdi-cable-data"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>Cables</q-item-label>
              </q-item-section>
            </q-item>
            <q-item-label header class="text-weight-bolder text-white">Settings</q-item-label>
            <q-item to="/maintenance" active-class="q-item-no-link-highlighting">
              <q-item-section avatar>
                <q-icon name="settings"/>
              </q-item-section>
              <q-item-section>
                <q-item-label>Maintenance</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-expansion-item>
      </q-list>
    </q-scroll-area>
    <q-img class="absolute-top bg-primary" src="~assets/layer-2.png" style="height: 50px">
      <div class="absolute-bottom dimmed text-h6">
        <div>{{ $t('project.name') }} v{{prjVersion}}</div>
      </div>
    </q-img>
  </q-drawer>
</template>
<script>
  import {defineComponent} from 'vue';
  import {useUiState} from '@/composables';

  export default defineComponent({
    name: 'SideBarLayout',
    setup() {
      const {isSidebarOpen} = useUiState();
      const prjVersion = process.env.PRJ_VERSION
      return {
        isSidebarOpen,
        prjVersion
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
