<template>
  <q-page padding>
    <q-card flat bordered>
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-sprinkler-variant" class="q-mr-sm"/>
            Sprinkler Scheduler
          </div>
          <q-space/>
          <q-btn
            color="primary"
            icon="mdi-content-save"
            label="Save schedule"
            :loading="saving"
            :disable="saving || !rows.length"
            @click="saveSchedule"
          />
        </div>
      </q-card-section>

      <!-- Common day-of-week selector -->
      <q-card-section class="q-pt-none">
        <div class="text-subtitle2 text-grey-7 q-mb-sm">Run on days (applies to all)</div>
        <div class="row q-gutter-sm">
          <q-checkbox
            v-for="d in dayOptions"
            :key="d.value"
            v-model="selectedDays"
            :val="d.value"
            :label="d.label"
            dense
          />
        </div>
      </q-card-section>

      <q-separator/>

      <!-- 24h strip header -->
      <q-card-section class="q-pt-sm">
        <div class="row items-center">
          <div class="scheduler-label-col text-right text-grey-7 text-caption">Sprinkler</div>
          <div class="scheduler-strip-header flex">
            <span v-for="h in 24" :key="h" class="strip-hour">{{ h - 1 }}:00</span>
          </div>
        </div>
      </q-card-section>

      <!-- Rows: one per sprinkler job -->
      <q-card-section v-if="loading" class="text-center q-pa-xl">
        <q-spinner-dots color="primary" size="40px"/>
      </q-card-section>
      <q-card-section v-else-if="!rows.length" class="text-center text-grey-6 q-pa-xl">
        No active sprinkler jobs. Create a job linked to a sprinkler peripheral and set it to ACTIVE to see it here.
      </q-card-section>
      <template v-else>
        <q-card-section
          v-for="row in rows"
          :key="row.jobId"
          class="scheduler-row row items-center no-wrap"
        >
          <div class="scheduler-label-col">
            <div class="text-weight-medium">
              <router-link :to="'/admin/peripherals/' + row.peripheralId + '/view'" class="text-primary text-weight-medium" style="text-decoration: none;">
                {{ row.peripheralName }}
              </router-link>
            </div>
            <div class="text-caption text-grey-7">
              <router-link :to="'/admin/peripherals/' + row.peripheralId + '/view'" class="text-grey-7">#{{ row.peripheralId }}</router-link>
              · {{ row.timeoutMinutes }} min
              <span v-if="row.peripheralDescription"> · {{ row.peripheralDescription }}</span>
            </div>
          </div>
          <div
            class="scheduler-strip flex relative-position"
            @click="onStripClick(row, $event)"
            @mousedown="onBarMouseDown(row, $event)"
          >
            <div
              v-if="row.startMinutesLocal != null"
              class="scheduler-bar"
              :style="barStyle(row)"
              @mousedown.stop="onBarMouseDown(row, $event)"
            >
              <span class="bar-label">{{ formatTime(row.startMinutesLocal) }}</span>
            </div>
          </div>
        </q-card-section>
      </template>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useQuery, useMutation } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { SPRINKLER_SCHEDULE_JOBS, JOB_UPDATE } from '@/graphql/queries/jobs';

const MINUTES_PER_DAY = 24 * 60;
const STEP_MINUTES = 5;
/** Last 5-min slot: 23:55 */
const MAX_SNAPPED_MINUTES = 23 * 60 + 55; // 1435
const QUARTZ_DOW = { SUN: 1, MON: 2, TUE: 3, WED: 4, THU: 5, FRI: 6, SAT: 7 };

/** Snap minutes to STEP_MINUTES (0, 5, 10, ...), clamped to [0, MAX_SNAPPED_MINUTES]. */
function snapToStep(minutes) {
  const m = Math.round(Number(minutes) / STEP_MINUTES) * STEP_MINUTES;
  return Math.max(0, Math.min(MAX_SNAPPED_MINUTES, m));
}

/** Get strip content box (where the bar is drawn) so pixel position matches bar %. */
function getStripContentRect(stripEl) {
  const rect = stripEl.getBoundingClientRect();
  const cs = getComputedStyle(stripEl);
  const pl = Number.parseFloat(cs.paddingLeft) || 0;
  const pr = Number.parseFloat(cs.paddingRight) || 0;
  const bl = Number.parseFloat(cs.borderLeftWidth) || 0;
  const br = Number.parseFloat(cs.borderRightWidth) || 0;
  const contentLeft = rect.left + bl + pl;
  const contentWidth = rect.width - bl - br - pl - pr;
  return { contentLeft, contentWidth };
}

/** Pixel position to minutes (0..1439) using strip content box, then snap to step. */
function positionToMinutes(clientX, stripEl) {
  const { contentLeft, contentWidth } = getStripContentRect(stripEl);
  if (contentWidth <= 0) return 0;
  const x = clientX - contentLeft;
  const pct = Math.max(0, Math.min(1, x / contentWidth));
  return snapToStep(pct * MINUTES_PER_DAY);
}

/** Parse Quartz cron (sec min hour dayOfMonth month dayOfWeek) to { hourUtc, minuteUtc, dayOfWeek } */
function parseCron(cron) {
  if (!cron || !cron.trim()) return null;
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) return null;
  const minute = Number.parseInt(parts[1], 10);
  const hour = Number.parseInt(parts[2], 10);
  const dayOfWeek = parts[5];
  if (Number.isNaN(minute) || Number.isNaN(hour)) return null;
  return { hourUtc: hour, minuteUtc: minute, dayOfWeek };
}

/** UTC time (hour, minute) to local start minutes (0..1439) */
function utcToLocalMinutes(hourUtc, minuteUtc) {
  const d = new Date();
  d.setUTCHours(hourUtc, minuteUtc, 0, 0);
  return d.getHours() * 60 + d.getMinutes();
}

/** Local start minutes (0..1439) to UTC { hour, minute } */
function localMinutesToUtc(localMinutes) {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setMinutes(localMinutes);
  return { hourUtc: d.getUTCHours(), minuteUtc: d.getUTCMinutes() };
}

/** Build Quartz cron: 0 sec min hour ? * dayOfWeek */
function buildCron(minuteUtc, hourUtc, dayOfWeekStr) {
  return `0 ${minuteUtc} ${hourUtc} ? * ${dayOfWeekStr}`;
}

/**
 * Parse Quartz day-of-week field (1-7, Sun-Sat) to array of numbers.
 * Supports "2,4,6", "1-5", "*", "?".
 */
function parseDayOfWeekToArray(dowStr) {
  if (!dowStr || !dowStr.trim()) return [QUARTZ_DOW.MON, QUARTZ_DOW.TUE, QUARTZ_DOW.WED, QUARTZ_DOW.THU, QUARTZ_DOW.FRI];
  const s = dowStr.trim();
  if (s === '*' || s === '?') return [1, 2, 3, 4, 5, 6, 7];
  const set = new Set();
  const parts = s.split(',');
  for (const part of parts) {
    const range = part.trim().split('-');
    if (range.length === 1) {
      const n = Number.parseInt(range[0], 10);
      if (!Number.isNaN(n) && n >= 1 && n <= 7) set.add(n);
    } else {
      const low = Number.parseInt(range[0].trim(), 10);
      const high = Number.parseInt(range[1].trim(), 10);
      if (!Number.isNaN(low) && !Number.isNaN(high)) {
        for (let i = Math.max(1, low); i <= Math.min(7, high); i++) set.add(i);
      }
    }
  }
  const arr = [...set].sort((a, b) => a - b);
  return arr.length > 0 ? arr : [QUARTZ_DOW.MON, QUARTZ_DOW.TUE, QUARTZ_DOW.WED, QUARTZ_DOW.THU, QUARTZ_DOW.FRI];
}

export default defineComponent({
  name: 'SprinklerScheduler',
  setup() {
    const $q = useQuasar();
    const selectedDays = ref([QUARTZ_DOW.MON, QUARTZ_DOW.TUE, QUARTZ_DOW.WED, QUARTZ_DOW.THU, QUARTZ_DOW.FRI]);
    const saving = ref(false);
    /** User overrides: jobId -> start minutes (0..1439). Persists across re-renders so drag works. */
    const startMinutesOverrides = ref({});
    const dragJobId = ref(null);
    const dragStripEl = ref(null);
    const dragStartX = ref(0);
    const dragStartMinutes = ref(0);

    const dayOptions = [
      { value: QUARTZ_DOW.SUN, label: 'Sun' },
      { value: QUARTZ_DOW.MON, label: 'Mon' },
      { value: QUARTZ_DOW.TUE, label: 'Tue' },
      { value: QUARTZ_DOW.WED, label: 'Wed' },
      { value: QUARTZ_DOW.THU, label: 'Thu' },
      { value: QUARTZ_DOW.FRI, label: 'Fri' },
      { value: QUARTZ_DOW.SAT, label: 'Sat' },
    ];

    const { result: scheduleResult, loading, refetch } = useQuery(SPRINKLER_SCHEDULE_JOBS);
    const jobUpdateMutation = useMutation(JOB_UPDATE);

    // Initialize selected days from first job's cron when data loads
    watch(
      () => scheduleResult.value?.sprinklerScheduleJobs,
      (jobs) => {
        const trigger = jobs?.[0]?.cronTriggers?.[0];
        const expr = trigger?.expression;
        if (!expr) return;
        const parsed = parseCron(expr);
        if (parsed?.dayOfWeek) {
          selectedDays.value = parseDayOfWeekToArray(parsed.dayOfWeek);
        }
      },
      { immediate: true }
    );

    const rows = computed(() => {
      const data = scheduleResult.value?.sprinklerScheduleJobs;
      const overrides = startMinutesOverrides.value;
      if (!data || !Array.isArray(data)) return [];
      return data.map((entry) => {
        const trigger = entry.cronTriggers?.[0];
        let fromCron = null;
        let cronParsed = null;
        if (trigger?.expression) {
          cronParsed = parseCron(trigger.expression);
          if (cronParsed) {
            fromCron = utcToLocalMinutes(cronParsed.hourUtc, cronParsed.minuteUtc);
          }
        }
        const startMinutesLocal = overrides[entry.jobId] !== undefined
          ? overrides[entry.jobId]
          : (fromCron === null || fromCron === undefined ? 0 : fromCron);
        return {
          ...entry,
          triggerId: trigger?.id,
          triggerExpression: trigger?.expression,
          cronParsed,
          startMinutesLocal,
        };
      });
    });

    function formatTime(minutes) {
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return `${h}:${m.toString().padStart(2, '0')}`;
    }

    function barStyle(row) {
      const left = (row.startMinutesLocal / MINUTES_PER_DAY) * 100;
      const width = (row.timeoutMinutes / 60 / 24) * 100;
      return {
        left: `${left}%`,
        width: `${Math.min(width, 100 - left)}%`,
      };
    }

    function onStripClick(row, ev) {
      const strip = ev.currentTarget;
      const m = positionToMinutes(ev.clientX, strip);
      startMinutesOverrides.value = { ...startMinutesOverrides.value, [row.jobId]: m };
    }

    function onBarMouseDown(row, ev) {
      ev.preventDefault();
      ev.stopPropagation();
      dragJobId.value = row.jobId;
      dragStripEl.value = ev.target.closest('.scheduler-strip');
      dragStartX.value = ev.clientX;
      dragStartMinutes.value = row.startMinutesLocal;
    }

    function onMouseMove(ev) {
      if (dragJobId.value == null || !dragStripEl.value) return;
      const m = positionToMinutes(ev.clientX, dragStripEl.value);
      startMinutesOverrides.value = { ...startMinutesOverrides.value, [dragJobId.value]: m };
    }

    function onMouseUp() {
      dragJobId.value = null;
      dragStripEl.value = null;
    }

    function getDayOfWeekCronValue() {
      if (!selectedDays.value || selectedDays.value.length === 0) return '*';
      return selectedDays.value.sort((a, b) => a - b).join(',');
    }

    async function saveSchedule() {
      saving.value = true;
      const dow = getDayOfWeekCronValue();
      try {
        for (const row of rows.value) {
          const { hourUtc, minuteUtc } = localMinutesToUtc(row.startMinutesLocal);
          const newExpression = buildCron(minuteUtc, hourUtc, dow);
          if (!row.triggerId) continue;
          if (row.triggerExpression === newExpression) continue;
          const { mutate } = jobUpdateMutation;
          await mutate({
            id: row.jobId,
            job: {
              cronTriggers: [{ id: row.triggerId, expression: newExpression }],
            },
          });
        }
        await refetch();
        startMinutesOverrides.value = {};
        $q.notify({
          color: 'positive',
          message: 'Schedule saved',
          icon: 'mdi-check-circle',
          position: 'top',
          timeout: 2000,
        });
      } catch (e) {
        $q.notify({
          color: 'negative',
          message: 'Failed to save: ' + (e.message || String(e)),
          icon: 'mdi-alert-circle',
          position: 'top',
        });
      } finally {
        saving.value = false;
      }
    }

    onMounted(() => {
      globalThis.addEventListener('mousemove', onMouseMove);
      globalThis.addEventListener('mouseup', onMouseUp);
    });
    onUnmounted(() => {
      globalThis.removeEventListener('mousemove', onMouseMove);
      globalThis.removeEventListener('mouseup', onMouseUp);
    });

    return {
      loading,
      saving,
      rows,
      selectedDays,
      dayOptions,
      barStyle,
      formatTime,
      onStripClick,
      onBarMouseDown,
      saveSchedule,
    };
  },
});
</script>

<style scoped>
.scheduler-label-col {
  width: 220px;
  min-width: 220px;
}
.scheduler-strip-header,
.scheduler-strip {
  height: 36px;
  flex: 1;
  min-width: 0;
  background: repeating-linear-gradient(
    90deg,
    transparent,
    transparent  calc(100% / 24 - 1px),
    rgba(0, 0, 0, 0.06) calc(100% / 24 - 1px),
    rgba(0, 0, 0, 0.06) calc(100% / 24)
  );
  border-radius: 4px;
}
.scheduler-strip-header {
  display: flex;
  align-items: center;
}
.strip-hour {
  flex: 1;
  font-size: 10px;
  text-align: center;
  color: #999;
}
.scheduler-row {
  padding-top: 8px;
  padding-bottom: 8px;
}
.scheduler-row:hover .scheduler-strip {
  background-color: rgba(0, 0, 0, 0.02);
}
.scheduler-bar {
  position: absolute;
  top: 4px;
  height: 28px;
  background: linear-gradient(135deg, #26a69a 0%, #00897b 100%);
  border-radius: 4px;
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.scheduler-bar:hover {
  filter: brightness(1.05);
}
.scheduler-bar:active {
  cursor: grabbing;
}
.bar-label {
  font-size: 11px;
  color: white;
  text-shadow: 0 0 1px rgba(0,0,0,0.5);
}
</style>
