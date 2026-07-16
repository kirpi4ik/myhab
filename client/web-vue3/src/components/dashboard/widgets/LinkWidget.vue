<template>
  <g
    :transform="`translate(${widget.x}, ${widget.y})`"
    :class="['screen-widget', 'link-widget', { selected: selected, actionable: mode === 'view' }]"
    @click.stop="onClick"
    @pointerdown.stop="$emit('widget-pointerdown', { widget, event: $event })"
  >
    <circle class="link-disc" :r="radius" cx="0" cy="0" />
    <path
      v-if="iconPath"
      class="link-glyph"
      :d="iconPath"
      :transform="`translate(${-iconBox / 2}, ${-iconBox / 2}) scale(${iconBox / 24})`"
    />
    <text v-if="widget.label" class="link-label" x="0" :y="radius + 12" text-anchor="middle">
      {{ widget.label }}
    </text>
    <circle v-if="selected" class="selection-ring" :r="radius + 4" cx="0" cy="0" />
  </g>
</template>

<script setup>
import { computed, ref } from 'vue';
import { mdiNameToExport } from '@/_helpers/screen-widget';

/**
 * Navigation control widget — not bound to a peripheral. Clicking it in view
 * mode emits 'activate'; the viewer routes to widget.href.
 */
const props = defineProps({
  widget: { type: Object, required: true },
  mode: { type: String, default: 'view' },
  selected: { type: Boolean, default: false },
});

const emit = defineEmits(['activate', 'widget-pointerdown']);

const mdiIcons = ref(null);
import('@quasar/extras/mdi-v6').then((module) => {
  mdiIcons.value = module;
});

const radius = computed(() => (props.widget.size || 36) / 2);
const iconBox = computed(() => radius.value * 1.3);

const iconPath = computed(() => {
  if (!mdiIcons.value) return null;
  const name = props.widget.icon || 'mdi-open-in-app';
  return mdiIcons.value[mdiNameToExport(name)] || mdiIcons.value.mdiOpenInApp || null;
});

const onClick = () => {
  if (props.mode !== 'view') return;
  emit('activate', props.widget);
};
</script>
