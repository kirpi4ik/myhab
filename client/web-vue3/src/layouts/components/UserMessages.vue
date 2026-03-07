<template>
  <div>
    <q-item v-if="loading" class="justify-center q-pa-md">
      <q-spinner-dots size="24px" color="primary"/>
    </q-item>
    <template v-else-if="messages.length > 0">
      <q-item
        style="max-width: 420px"
        v-for="msg in messages"
        :key="msg.id"
        clickable
        v-ripple
        @click="$router.push('/messages')"
      >
        <q-item-section avatar>
          <q-icon
            :name="levelIcon(msg.level)"
            :color="levelColor(msg.level)"
            size="sm"
          />
        </q-item-section>
        <q-item-section>
          <q-item-label :lines="1" class="text-weight-medium">{{ msg.subject }}</q-item-label>
          <q-item-label caption :lines="1">{{ msg.fromSender }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-item-label caption>{{ formatRelativeDate(msg.tsCreated) }}</q-item-label>
        </q-item-section>
      </q-item>
    </template>
    <q-item v-else>
      <q-item-section class="text-center text-grey-6 q-pa-sm">
        No new messages
      </q-item-section>
    </q-item>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { formatDistanceToNow, parseISO } from 'date-fns';
import { MY_MESSAGES } from '@/graphql/queries';

export default defineComponent({
  name: 'UserMessages',
  setup() {
    const { client } = useApolloClient();
    const messages = ref([]);
    const loading = ref(false);

    const fetchMessages = async () => {
      loading.value = true;
      try {
        const response = await client.query({
          query: MY_MESSAGES,
          variables: { state: 'NEW' },
          fetchPolicy: 'network-only'
        });
        const all = response.data.myMessages || [];
        messages.value = all.slice(0, 5);
      } catch {
        messages.value = [];
      } finally {
        loading.value = false;
      }
    };

    const levelIcon = (level) => {
      switch (level) {
        case 'ERROR': return 'mdi-alert-circle';
        case 'WARN': return 'mdi-alert';
        default: return 'mdi-information';
      }
    };

    const levelColor = (level) => {
      switch (level) {
        case 'ERROR': return 'negative';
        case 'WARN': return 'warning';
        default: return 'info';
      }
    };

    const formatRelativeDate = (dateStr) => {
      if (!dateStr) return '';
      try {
        return formatDistanceToNow(parseISO(dateStr), { addSuffix: true });
      } catch {
        return '';
      }
    };

    onMounted(() => {
      fetchMessages();
    });

    return {
      messages,
      loading,
      levelIcon,
      levelColor,
      formatRelativeDate
    };
  }
});
</script>

