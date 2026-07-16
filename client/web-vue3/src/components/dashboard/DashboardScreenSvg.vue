<template>
  <svg
    ref="svgEl"
    class="screen-svg"
    :class="{ 'edit-mode': mode === 'edit' }"
    :viewBox="`0 0 ${width} ${height}`"
    preserveAspectRatio="xMidYMid meet"
    @click="onCanvasClick"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
  >
    <image
      v-if="bgUrl"
      :href="bgUrl"
      x="0"
      y="0"
      :width="width"
      :height="height"
    />
    <rect v-else class="no-bg" x="0" y="0" :width="width" :height="height" />

    <!-- zones under markers/text -->
    <ZoneWidget
      v-for="w in zones"
      :key="w.id"
      :widget="w"
      :peripheral="peripheralOf(w)"
      :mode="mode"
      :selected="w.id === selectedWidgetId"
      @activate="$emit('widget-activate', w)"
      @widget-pointerdown="$emit('widget-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
      @vertex-pointerdown="$emit('vertex-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
      @vertex-dblclick="$emit('vertex-dblclick', $event)"
      @midpoint-pointerdown="$emit('midpoint-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
    />
    <MarkerWidget
      v-for="w in markers"
      :key="w.id"
      :widget="w"
      :peripheral="peripheralOf(w)"
      :mode="mode"
      :selected="w.id === selectedWidgetId"
      @activate="$emit('widget-activate', w)"
      @widget-pointerdown="$emit('widget-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
    />
    <TextWidget
      v-for="w in texts"
      :key="w.id"
      :widget="w"
      :peripheral="peripheralOf(w)"
      :mode="mode"
      :selected="w.id === selectedWidgetId"
      @widget-pointerdown="$emit('widget-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
    />
    <!-- navigation controls (no peripheral binding) — topmost, always tappable -->
    <LinkWidget
      v-for="w in links"
      :key="w.id"
      :widget="w"
      :mode="mode"
      :selected="w.id === selectedWidgetId"
      @activate="$emit('widget-activate', w)"
      @widget-pointerdown="$emit('widget-pointerdown', { ...$event, coords: toSvgCoords($event.event) })"
    />

    <!-- zone-drawing preview (edit mode) -->
    <g v-if="draftPoints && draftPoints.length" class="draft-zone">
      <polyline :points="draftPoints.map((p) => `${p[0]},${p[1]}`).join(' ')" />
      <circle v-for="(p, i) in draftPoints" :key="i" class="vertex-handle" :cx="p[0]" :cy="p[1]" r="5" />
    </g>
  </svg>
</template>

<script setup>
import { computed, ref } from 'vue';
import MarkerWidget from './widgets/MarkerWidget.vue';
import ZoneWidget from './widgets/ZoneWidget.vue';
import TextWidget from './widgets/TextWidget.vue';
import LinkWidget from './widgets/LinkWidget.vue';

const props = defineProps({
  screen: { type: Object, required: true },
  /** layout widgets (parsed); passed separately so the editor can edit a draft copy */
  widgets: { type: Array, required: true },
  peripherals: { type: Object, required: true },
  bgUrl: { type: String, default: null },
  mode: { type: String, default: 'view' },
  selectedWidgetId: { type: String, default: null },
  /** in-progress zone vertices while drawing (edit mode) */
  draftPoints: { type: Array, default: null },
});

const emit = defineEmits([
  'widget-activate',
  'widget-pointerdown',
  'vertex-pointerdown',
  'vertex-dblclick',
  'midpoint-pointerdown',
  'canvas-click',
  'canvas-pointermove',
  'canvas-pointerup',
]);

const svgEl = ref(null);

const width = computed(() => props.screen.backgroundWidth || 1600);
const height = computed(() => props.screen.backgroundHeight || 1000);

const zones = computed(() => props.widgets.filter((w) => w.kind === 'zone'));
const markers = computed(() => props.widgets.filter((w) => w.kind === 'marker'));
const texts = computed(() => props.widgets.filter((w) => w.kind === 'text'));
const links = computed(() => props.widgets.filter((w) => w.kind === 'link'));

const peripheralOf = (w) => props.peripherals[w.peripheralId] || null;

/** Map a pointer/mouse event to SVG viewBox coordinates. */
const toSvgCoords = (event) => {
  const svg = svgEl.value;
  if (!svg) return { x: 0, y: 0 };
  const point = svg.createSVGPoint();
  point.x = event.clientX;
  point.y = event.clientY;
  const mapped = point.matrixTransform(svg.getScreenCTM().inverse());
  return { x: mapped.x, y: mapped.y };
};

const onCanvasClick = (event) => {
  if (props.mode !== 'edit') return;
  emit('canvas-click', { coords: toSvgCoords(event), event });
};
const onPointerMove = (event) => {
  if (props.mode !== 'edit') return;
  emit('canvas-pointermove', { coords: toSvgCoords(event), event });
};
const onPointerUp = (event) => {
  if (props.mode !== 'edit') return;
  emit('canvas-pointerup', { coords: toSvgCoords(event), event });
};

defineExpose({ toSvgCoords });
</script>

<style>
/* Screen widget state classes — moved from the legacy MobileWebLayout styles.
   Unscoped on purpose: applied to SVG nodes inside child widget components. */
.screen-svg {
  width: 100%;
  height: 100%;
}

.screen-svg.edit-mode {
  touch-action: none; /* reliable pointer-dragging on tablets */
  cursor: crosshair;
}

.screen-svg .no-bg {
  fill: #eceff1;
}

.screen-svg .marker-glyph {
  /* rendered as an SVG <path> from the MDI catalog (24x24 grid, scaled) */
  fill: #37474f;
  pointer-events: none;
}

.screen-svg .marker-disc {
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #2b6095;
  stroke-width: 1;
}

.screen-svg .actionable {
  cursor: pointer;
}

/* navigation link controls */
.screen-svg .link-disc {
  fill: #37474f;
  fill-opacity: 0.75;
  stroke: #cfd8dc;
  stroke-width: 1.2;
  paint-order: stroke;
}

.screen-svg .link-glyph {
  fill: #eceff1;
  pointer-events: none;
}

.screen-svg .link-label {
  fill: #37474f;
  font-family: Arial, sans-serif;
  font-size: 11px;
  paint-order: stroke;
  stroke: #fff;
  stroke-width: 2.5;
  pointer-events: none;
}

.screen-svg .selection-ring,
.screen-svg .vertex-handle {
  fill: rgba(255, 235, 59, 0.5);
  stroke: #f57f17;
  stroke-width: 1.5;
}

.screen-svg .vertex-handle {
  cursor: grab;
}

.screen-svg .midpoint-handle {
  fill: #fff;
  fill-opacity: 0.9;
  stroke: #f57f17;
  stroke-width: 1;
  stroke-dasharray: 2 1.5;
  cursor: copy;
}

.screen-svg .draft-zone polyline {
  fill: rgba(255, 235, 59, 0.2);
  stroke: #f57f17;
  stroke-width: 1.5;
  stroke-dasharray: 4 3;
}

.screen-svg .zone-widget.selected polygon {
  stroke: #f57f17;
  stroke-width: 1.5;
  stroke-dasharray: 4 3;
}

/* edit-mode hit/selection box around text widgets — transparent fill still
   captures pointer events (unlike fill: none), giving a pressable target */
.screen-svg .text-hit {
  fill: transparent;
  cursor: move;
}

.screen-svg .text-widget.selected .text-hit {
  fill: rgba(255, 235, 59, 0.15);
  stroke: #f57f17;
  stroke-width: 1.5;
  stroke-dasharray: 4 3;
}

/* --- category/state classes (legacy look preserved) --- */
.screen-svg .focus circle,
.screen-svg .focus polygon,
.screen-svg .focus.marker-widget .marker-disc {
  fill: green;
  stroke: yellow;
  stroke-width: 1;
}

.screen-svg .bulb-on .marker-disc,
.screen-svg .bulb-on polygon {
  fill: #d6d40f;
  fill-opacity: 0.7;
}

.screen-svg .bulb-off .marker-disc,
.screen-svg .bulb-off polygon {
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #2b6095;
  stroke-width: 0.5;
}

/* zone-shaped lights (LED strips) get a visible outline even when off */
.screen-svg .zone-widget.bulb-off polygon {
  stroke-width: 1;
}

.screen-svg .motion-off polygon {
  fill: #5a99de;
  fill-opacity: 0.3;
  stroke: #70808e;
  stroke-width: 0.8;
}

.screen-svg .motion-on polygon {
  fill: #e8b8bc;
  fill-opacity: 0.3;
  stroke: #ba1334;
  stroke-width: 0.8;
}

.screen-svg .heat-on polygon,
.screen-svg .heat-on .marker-disc {
  fill: rgb(244, 194, 168);
  fill-opacity: 0.61;
  stroke: rgb(226, 9, 9);
  stroke-opacity: 0.5;
}

.screen-svg .heat-off polygon,
.screen-svg .heat-off .marker-disc {
  fill: rgb(168, 193, 244);
  fill-opacity: 0.61;
  stroke: rgb(9, 96, 226);
  stroke-opacity: 0.5;
}

.screen-svg .lock .marker-disc {
  cursor: pointer;
  fill: rgb(98, 117, 129);
  fill-opacity: 0.59;
  stroke: #d3e5e5;
  stroke-width: 1.2;
  paint-order: stroke;
}

.screen-svg .lock .marker-glyph {
  fill: #d3e5e5;
}

.screen-svg .device-offline {
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);
  text-decoration: line-through;
}

.screen-svg text.device-offline,
.screen-svg .device-offline text {
  fill: rgb(226, 9, 9);
  stroke: none;
}

.screen-svg .luminosity-text,
.screen-svg .text-widget text {
  fill: #d3e5e5;
  fill-opacity: 0.9;
  font-family: Arial, sans-serif;
}
</style>
