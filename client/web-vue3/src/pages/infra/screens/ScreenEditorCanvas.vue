<template>
  <div class="screen-editor">
    <!-- Toolbar -->
    <div class="row items-center q-gutter-sm q-mb-sm">
      <q-btn-toggle
        v-model="tool"
        dense
        unelevated
        toggle-color="primary"
        :options="[
          { value: 'select', slot: 'select' },
          { value: 'add', slot: 'add' },
          { value: 'draw-zone', slot: 'draw-zone' },
          { value: 'add-link', slot: 'add-link' },
        ]"
      >
        <template #select>
          <q-icon name="mdi-cursor-default" />
          <q-tooltip>Select / drag</q-tooltip>
        </template>
        <template #add>
          <q-icon name="mdi-map-marker-plus" />
          <q-tooltip>Place widget (pick a peripheral, then click the plan)</q-tooltip>
        </template>
        <template #draw-zone>
          <q-icon name="mdi-vector-polygon" />
          <q-tooltip>Draw zone (click vertices, double-click to close, Esc cancels)</q-tooltip>
        </template>
        <template #add-link>
          <q-icon name="mdi-link-plus" />
          <q-tooltip>Place a link control (navigation, e.g. to the Web UI) — click the plan</q-tooltip>
        </template>
      </q-btn-toggle>

      <q-select
        v-if="tool === 'add' || tool === 'draw-zone'"
        v-model="selectedPeripheral"
        :options="peripheralOptions"
        option-label="label"
        use-input
        dense
        outlined
        clearable
        input-debounce="150"
        style="min-width: 280px"
        label="Peripheral"
        @filter="filterPeripherals"
      />
      <q-btn-toggle
        v-if="tool === 'add' && selectedPeripheral"
        v-model="addKind"
        dense
        unelevated
        toggle-color="secondary"
        :options="[
          { value: 'marker', label: 'Marker' },
          { value: 'text', label: 'Text' },
        ]"
      />

      <q-space />

      <q-btn-dropdown dense flat icon="mdi-resize" :loading="resizing" :disable="!bgUrl">
        <q-tooltip>Resize canvas to a device preset (widgets are rescaled)</q-tooltip>
        <q-list dense>
          <q-item-label header>
            Resize canvas — current {{ screen.backgroundWidth }} × {{ screen.backgroundHeight }}
          </q-item-label>
          <q-item v-close-popup clickable @click="resizeCanvas('mobile')">
            <q-item-section>Mobile — 780 × 500</q-item-section>
          </q-item>
          <q-item v-close-popup clickable @click="resizeCanvas('tablet')">
            <q-item-section>Tablet — 1280 × 800</q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>

      <q-badge v-if="dirty" color="orange" label="unsaved" />
      <q-btn dense color="primary" icon="mdi-content-save" label="Save" :disable="!dirty" :loading="saving" @click="save" />
      <q-btn dense flat icon="mdi-close" label="Close" @click="requestClose" />
    </div>

    <!-- Selected widget inspector — always rendered with a fixed height so
         selecting a widget never shifts the canvas (a layout jump mid-click
         would land the click on empty canvas and clear the selection) -->
    <q-banner dense rounded class="bg-blue-grey-1 q-mb-sm inspector">
      <div v-if="!selectedWidget" class="text-caption text-grey-6 row items-center" style="min-height: 40px">
        Select a widget on the plan to see peripheral details
      </div>
      <div v-else-if="selectedWidget.kind === 'link'" class="row items-center q-gutter-md" style="min-height: 40px">
        <div class="text-subtitle2">
          Link control
          <q-badge class="q-ml-xs" color="blue-grey-6" label="navigation" />
        </div>
        <q-input
          :model-value="selectedWidget.label || ''"
          label="Label"
          dense
          outlined
          style="width: 130px"
          @update:model-value="(val) => { selectedWidget.label = val; dirty = true; }"
        />
        <q-input
          :model-value="selectedWidget.href || ''"
          label="Target URL ('/' = Web UI)"
          dense
          outlined
          style="width: 210px"
          @update:model-value="(val) => { selectedWidget.href = val; dirty = true; }"
        />
        <IconPicker
          :model-value="selectedWidget.icon || null"
          label="Icon"
          style="min-width: 200px"
          @update:model-value="(val) => { selectedWidget.icon = val; dirty = true; }"
        />
        <div class="row items-center q-gutter-sm">
          <span class="text-caption">Size</span>
          <q-slider
            :model-value="selectedWidget.size || 36"
            :min="16"
            :max="72"
            dense
            style="width: 100px"
            @update:model-value="(val) => { selectedWidget.size = val; dirty = true; }"
          />
        </div>

        <q-space />

        <q-btn dense flat color="negative" icon="mdi-delete" @click="deleteSelected">
          <q-tooltip>Remove from screen (Del)</q-tooltip>
        </q-btn>
      </div>
      <div v-else class="row items-center q-gutter-md" style="min-height: 40px">
        <div>
          <div class="text-subtitle2">
            {{ inspectedPeripheral?.name || 'Unknown peripheral' }}
            <q-badge class="q-ml-xs" color="blue-grey-6">
              {{ inspectedPeripheral?.category?.title || inspectedPeripheral?.category?.name || '?' }}
            </q-badge>
            <q-badge v-if="!inspectedPeripheral" class="q-ml-xs" color="negative" label="peripheral not found" />
          </div>
          <div class="text-caption text-grey-8">
            #{{ selectedWidget.peripheralId }} · {{ selectedWidget.kind }}
            <template v-if="inspectedPeripheral">
              · value: {{ inspectedPeripheral.portValue ?? '—' }}
              <span v-if="inspectedPeripheral.deviceStatus === 'OFFLINE'" class="text-negative"> · device offline</span>
            </template>
            <template v-if="inspectedPeripheral?.description"> · {{ inspectedPeripheral.description }}</template>
          </div>
        </div>
        <div v-if="selectedWidget.kind === 'marker'" class="row items-center q-gutter-sm" style="min-width: 220px">
          <span class="text-caption">Size</span>
          <q-slider
            :model-value="selectedWidget.size || 28"
            :min="12"
            :max="72"
            dense
            style="width: 140px"
            @update:model-value="(val) => { selectedWidget.size = val; dirty = true; }"
          />
          <span class="text-caption">{{ selectedWidget.size || 28 }}</span>
        </div>
        <IconPicker
          v-if="selectedWidget.kind === 'marker'"
          :model-value="selectedWidget.icon || null"
          label="Icon (empty = category default)"
          style="min-width: 250px"
          @update:model-value="(val) => { selectedWidget.icon = val; dirty = true; }"
        />
        <div v-if="selectedWidget.kind === 'text'" class="row items-center q-gutter-sm" style="min-width: 220px">
          <span class="text-caption">Font</span>
          <q-slider
            :model-value="selectedWidget.fontSize || 20"
            :min="8"
            :max="64"
            dense
            style="width: 140px"
            @update:model-value="(val) => { selectedWidget.fontSize = val; dirty = true; }"
          />
          <span class="text-caption">{{ selectedWidget.fontSize || 20 }}</span>
        </div>
        <div v-if="selectedWidget.kind === 'zone'" class="text-caption text-grey-7">
          drag corners · click an edge midpoint to add a point · double-click a corner to remove it
        </div>

        <q-space />

        <q-btn dense flat icon="mdi-arrange-send-backward" @click="moveSelected(-1)">
          <q-tooltip>Send backward (under overlapping widgets)</q-tooltip>
        </q-btn>
        <q-btn dense flat icon="mdi-arrange-bring-forward" @click="moveSelected(1)">
          <q-tooltip>Bring forward (over overlapping widgets)</q-tooltip>
        </q-btn>
        <q-btn dense flat color="negative" icon="mdi-delete" @click="deleteSelected">
          <q-tooltip>Remove from screen (Del)</q-tooltip>
        </q-btn>
      </div>
    </q-banner>

    <!-- Canvas -->
    <div class="canvas-wrap">
      <DashboardScreenSvg
        :screen="screen"
        :widgets="widgets"
        :peripherals="peripherals"
        :bg-url="bgUrl"
        mode="edit"
        :selected-widget-id="selectedWidgetId"
        :draft-points="draftPoints"
        @canvas-click="onCanvasClick"
        @canvas-pointermove="onPointerMove"
        @canvas-pointerup="onPointerUp"
        @widget-pointerdown="onWidgetPointerDown"
        @vertex-pointerdown="onVertexPointerDown"
        @vertex-dblclick="onVertexDblClick"
        @midpoint-pointerdown="onMidpointPointerDown"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { SCREEN_SAVE_LAYOUT } from '@/graphql/queries';
import { screenService } from '@/_services';
import { useNotifications } from '@/composables';
import { defaultKindForCategory } from '@/_helpers/screen-widget';
import DashboardScreenSvg from '@/components/dashboard/DashboardScreenSvg.vue';
import IconPicker from '@/components/IconPicker.vue';

const props = defineProps({
  screen: { type: Object, required: true },
  peripherals: { type: Object, required: true },
  bgUrl: { type: String, default: null },
});

const emit = defineEmits(['saved', 'close']);

const { client } = useApolloClient();
const { notifyError, notifySuccess } = useNotifications();

// --- widgets draft state ---
const parseLayout = () => {
  try {
    const layout = JSON.parse(props.screen.layoutJson || '{"version":1,"widgets":[]}');
    return Array.isArray(layout.widgets) ? layout.widgets.map((w) => ({ ...w })) : [];
  } catch {
    return [];
  }
};

const widgets = ref(parseLayout());
const dirty = ref(false);
const saving = ref(false);
const selectedWidgetId = ref(null);
const tool = ref('select');
const addKind = ref('marker');

const selectedWidget = computed(() => widgets.value.find((w) => w.id === selectedWidgetId.value) || null);
const inspectedPeripheral = computed(() =>
  selectedWidget.value ? props.peripherals[selectedWidget.value.peripheralId] || null : null
);

watch(() => props.screen.id, () => {
  widgets.value = parseLayout();
  dirty.value = false;
  selectedWidgetId.value = null;
  tool.value = 'select';
});

// Canvas dimension change means the server rescaled the saved layout
// (preset resize / background replacement) — the local draft is in the old
// coordinate space, so re-parse from the authoritative layoutJson.
watch(
  () => [props.screen.backgroundWidth, props.screen.backgroundHeight],
  ([w, h], [oldW, oldH]) => {
    if (w === oldW && h === oldH) return;
    widgets.value = parseLayout();
    dirty.value = false;
  }
);

// --- peripheral picker ---
const selectedPeripheral = ref(null);
const peripheralFilter = ref('');

const allPeripheralOptions = computed(() =>
  Object.values(props.peripherals)
    .map((p) => ({
      label: `${p.name} — ${p.category?.title || p.category?.name || '?'} (#${p.id})`,
      value: p.id,
      category: p.category?.name,
    }))
    .sort((a, b) => a.label.localeCompare(b.label))
);
const peripheralOptions = computed(() => {
  const needle = peripheralFilter.value.toLowerCase();
  if (!needle) return allPeripheralOptions.value;
  return allPeripheralOptions.value.filter((o) => o.label.toLowerCase().includes(needle));
});
const filterPeripherals = (val, update) => {
  update(() => {
    peripheralFilter.value = val || '';
  });
};

watch(selectedPeripheral, (p) => {
  if (p && tool.value === 'add') {
    addKind.value = defaultKindForCategory(p.category) === 'text' ? 'text' : 'marker';
  }
});

// --- placement ---
const draftPoints = ref(null);

const round1 = (n) => Math.round(n * 10) / 10;

// set on drag release; the click the browser fires right after must not
// clear the selection (cleared on a 0ms timer, i.e. right after that click)
let suppressCanvasClick = false;

const onCanvasClick = ({ coords }) => {
  if (suppressCanvasClick) return;
  if (tool.value === 'add' && selectedPeripheral.value) {
    const widget = {
      id: crypto.randomUUID(),
      kind: addKind.value,
      peripheralId: selectedPeripheral.value.value,
      x: round1(coords.x),
      y: round1(coords.y),
    };
    if (widget.kind === 'marker') {
      widget.size = 28;
      widget.icon = null;
    } else {
      widget.fontSize = 20;
    }
    widgets.value.push(widget);
    selectedWidgetId.value = widget.id;
    dirty.value = true;
  } else if (tool.value === 'add-link') {
    const widget = {
      id: crypto.randomUUID(),
      kind: 'link',
      x: round1(coords.x),
      y: round1(coords.y),
      size: 36,
      icon: 'mdi-open-in-app',
      label: 'Web UI',
      href: '/',
    };
    widgets.value.push(widget);
    selectedWidgetId.value = widget.id;
    dirty.value = true;
    tool.value = 'select';
  } else if (tool.value === 'draw-zone' && selectedPeripheral.value) {
    draftPoints.value = [...(draftPoints.value || []), [round1(coords.x), round1(coords.y)]];
  } else {
    selectedWidgetId.value = null;
  }
};

const closeDraftZone = () => {
  if (!selectedPeripheral.value || !draftPoints.value || draftPoints.value.length < 3) return;
  const widget = {
    id: crypto.randomUUID(),
    kind: 'zone',
    peripheralId: selectedPeripheral.value.value,
    points: draftPoints.value,
  };
  widgets.value.push(widget);
  selectedWidgetId.value = widget.id;
  draftPoints.value = null;
  dirty.value = true;
  tool.value = 'select';
};

// --- dragging (whole widget / zone vertex) ---
const drag = ref(null); // { widget, vertexIndex?, start: {x,y}, orig }

const onWidgetPointerDown = ({ widget, coords }) => {
  selectedWidgetId.value = widget.id;
  if (tool.value !== 'select') return;
  drag.value = {
    widget,
    start: coords,
    orig: widget.kind === 'zone' ? widget.points.map((p) => [...p]) : { x: widget.x, y: widget.y },
  };
};

const onVertexPointerDown = ({ widget, vertexIndex, coords }) => {
  selectedWidgetId.value = widget.id;
  drag.value = { widget, vertexIndex, start: coords, orig: widget.points.map((p) => [...p]) };
};

/** Double-click on a vertex removes it (a polygon keeps at least 3 points). */
const onVertexDblClick = ({ widget, vertexIndex }) => {
  drag.value = null; // the dblclick's own pointerdowns may have armed a drag
  if (widget.points.length <= 3) return;
  widget.points.splice(vertexIndex, 1);
  dirty.value = true;
};

/** Click on an edge midpoint inserts a vertex there and starts dragging it. */
const onMidpointPointerDown = ({ widget, edgeIndex, coords }) => {
  selectedWidgetId.value = widget.id;
  const insertAt = edgeIndex + 1;
  widget.points.splice(insertAt, 0, [round1(coords.x), round1(coords.y)]);
  dirty.value = true;
  drag.value = { widget, vertexIndex: insertAt, start: coords, orig: widget.points.map((p) => [...p]) };
};

/**
 * Move the selected widget backward/forward in paint order. Only the order
 * within the same kind matters (zones, markers and texts render as separate
 * groups), so we swap with the nearest same-kind neighbor.
 */
const moveSelected = (dir) => {
  const widget = selectedWidget.value;
  if (!widget) return;
  const arr = widgets.value;
  const from = arr.indexOf(widget);
  let to = from + dir;
  while (to >= 0 && to < arr.length && arr[to].kind !== widget.kind) to += dir;
  if (to < 0 || to >= arr.length) return;
  [arr[from], arr[to]] = [arr[to], arr[from]];
  dirty.value = true;
};

const onPointerMove = ({ coords }) => {
  if (!drag.value) return;
  drag.value.moved = true;
  const { widget, vertexIndex, start, orig } = drag.value;
  const dx = coords.x - start.x;
  const dy = coords.y - start.y;
  if (vertexIndex != null) {
    widget.points[vertexIndex] = [round1(orig[vertexIndex][0] + dx), round1(orig[vertexIndex][1] + dy)];
  } else if (widget.kind === 'zone') {
    widget.points = orig.map((p) => [round1(p[0] + dx), round1(p[1] + dy)]);
  } else {
    widget.x = round1(orig.x + dx);
    widget.y = round1(orig.y + dy);
  }
};

const onPointerUp = () => {
  if (drag.value) {
    if (drag.value.moved) {
      // When a drag ends with the pointer off the widget, the browser fires
      // the click on the canvas (common ancestor) — which would deselect the
      // widget just dragged. Swallow exactly that one click.
      suppressCanvasClick = true;
      setTimeout(() => { suppressCanvasClick = false; }, 0);
      dirty.value = true;
    }
    drag.value = null;
  }
};

// --- delete / keyboard ---
const deleteSelected = () => {
  if (!selectedWidgetId.value) return;
  widgets.value = widgets.value.filter((w) => w.id !== selectedWidgetId.value);
  selectedWidgetId.value = null;
  dirty.value = true;
};

const onKeyDown = (event) => {
  if (event.key === 'Escape') {
    draftPoints.value = null;
  } else if (event.key === 'Enter' && draftPoints.value) {
    closeDraftZone();
  } else if ((event.key === 'Delete' || event.key === 'Backspace') && selectedWidgetId.value) {
    // don't hijack typing in inputs
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;
    deleteSelected();
  }
};

const onDblClick = () => {
  if (tool.value === 'draw-zone' && draftPoints.value) closeDraftZone();
};

onMounted(() => {
  document.addEventListener('keydown', onKeyDown);
  document.addEventListener('dblclick', onDblClick);
});
onUnmounted(() => {
  document.removeEventListener('keydown', onKeyDown);
  document.removeEventListener('dblclick', onDblClick);
});

// --- canvas resize (re-fits the stored background, server rescales layout) ---
const resizing = ref(false);

const resizeCanvas = async (preset) => {
  if (dirty.value) {
    if (!window.confirm('Resizing saves the current layout first. Continue?')) return;
    await save();
    if (dirty.value) return; // save failed — don't resize a stale layout
  }
  resizing.value = true;
  try {
    const result = await screenService.resizeBackground(props.screen.id, preset);
    notifySuccess(`Canvas resized to ${result.width}×${result.height}`);
    emit('saved'); // parent reloads the screen + background; dims watch re-parses widgets
  } catch (err) {
    notifyError(`Failed to resize canvas: ${err.message}`);
  } finally {
    resizing.value = false;
  }
};

// --- save / close ---
const save = async () => {
  saving.value = true;
  try {
    await client.mutate({
      mutation: SCREEN_SAVE_LAYOUT,
      variables: {
        id: props.screen.id,
        layoutJson: JSON.stringify({ version: 1, widgets: widgets.value }),
      },
    });
    dirty.value = false;
    notifySuccess('Layout saved');
    emit('saved');
  } catch (err) {
    notifyError(`Failed to save layout: ${err.message}`);
  } finally {
    saving.value = false;
  }
};

const requestClose = () => {
  if (dirty.value && !window.confirm('Discard unsaved layout changes?')) return;
  emit('close');
};

defineExpose({ dirty });
</script>

<style scoped>
.inspector {
  /* fixed height: appearing/changing content must never move the canvas */
  height: 64px;
  overflow: hidden;
}

.canvas-wrap {
  border: 1px solid #cfd8dc;
  border-radius: 4px;
  background: #fafafa;
  /* the SVG scales to fit; cap height so the toolbar stays visible */
  max-height: calc(100vh - 220px);
  overflow: auto;
}
</style>
