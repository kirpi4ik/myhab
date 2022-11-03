<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new peripheral</div>
          <q-input v-model="peripheral.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.model" label="Model" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-select v-model="peripheral.category"
                    :options="categoryList"
                    option-label="name"
                    label="Category" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="peripheral.category = null" class="cursor-pointer text-blue"/>
          </q-select>
        </q-card-section>
        <q-separator/>
        <q-card-actions>
          <q-btn flat color="secondary" type="submit">
            Save
          </q-btn>
          <q-btn flat color="secondary" :to="$route.matched[1]">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {useQuasar} from 'quasar'
import {defineComponent, ref} from 'vue';
import {PERIPHERAL_CATEGORIES, PERIPHERAL_CATEGORY_CREATE, PERIPHERAL_CREATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";
import _ from "lodash";

export default defineComponent({
  name: 'PeripheralNew',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const peripheral = ref({})
    const categoryList = ref([])
    const router = useRouter();
    client.query({
      query: PERIPHERAL_CATEGORIES,
      variables: {},
      fetchPolicy: 'network-only',
    }).then(response => {
      categoryList.value = response.data.peripheralCategoryList
    })
    const onSave = () => {
      if (peripheral.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: PERIPHERAL_CREATE,
          variables: {devicePeripheral: peripheral.value},
        }).then(response => {
          router.push({path: `/admin/peripherals/${response.data.devicePeripheralCreate.id}/edit`})
        });
      }
    }

    return {
      peripheral,
      categoryList,
      onSave
    }
  }
});
</script>
