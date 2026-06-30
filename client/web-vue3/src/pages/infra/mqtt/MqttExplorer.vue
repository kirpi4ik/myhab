<template>
  <q-page padding>
    <div class="row q-col-gutter-md">
      <!-- Topic tree -->
      <div class="col-12 col-md-8">
        <q-card flat bordered>
          <q-bar class="bg-primary text-white">
            <q-icon name="mdi-file-tree" />
            <div>MQTT Explorer</div>
            <q-badge color="white" text-color="primary" :label="topicCount + ' topics'" />
            <q-space />
            <q-chip
              dense
              size="sm"
              :color="connection === 'ONLINE' ? 'green-5' : 'red-5'"
              text-color="white"
              :icon="connection === 'ONLINE' ? 'mdi-lan-connect' : 'mdi-lan-disconnect'"
            >
              {{ connection }}
            </q-chip>
            <q-btn dense flat round icon="mdi-collapse-all" @click="setExpanded(false)">
              <q-tooltip>Collapse all</q-tooltip>
            </q-btn>
            <q-btn dense flat round icon="mdi-expand-all" @click="setExpanded(true)">
              <q-tooltip>Expand all</q-tooltip>
            </q-btn>
            <q-btn dense flat round icon="mdi-trash-can-outline" @click="resetTree">
              <q-tooltip>Reset tree</q-tooltip>
            </q-btn>
          </q-bar>

          <div class="tree-wrapper">
            <mqtt-tree-node
              v-for="child in sortedRoots"
              :key="child.path"
              :node="child"
              :depth="0"
              @select="onSelect"
              @toggle="onToggle"
            />
            <div v-if="topicCount === 0" class="text-grey-6 text-center q-pa-lg">
              <q-icon name="mdi-radio-tower" size="md" />
              <div>Waiting for MQTT messages…</div>
            </div>
          </div>
        </q-card>
      </div>

      <!-- Publish panel -->
      <div class="col-12 col-md-4">
        <q-card flat bordered>
          <q-bar class="bg-secondary text-white">
            <q-icon name="mdi-upload" />
            <div>Publish</div>
          </q-bar>
          <q-card-section class="q-gutter-sm">
            <q-input v-model="pubTopic" dense outlined label="Topic" />
            <q-input
              v-model="pubPayload"
              dense
              outlined
              type="textarea"
              autogrow
              label="Payload"
            />
            <q-btn
              color="secondary"
              icon="mdi-send"
              label="Publish"
              :loading="publishing"
              :disable="!pubTopic"
              @click="doPublish"
            />
          </q-card-section>
        </q-card>
      </div>

      <!-- Raw monitor -->
      <div class="col-12">
        <mqtt-raw-monitor title="MQTT raw monitor" height="320px" :max="500" />
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { useMqttStream } from '@/composables';
import { useWebSocketStore } from '@/store/websocket.store';
import { MQTT_PUBLISH } from '@/graphql/queries';
import MqttTreeNode from './MqttTreeNode.vue';
import MqttRawMonitor from 'components/MqttRawMonitor.vue';

const $q = useQuasar();
const { client } = useApolloClient();
const wsStore = useWebSocketStore();
const connection = computed(() => wsStore.connection);

// Reactive topic tree. `root.children` keyed by path segment; each node carries
// its last value, a highlight flag, and an expand flag.
const root = reactive({ children: {} });
const topicCount = ref(0);

const sortedRoots = computed(() =>
  Object.values(root.children).sort((a, b) => a.name.localeCompare(b.name))
);

const makeNode = (name, path) =>
  reactive({ name, path, children: {}, value: null, highlight: false, expanded: true, _t: null });

const upsert = (topic, payload) => {
  if (!topic) return;
  const segments = topic.split('/');
  let parent = root;
  let path = '';
  let node = null;
  for (const seg of segments) {
    path = path ? `${path}/${seg}` : seg;
    if (!parent.children[seg]) {
      parent.children[seg] = makeNode(seg, path);
      topicCount.value += 1;
    }
    node = parent.children[seg];
    parent = node;
  }
  // Leaf node receives the value + a 3s highlight (reset on each message).
  node.value = payload;
  node.highlight = true;
  if (node._t) clearTimeout(node._t);
  node._t = setTimeout(() => {
    node.highlight = false;
  }, 3000);
};

useMqttStream((msg) => upsert(msg.topic, msg.payload));

const setExpandedRecursive = (node, value) => {
  node.expanded = value;
  Object.values(node.children).forEach((c) => setExpandedRecursive(c, value));
};
const setExpanded = (value) =>
  Object.values(root.children).forEach((c) => setExpandedRecursive(c, value));

const onToggle = (node) => {
  node.expanded = !node.expanded;
};

const resetTree = () => {
  root.children = {};
  topicCount.value = 0;
};

// --- Publish ---
const pubTopic = ref('');
const pubPayload = ref('');
const publishing = ref(false);

const onSelect = (node) => {
  pubTopic.value = node.path;
  if (node.value !== null && node.value !== undefined) {
    pubPayload.value = String(node.value);
  }
};

const doPublish = async () => {
  if (!pubTopic.value || publishing.value) return;
  publishing.value = true;
  try {
    const { data } = await client.mutate({
      mutation: MQTT_PUBLISH,
      variables: { topic: pubTopic.value, payload: pubPayload.value || '' },
      fetchPolicy: 'no-cache',
    });
    const res = data?.publishMqtt;
    if (res?.success) {
      $q.notify({ color: 'positive', message: 'Published', icon: 'mdi-check-circle', position: 'top' });
    } else {
      $q.notify({
        color: 'negative',
        message: res?.error || 'Publish failed',
        icon: 'mdi-alert-circle',
        position: 'top',
      });
    }
  } catch (e) {
    $q.notify({ color: 'negative', message: 'Publish failed', icon: 'mdi-alert-circle', position: 'top' });
  } finally {
    publishing.value = false;
  }
};
</script>

<style scoped>
.tree-wrapper {
  max-height: 65vh;
  overflow: auto;
  padding: 6px 0;
}
</style>
