<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new category</div>
          <q-input v-model="category.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="category.title" label="Title" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
        </q-card-section>
        <q-separator/>
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
  </q-page>
</template>

<script>
import {useQuasar} from 'quasar'
import {defineComponent, ref} from 'vue';
import {PERIPHERAL_CATEGORY_CREATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PCategoryNew',
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
          mutation: PERIPHERAL_CATEGORY_CREATE,
          variables: {peripheralCategory: category.value},
        }).then(response => {
          router.push({path: `/admin/pcategories/${response.data.peripheralCategoryCreate.id}/edit`})
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
