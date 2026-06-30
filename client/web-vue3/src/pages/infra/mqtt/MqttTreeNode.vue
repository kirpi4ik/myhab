<template>
  <div class="tree-node">
    <div
      class="node-row"
      :class="{ highlight: node.highlight }"
      :style="{ paddingLeft: depth * 16 + 8 + 'px' }"
      @click="onSelect"
    >
      <q-icon
        v-if="hasChildren"
        :name="node.expanded ? 'mdi-menu-down' : 'mdi-menu-right'"
        size="20px"
        class="caret"
        @click.stop="$emit('toggle', node)"
      />
      <span v-else class="caret-spacer" />

      <q-icon
        :name="hasChildren ? 'mdi-folder-outline' : 'mdi-leaf'"
        size="16px"
        class="q-mr-xs"
        :color="hasChildren ? 'blue-grey-5' : 'teal-5'"
      />
      <span class="node-name">{{ node.name }}</span>

      <span v-if="node.value !== null && node.value !== undefined" class="node-value">
        {{ node.value }}
      </span>
      <span v-if="childCount" class="node-count">({{ childCount }})</span>
    </div>

    <div v-if="hasChildren && node.expanded">
      <mqtt-tree-node
        v-for="child in sortedChildren"
        :key="child.path"
        :node="child"
        :depth="depth + 1"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
});

const emit = defineEmits(['select', 'toggle']);

const childCount = computed(() => Object.keys(props.node.children || {}).length);
const hasChildren = computed(() => childCount.value > 0);
const sortedChildren = computed(() =>
  Object.values(props.node.children || {}).sort((a, b) => a.name.localeCompare(b.name))
);

const onSelect = () => emit('select', props.node);
</script>

<style scoped>
.node-row {
  display: flex;
  align-items: center;
  padding: 3px 8px;
  cursor: pointer;
  border-radius: 4px;
  /* fade back to normal when the 3s highlight is cleared */
  transition: background-color 0.8s ease;
  font-size: 0.85rem;
}

.node-row:hover {
  background-color: rgba(33, 150, 243, 0.08);
}

/* Lit while a value is being received; the page clears it 3s after the last
   message, then the transition above fades it out. */
.node-row.highlight {
  background-color: #fff59d;
  transition: background-color 0.05s ease;
}

.caret {
  cursor: pointer;
}

.caret-spacer {
  display: inline-block;
  width: 20px;
}

.node-name {
  font-weight: 500;
  color: #37474f;
}

.node-value {
  margin-left: 10px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.8rem;
  color: #00695c;
  background: #e0f2f1;
  padding: 0 6px;
  border-radius: 3px;
  max-width: 480px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-count {
  margin-left: 8px;
  font-size: 0.72rem;
  color: #90a4ae;
}
</style>
