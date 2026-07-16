<template>
  <g
    :transform="`translate(${widget.x}, ${widget.y})`"
    :class="['screen-widget', 'marker-widget', stateClass, { selected: selected, actionable: actionable }]"
    @click.stop="onClick"
    @pointerdown.stop="$emit('widget-pointerdown', { widget, event: $event })"
  >
    <circle class="marker-disc" :r="radius" cx="0" cy="0" />
    <path
      v-if="iconPath"
      class="marker-glyph"
      :d="iconPath"
      :transform="`translate(${-iconBox / 2}, ${-iconBox / 2}) scale(${iconBox / 24})`"
    />
    <circle v-if="selected" class="selection-ring" :r="radius + 4" cx="0" cy="0" />
  </g>
</template>

<script setup>
import { computed, ref } from 'vue';
import { getWidgetClass, resolveIconName, mdiNameToExport, isActionableCategory } from '@/_helpers/screen-widget';

const props = defineProps({
  widget: { type: Object, required: true },
  peripheral: { type: Object, default: null },
  mode: { type: String, default: 'view' },
  selected: { type: Boolean, default: false },
});

const emit = defineEmits(['activate', 'widget-pointerdown']);

// Full MDI catalog (SVG path data, 24x24 grid) — lazily loaded once for all
// markers; webpack splits it into its own cached chunk.
const mdiIcons = ref(null);
import('@quasar/extras/mdi-v6').then((module) => {
  mdiIcons.value = module;
});

const flashing = ref(false);

const category = computed(() => props.peripheral?.category?.name);
const radius = computed(() => (props.widget.size || 28) / 2);
const iconBox = computed(() => radius.value * 1.3);
const actionable = computed(() => props.mode === 'view' && isActionableCategory(category.value));

const iconPath = computed(() => {
  if (!mdiIcons.value) return null;
  const name = resolveIconName(props.widget, props.peripheral);
  return mdiIcons.value[mdiNameToExport(name)] || mdiIcons.value.mdiCircleMedium || null;
});

const stateClass = computed(() => {
  if (flashing.value) return 'focus';
  return getWidgetClass(category.value, props.peripheral?.state, props.peripheral?.deviceStatus);
});

const onClick = () => {
  if (props.mode !== 'view') return;
  if (actionable.value) {
    // brief visual feedback (port of the legacy applyFocusEffect)
    flashing.value = true;
    setTimeout(() => { flashing.value = false; }, 100);
  }
  emit('activate', props.widget);
};
</script>
