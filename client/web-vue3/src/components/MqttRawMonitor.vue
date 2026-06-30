<template>
  <q-card flat bordered class="mqtt-raw-monitor">
    <q-bar class="bg-grey-9 text-white">
      <q-icon name="mdi-console-line" size="18px" />
      <div class="text-caption">{{ title }}</div>
      <q-badge color="primary" :label="rows.length" />
      <q-space />
      <q-btn
        dense
        flat
        round
        size="sm"
        :icon="paused ? 'mdi-play' : 'mdi-pause'"
        :color="paused ? 'amber' : 'white'"
        @click="paused = !paused"
      >
        <q-tooltip>{{ paused ? 'Resume' : 'Pause' }}</q-tooltip>
      </q-btn>
      <q-btn dense flat round size="sm" icon="mdi-trash-can-outline" @click="clear">
        <q-tooltip>Clear</q-tooltip>
      </q-btn>
    </q-bar>

    <q-virtual-scroll
      :items="rows"
      :style="{ maxHeight: height }"
      class="raw-scroll bg-grey-10"
    >
      <template v-slot="{ item }">
        <div class="raw-row">
          <span class="raw-ts">{{ item.time }}</span>
          <span class="raw-topic">{{ item.topic }}</span>
          <span class="raw-eq">=</span>
          <span class="raw-payload">{{ item.payload }}</span>
        </div>
      </template>
    </q-virtual-scroll>

    <div v-if="rows.length === 0" class="text-grey-6 text-center q-pa-md">
      <q-icon name="mdi-radio-tower" size="sm" class="q-mr-xs" />
      Waiting for MQTT messages…
    </div>
  </q-card>
</template>

<script setup>
import { ref } from 'vue';
import { format } from 'date-fns';
import { useMqttStream } from '@/composables';

const props = defineProps({
  // Optional predicate to keep only matching messages (e.g. a single port).
  filter: { type: Function, default: null },
  // Cap the buffer to avoid unbounded memory growth.
  max: { type: Number, default: 500 },
  height: { type: String, default: '300px' },
  title: { type: String, default: 'MQTT raw monitor' },
});

const rows = ref([]);
const paused = ref(false);

const clear = () => {
  rows.value = [];
};

useMqttStream(
  (msg) => {
    if (paused.value) return;
    rows.value.unshift({
      time: format(new Date(msg.ts || Date.now()), 'HH:mm:ss'),
      topic: msg.topic,
      payload: msg.payload,
    });
    if (rows.value.length > props.max) {
      rows.value.length = props.max;
    }
  },
  { filter: props.filter }
);
</script>

<style scoped>
.mqtt-raw-monitor {
  overflow: hidden;
}

.raw-scroll {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.8rem;
}

.raw-row {
  padding: 2px 8px;
  white-space: nowrap;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: #d4d4d4;
}

.raw-ts {
  color: #7f9cc0;
  margin-right: 8px;
}

.raw-topic {
  color: #4ec9b0;
}

.raw-eq {
  color: #808080;
  margin: 0 6px;
}

.raw-payload {
  color: #ce9178;
  word-break: break-all;
}
</style>
