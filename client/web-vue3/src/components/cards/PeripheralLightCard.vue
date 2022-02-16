<template>
  <q-card class="text-white" style="background: linear-gradient(#156cb8, #60a3c2)">
    <q-item>
      <q-item-section avatar>
        <q-avatar size="60px" :class="asset['state']? 'shadow-10 bg-blue' : 'shadow-10 bg-yellow-4'">
          <q-icon name="mdi-lightbulb-on" color="yellow-10" size="40px" v-if="!asset['state']"/>
          <q-icon name="mdi-lightbulb-off" color="white" size="40px" v-if="asset['state']"/>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label class="text-weight-medium text-h5">{{ asset.data.name }}</q-item-label>
      </q-item-section>

      <q-item-section side>
        <q-item-label>
          <q-btn size="sm" flat round icon="settings" class="text-white"/>
        </q-item-label>
        <q-item-label>
          <q-btn size="sm" flat round icon="mdi-view-list" class="text-white"/>
        </q-item-label>

      </q-item-section>
    </q-item>

    <q-separator></q-separator>
    <q-card-section>
      <div class="q-pa-sm text-grey-8">
        <toggle v-model="asset['state']"/>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
  import {defineComponent, toRefs} from 'vue';
  import Toggle from '@vueform/toggle'

  export default defineComponent({
    name: 'PeripheralLightCard',
    components: {
      Toggle
    },
    props: {
      peripheral: Object
    },
    setup(props) {
      const {peripheral: asset} = toRefs(props)
      return {asset};
    },
    methods: {},
  });
</script>
<style>
  .toggle, .toggle-container {
    width: 100% !important;
    height: 40px !important;
  }

  .toggle-handle {
    height: 40px !important;
    width: 40px !important;
  }

  .toggle-off {
    background-color: red !important;
  }

  .toggle-on .toggle-handle {
    background: linear-gradient(rgb(0, 191, 54), rgb(53, 113, 61)) !important;
    border: 1px solid #386668;
  }

  .toggle-off .toggle-handle {
    background: linear-gradient(rgb(191, 0, 0), rgb(255, 190, 98)) !important;
    border: 1px solid #386668;
  }
</style>
<style src="@vueform/toggle/themes/default.css"></style>
