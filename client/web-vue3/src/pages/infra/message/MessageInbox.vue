<template>
  <q-page padding>
    <q-card flat bordered class="message-inbox">
      <!-- Header -->
      <q-card-section class="q-pb-none">
        <div class="row items-center q-gutter-sm">
          <q-icon name="mdi-email-multiple" size="sm" color="primary"/>
          <div class="text-h5 text-primary">Messages</div>
          <q-space/>
          <q-btn-toggle
            v-model="stateFilter"
            no-caps
            rounded
            toggle-color="primary"
            :options="stateOptions"
            class="q-mr-sm"
          />
          <q-select
            v-model="levelFilter"
            :options="levelOptions"
            label="Level"
            dense
            outlined
            clearable
            emit-value
            map-options
            style="min-width: 130px"
            class="q-mr-sm"
          />
          <q-input
            v-model="searchText"
            dense
            outlined
            debounce="300"
            placeholder="Search..."
            clearable
            style="min-width: 200px"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-magnify"/>
            </template>
          </q-input>
        </div>
      </q-card-section>

      <q-separator class="q-mt-sm"/>

      <!-- Two-panel layout -->
      <div class="row" style="min-height: calc(100vh - 220px)">
        <!-- Left panel: message list -->
        <div :class="leftPanelClass" class="message-list-panel">
          <q-scroll-area style="height: calc(100vh - 270px)">
            <q-list separator>
              <q-item
                v-for="msg in filteredMessages"
                :key="msg.id"
                clickable
                v-ripple
                :active="selectedMessage?.id === msg.id"
                active-class="bg-blue-1"
                @click="selectMessage(msg)"
                :class="{ 'text-weight-bold': msg.state === 'NEW' }"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="levelIcon(msg.level)"
                    :color="levelColor(msg.level)"
                    size="sm"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label :lines="1">{{ msg.subject }}</q-item-label>
                  <q-item-label caption :lines="1">
                    {{ msg.fromSender }}
                  </q-item-label>
                </q-item-section>
                <q-item-section side top>
                  <q-item-label caption>{{ formatRelativeDate(msg.tsCreated) }}</q-item-label>
                  <q-badge
                    v-if="msg.state === 'NEW'"
                    color="blue"
                    label="NEW"
                    class="q-mt-xs"
                  />
                  <q-badge
                    v-else-if="msg.state === 'ARCHIVE'"
                    color="grey"
                    label="ARCHIVE"
                    class="q-mt-xs"
                  />
                </q-item-section>
              </q-item>
              <q-item v-if="filteredMessages.length === 0 && !loading">
                <q-item-section class="text-center text-grey-6 q-pa-lg">
                  <q-icon name="mdi-email-open-outline" size="48px" class="q-mb-sm"/>
                  <div>No messages found</div>
                </q-item-section>
              </q-item>
            </q-list>
          </q-scroll-area>
        </div>

        <!-- Right panel: message detail -->
        <div :class="rightPanelClass" class="message-detail-panel" v-if="showDetailPanel">
          <div v-if="selectedMessage" class="q-pa-md">
            <!-- Detail header -->
            <div class="row items-center q-mb-md">
              <q-btn
                v-if="$q.screen.lt.md"
                flat
                round
                icon="mdi-arrow-left"
                @click="selectedMessage = null"
                class="q-mr-sm"
              />
              <div class="col">
                <div class="text-h6">{{ selectedMessage.subject }}</div>
                <div class="text-caption text-grey-7">
                  From: <strong>{{ selectedMessage.fromSender }}</strong>
                  &middot; {{ formatDate(selectedMessage.tsCreated) }}
                </div>
              </div>
              <q-badge
                :color="levelColor(selectedMessage.level)"
                :label="selectedMessage.level"
                class="q-mr-sm"
              />
              <q-badge
                :color="stateColor(selectedMessage.state)"
                :label="selectedMessage.state"
              />
            </div>

            <q-separator class="q-mb-md"/>

            <!-- Message body -->
            <div class="message-body q-mb-lg" style="white-space: pre-wrap; line-height: 1.6;">{{ selectedMessage.message }}</div>

            <q-separator class="q-mb-md"/>

            <!-- Actions -->
            <div class="row q-gutter-sm">
              <q-btn
                v-if="selectedMessage.state !== 'NEW'"
                flat
                no-caps
                color="blue"
                icon="mdi-email-mark-as-unread"
                label="Mark as New"
                @click="updateState(selectedMessage, 'NEW')"
              />
              <q-btn
                v-if="selectedMessage.state !== 'READ'"
                flat
                no-caps
                color="positive"
                icon="mdi-email-open"
                label="Mark as Read"
                @click="updateState(selectedMessage, 'READ')"
              />
              <q-btn
                v-if="selectedMessage.state !== 'ARCHIVE'"
                flat
                no-caps
                color="grey"
                icon="mdi-archive"
                label="Archive"
                @click="updateState(selectedMessage, 'ARCHIVE')"
              />
            </div>
          </div>
          <div v-else class="flex flex-center" style="height: 100%">
            <div class="text-center text-grey-5">
              <q-icon name="mdi-email-outline" size="64px" class="q-mb-md"/>
              <div class="text-subtitle1">Select a message to read</div>
            </div>
          </div>
        </div>
      </div>

      <q-inner-loading :showing="loading">
        <q-spinner-dots size="40px" color="primary"/>
      </q-inner-loading>
    </q-card>
  </q-page>
</template>

<script>
import { computed, defineComponent, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { formatDistanceToNow, format, parseISO } from 'date-fns';
import {
  MY_MESSAGES,
  MESSAGE_UPDATE_STATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'MessageInbox',
  setup() {
    const $q = useQuasar();
    const route = useRoute();
    const { client } = useApolloClient();

    const messages = ref([]);
    const loading = ref(false);
    const selectedMessage = ref(null);
    const stateFilter = ref(null);
    const levelFilter = ref(null);
    const searchText = ref('');

    const stateOptions = [
      { label: 'All', value: null },
      { label: 'New', value: 'NEW' },
      { label: 'Read', value: 'READ' },
      { label: 'Archive', value: 'ARCHIVE' }
    ];

    const levelOptions = [
      { label: 'Info', value: 'INFO' },
      { label: 'Warning', value: 'WARN' },
      { label: 'Error', value: 'ERROR' }
    ];

    const fetchMessages = async () => {
      loading.value = true;
      try {
        const variables = {};
        if (levelFilter.value) variables.level = levelFilter.value;
        const response = await client.query({
          query: MY_MESSAGES,
          variables,
          fetchPolicy: 'network-only'
        });
        // Deep-copy to avoid Apollo cache frozen objects (prevents "read only property" on state updates)
        const raw = response.data.myMessages || [];
        messages.value = raw.map(m => structuredClone(m));
      } catch (e) {
        console.error('Failed to load messages', e);
        $q.notify({ color: 'negative', message: 'Failed to load messages', icon: 'mdi-alert-circle', position: 'top' });
      } finally {
        loading.value = false;
      }
    };

    const filteredMessages = computed(() => {
      let list = messages.value;
      if (stateFilter.value) {
        list = list.filter(m => m.state === stateFilter.value);
      }
      if (searchText.value) {
        const term = searchText.value.toLowerCase();
        list = list.filter(m =>
          m.subject?.toLowerCase().includes(term) ||
          m.fromSender?.toLowerCase().includes(term) ||
          m.message?.toLowerCase().includes(term)
        );
      }
      return list;
    });

    const selectMessage = async (msg) => {
      selectedMessage.value = msg;
      if (msg.state === 'NEW') {
        await updateState(msg, 'READ');
      }
    };

    const updateState = async (msg, newState) => {
      try {
        const response = await client.mutate({
          mutation: MESSAGE_UPDATE_STATE,
          variables: { id: msg.id, state: newState },
          fetchPolicy: 'no-cache'
        });
        if (response.data.messageUpdateState?.success) {
          const id = msg.id;
          const list = messages.value.map(m =>
            String(m.id) === String(id) ? { ...m, state: newState } : m
          );
          messages.value = list;
          if (selectedMessage.value && String(selectedMessage.value.id) === String(id)) {
            selectedMessage.value = list.find(m => String(m.id) === String(id)) ?? selectedMessage.value;
          }
        } else {
          $q.notify({ color: 'negative', message: response.data.messageUpdateState?.error || 'Update failed', position: 'top' });
        }
      } catch (e) {
        console.error('Failed to update message state', e);
        $q.notify({ color: 'negative', message: 'Failed to update message state', icon: 'mdi-alert-circle', position: 'top' });
      }
    };

    const levelIcon = (level) => {
      switch (level) {
        case 'ERROR': return 'mdi-alert-circle';
        case 'WARN': return 'mdi-alert';
        case 'INFO':
        default: return 'mdi-information';
      }
    };

    const levelColor = (level) => {
      switch (level) {
        case 'ERROR': return 'negative';
        case 'WARN': return 'warning';
        case 'INFO':
        default: return 'info';
      }
    };

    const stateColor = (state) => {
      switch (state) {
        case 'NEW': return 'blue';
        case 'READ': return 'positive';
        case 'ARCHIVE': return 'grey';
        default: return 'grey';
      }
    };

    const formatRelativeDate = (dateStr) => {
      if (!dateStr) return '';
      try {
        return formatDistanceToNow(parseISO(dateStr), { addSuffix: true });
      } catch {
        return dateStr;
      }
    };

    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      try {
        return format(parseISO(dateStr), 'MMM d, yyyy HH:mm');
      } catch {
        return dateStr;
      }
    };

    const leftPanelClass = computed(() => {
      if ($q.screen.lt.md) {
        return selectedMessage.value ? 'hidden' : 'col-12';
      }
      return 'col-5 col-lg-4';
    });

    const rightPanelClass = computed(() => {
      if ($q.screen.lt.md) {
        return 'col-12';
      }
      return 'col-7 col-lg-8';
    });

    const showDetailPanel = computed(() => {
      if ($q.screen.lt.md) {
        return !!selectedMessage.value;
      }
      return true;
    });

    const applyQuerySelection = async () => {
      const idFromQuery = route.query.id;
      if (!idFromQuery) {
        selectedMessage.value = null;
        return;
      }
      if (messages.value.length === 0) return;
      const msg = messages.value.find(m => String(m.id) === String(idFromQuery));
      if (msg) {
        await selectMessage(msg);
      }
    };

    watch(() => route.query.id, () => applyQuerySelection());

    onMounted(async () => {
      await fetchMessages();
      await applyQuerySelection();
    });

    return {
      messages,
      loading,
      selectedMessage,
      stateFilter,
      levelFilter,
      searchText,
      stateOptions,
      levelOptions,
      filteredMessages,
      selectMessage,
      updateState,
      levelIcon,
      levelColor,
      stateColor,
      formatRelativeDate,
      formatDate,
      leftPanelClass,
      rightPanelClass,
      showDetailPanel,
      fetchMessages
    };
  }
});
</script>

<style lang="scss" scoped>
.message-inbox {
  overflow: hidden;
}

.message-list-panel {
  border-right: 1px solid rgba(0, 0, 0, 0.12);
}

.message-body {
  font-size: 0.95rem;
}
</style>
