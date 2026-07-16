<template>
  <g
    :class="['screen-widget', 'text-widget', { selected: selected }]"
    @click.stop
    @pointerdown.stop="$emit('widget-pointerdown', { widget, event: $event })"
  >
    <!-- padded hit/selection box: bare <text> glyphs are near-impossible to
         press, and the box also shows the selection outline -->
    <rect
      v-if="mode === 'edit' && hitBox"
      class="text-hit"
      :x="hitBox.x"
      :y="hitBox.y"
      :width="hitBox.width"
      :height="hitBox.height"
    />
    <text
      ref="textEl"
      :x="widget.x"
      :y="widget.y"
      :font-size="widget.fontSize || 20"
      :class="stateClass"
      text-anchor="middle"
    >{{ displayValue }}</text>
  </g>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { getWidgetClass, formatWidgetValue } from '@/_helpers/screen-widget';

const props = defineProps({
  widget: { type: Object, required: true },
  peripheral: { type: Object, default: null },
  mode: { type: String, default: 'view' },
  selected: { type: Boolean, default: false },
});

defineEmits(['widget-pointerdown']);

const category = computed(() => props.peripheral?.category?.name);

const stateClass = computed(() => {
  const cls = getWidgetClass(category.value, props.peripheral?.state, props.peripheral?.deviceStatus);
  return cls || 'luminosity-text';
});

const displayValue = computed(() => {
  const value = formatWidgetValue(category.value, props.peripheral?.portValue);
  // In the editor a peripheral may have no live value yet — show a placeholder
  // so the widget stays visible and selectable.
  return value || (props.mode === 'edit' ? '—' : '');
});

// --- edit-mode hit box (measured from the rendered glyphs, padded) ---
const textEl = ref(null);
const hitBox = ref(null);

const measureHitBox = () => {
  if (props.mode !== 'edit' || !textEl.value) return;
  try {
    const box = textEl.value.getBBox();
    const pad = 8;
    hitBox.value = { x: box.x - pad, y: box.y - pad, width: box.width + 2 * pad, height: box.height + 2 * pad };
  } catch {
    // not attached/rendered yet — the watch re-measures on the next change
  }
};

onMounted(() => nextTick(measureHitBox));
watch(
  [displayValue, () => props.widget.x, () => props.widget.y, () => props.widget.fontSize],
  () => nextTick(measureHitBox)
);
</script>
