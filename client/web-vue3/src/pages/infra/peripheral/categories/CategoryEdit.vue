<template>
  <q-page padding>

  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {PERIPHERAL_CATEGORY_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PCategoryEdit',
  setup() {
  }
});
</script>
