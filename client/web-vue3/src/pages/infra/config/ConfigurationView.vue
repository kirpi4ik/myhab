<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Configurations</div>
          <q-btn icon="add" color="positive" :disable="loading" label="Add configuration" @click="addConfig = true"/>

          <q-card class="my-card" bordered v-for="cfg in cfgList" v-bind:key="cfg.key">
            <q-card-section horizontal>
              <q-card-section class="full-width">
                <q-input v-model="cfg.key" label="Key" clearable clear-icon="close" color="orange"/>
              </q-card-section>
              <q-card-section class="full-width">
                <q-input v-model="cfg.value" label="Value" clearable clear-icon="close" color="orange"/>
              </q-card-section>
              <q-btn color="red" icon="mdi-playlist-remove" @click="cfgToRemove=cfg; confirmDelete=true;"/>
            </q-card-section>
          </q-card>
        </q-card-section>
        <q-card-actions>
          <q-btn color="accent" type="submit">
            Save
          </q-btn>
          <q-btn color="info" @click="$router.go(-1)">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
    <q-dialog v-model="addConfig" transition-show="slide-up" transition-hide="slide-down" :maximized="maximizedToggle">
      <q-card>
        <q-bar>
          <q-space/>

          <q-btn dense flat icon="minimize" @click="maximizedToggle = false" :disable="!maximizedToggle">
            <q-tooltip v-if="maximizedToggle" class="bg-white text-primary">Minimize</q-tooltip>
          </q-btn>
          <q-btn dense flat icon="crop_square" @click="maximizedToggle = true" :disable="maximizedToggle">
            <q-tooltip v-if="!maximizedToggle" class="bg-white text-primary">Maximize</q-tooltip>
          </q-btn>
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip class="bg-white text-primary">Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-card-section class="full-width">
          <q-card-section horizontal>
            <q-card-section class="full-width">
              <q-select
                filled
                :model-value="newCfgKey"
                use-input
                hide-selected
                fill-input
                input-debounce="0"
                :options="keyOptionsFiltered"
                @filter="filterFn"
                @input-value="setNewKfgKey"
                hint="Configuration key"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      No results
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </q-card-section>
            <q-card-section class="full-width">
              <q-input v-model="newCfgValue" label="Value" clearable clear-icon="close" color="orange"/>
            </q-card-section>
          </q-card-section>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Add" color="red" v-close-popup @click="onAdd();addConfig=false"/>
          <q-btn flat label="Cancel" color="positive" v-close-popup/>
        </q-card-actions>
      </q-card>
    </q-dialog>
    <q-dialog v-model="confirmDelete" transition-show="jump-up" transition-hide="jump-down">
      <q-card>
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="lock"/>
          <div>Warning</div>
          <q-space/>
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip class="bg-primary">Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-card-section class="row items-center">
          <q-avatar icon="delete" color="red" text-color="white"/>
          <span class="q-ml-sm">Do you want to remove the user  <b class="text-h6 text-red">{{
              cfgToRemove.key
            }}</b> ?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Delete" color="red" v-close-popup
                 @click="onDelete();confirmDelete=false"/>
          <q-btn flat label="Cancel" color="positive" v-close-popup/>
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>
<script>
import {defineComponent, onMounted, ref} from "vue";
import {
  CONFIGURATION_ADDLIST_CONFIG_VALUE,
  CONFIGURATION_KEY_LIST,
  CONFIGURATION_LIST,
  CONFIGURATION_REMOVE_CONFIG
} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router";

export default defineComponent({
  name: 'ConfigurationView',
  setup() {
    const {client} = useApolloClient();
    const cfgList = ref([])
    const route = useRoute();
    const loading = ref(false)
    const confirmDelete = ref(false)
    const addConfig = ref(false)
    const cfgToRemove = ref(null)
    const newCfgKey = ref(null)
    const newCfgValue = ref(null)
    const keyOptions = ref([])
    const keyOptionsFiltered = ref(keyOptions)

    const fetchData = () => {
      client.query({
        query: CONFIGURATION_LIST,
        variables: {entityType: route.query.type.toUpperCase(), entityId: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        cfgList.value = [...response.data.configurationListByEntity]
      })
      client.query({
        query: CONFIGURATION_KEY_LIST,
        variables: {entityType: route.query.type.toUpperCase()},
        fetchPolicy: 'network-only',
      }).then(response => {
        keyOptions.value = [...response.data.configKeysByEntity]
      })
    }
    const filterFn = (val, update, abort) => {
      update(() => {
        const needle = val.toLocaleLowerCase()
        keyOptionsFiltered.value = keyOptions.value.filter(v => v.toLocaleLowerCase().indexOf(needle) > -1)
      })
    }
    const setNewKfgKey = (val) => {
      newCfgKey.value = val
    }
    const onAdd = () => {
      client.mutate({
        mutation: CONFIGURATION_ADDLIST_CONFIG_VALUE,
        variables: {
          key: newCfgKey.value,
          entityId: route.params.idPrimary,
          entityType: route.query.type.toUpperCase(),
          value: newCfgValue.value
        },
        fetchPolicy: 'network-only',
      }).then(response => {
        newCfgKey.value = null
        newCfgValue.value = null
        fetchData()
      })
    }
    const onDelete = () => {
      client.mutate({
        mutation: CONFIGURATION_REMOVE_CONFIG,
        variables: {
          id: cfgToRemove.value.id
        },
        fetchPolicy: 'network-only',
      }).then(response => {
        fetchData()
      })
    }
    onMounted(() => {
      fetchData()
    })
    return {
      loading,
      cfgList,
      confirmDelete,
      cfgToRemove,
      newCfgKey,
      newCfgValue,
      addConfig,
      onAdd,
      onDelete,
      keyOptionsFiltered,
      filterFn,
      maximizedToggle: ref(true),
      setNewKfgKey
    }
  }
})
</script>
