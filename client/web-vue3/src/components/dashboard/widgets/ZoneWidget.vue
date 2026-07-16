<template>
  <g
    :class="['screen-widget', 'zone-widget', stateClass, { selected: selected, actionable: actionable }]"
    @click.stop="onClick"
    @pointerdown.stop="$emit('widget-pointerdown', { widget, event: $event })"
  >
    <polygon :points="pointsAttr" />
    <template v-if="selected && mode === 'edit'">
      <!-- edge midpoints: click to insert a new vertex there -->
      <circle
        v-for="(m, i) in midpoints"
        :key="`m-${i}`"
        class="midpoint-handle"
        :cx="m[0]"
        :cy="m[1]"
        r="4"
        @pointerdown.stop="$emit('midpoint-pointerdown', { widget, edgeIndex: i, event: $event })"
      />
      <!-- vertices: drag to move, double-click to remove -->
      <circle
        v-for="(p, i) in widget.points"
        :key="`v-${i}`"
        class="vertex-handle"
        :cx="p[0]"
        :cy="p[1]"
        r="6"
        @pointerdown.stop="$emit('vertex-pointerdown', { widget, vertexIndex: i, event: $event })"
        @dblclick.stop="$emit('vertex-dblclick', { widget, vertexIndex: i })"
      />
    </template>
  </g>
</template>

<script setup>
import { computed, ref } from 'vue';
import { getWidgetClass, isActionableCategory } from '@/_helpers/screen-widget';

const props = defineProps({
  widget: { type: Object, required: true },
  peripheral: { type: Object, default: null },
  mode: { type: String, default: 'view' },
  selected: { type: Boolean, default: false },
});

const emit = defineEmits(['activate', 'widget-pointerdown', 'vertex-pointerdown', 'vertex-dblclick', 'midpoint-pointerdown']);

const flashing = ref(false);

const category = computed(() => props.peripheral?.category?.name);
const pointsAttr = computed(() => (props.widget.points || []).map((p) => `${p[0]},${p[1]}`).join(' '));

/** Midpoint of each polygon edge (edge i connects vertex i and i+1, wrapping). */
const midpoints = computed(() => {
  const pts = props.widget.points || [];
  return pts.map((p, i) => {
    const next = pts[(i + 1) % pts.length];
    return [(p[0] + next[0]) / 2, (p[1] + next[1]) / 2];
  });
});
const actionable = computed(() => props.mode === 'view' && isActionableCategory(category.value));

const stateClass = computed(() => {
  if (flashing.value) return 'focus';
  return getWidgetClass(category.value, props.peripheral?.state, props.peripheral?.deviceStatus);
});

const onClick = () => {
  if (props.mode !== 'view') return;
  if (actionable.value) {
    flashing.value = true;
    setTimeout(() => { flashing.value = false; }, 100);
  }
  emit('activate', props.widget);
};
</script>
