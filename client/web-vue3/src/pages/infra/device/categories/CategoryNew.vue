<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new category</div>
          <q-input v-model="category.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
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
import {DEVICE_CATEGORY_CREATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'DCategoryNew',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const category = ref({})
    const router = useRouter();

    const onSave = () => {
      if (category.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: DEVICE_CATEGORY_CREATE,
          variables: {deviceCategory: category.value},
        }).then(response => {
          router.push({path: `/admin/dcategories/${response.data.deviceCategoryCreate.id}/edit`})
        });
      }
    }

    return {
      category,
      onSave
    }
  }
});
</script>
