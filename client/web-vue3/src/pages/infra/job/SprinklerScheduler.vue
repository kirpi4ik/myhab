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

      <q-separator/>

      <!-- Default days bulk-apply control -->
      <q-card-section class="q-pt-md q-pb-sm">
        <div class="row items-center no-wrap">
          <div class="scheduler-label-col text-right text-grey-7 text-caption">Default days</div>
          <div class="scheduler-days-col">
            <div class="day-chip-group">
              <div
                v-for="d in dayOptions"
                :key="d.value"
                :class="['day-chip', { 'day-chip--on': defaultDays.includes(d.value) }]"
                @click="toggleDefaultDay(d.value)"
                :title="d.full + (defaultDays.includes(d.value) ? ' (selected)' : '')"
              >{{ d.label }}</div>
            </div>
          </div>
          <div class="scheduler-strip-wrap row items-center q-pl-sm">
            <q-btn
              flat
              dense
              no-caps
              color="primary"
              icon="mdi-content-duplicate"
              label="Apply to all"
              :disable="!rows.length"
              @click="applyDefaultDaysToAll"
            >
              <q-tooltip>Copy these days into every sprinkler row below. Each row remains individually editable afterwards.</q-tooltip>
            </q-btn>
          </div>
        </div>
      </q-card-section>

      <q-separator/>

      <!-- 24h strip header -->
      <q-card-section class="q-pt-sm">
        <div class="row items-center no-wrap">
          <div class="scheduler-label-col text-right text-grey-7 text-caption">Sprinkler</div>
          <div class="scheduler-days-col text-center text-grey-7 text-caption">Days</div>
          <div class="scheduler-strip-wrap">
            <div class="scheduler-strip-header">
              <span v-for="h in 24" :key="h" class="strip-hour">{{ h - 1 }}</span>
            </div>
          </div>
        </div>
      </q-card-section>

      <!-- Rows: one per sprinkler job -->
      <q-card-section v-if="loading" class="text-center q-pa-xl">
        <q-spinner-dots color="primary" size="40px"/>
      </q-card-section>
      <q-card-section v-else-if="!rows.length" class="text-center text-grey-6 q-pa-xl">
        No sprinkler jobs found. Create a job linked to a sprinkler peripheral to see it here.
      </q-card-section>
      <template v-else>
        <q-card-section
          v-for="row in rows"
          :key="row.jobId"
          :class="['scheduler-row row items-center no-wrap', { 'scheduler-row--disabled': !row.isActive }]"
        >
          <div class="scheduler-label-col">
            <div class="text-weight-medium">
              <router-link :to="'/admin/peripherals/' + row.peripheralId + '/view'" :class="row.isActive ? 'text-primary' : 'text-grey-6'" class="text-weight-medium" style="text-decoration: none;">
                {{ row.peripheralName }}
              </router-link>
              <q-badge v-if="!row.isActive" color="grey-4" text-color="grey-7" class="q-ml-sm" label="disabled"/>
            </div>
            <div class="text-caption text-grey-7">
              <router-link :to="'/admin/peripherals/' + row.peripheralId + '/view'" class="text-grey-7">#{{ row.peripheralId }}</router-link>
              · {{ row.timeoutMinutes }} min
              <span v-if="row.peripheralDescription"> · {{ row.peripheralDescription }}</span>
            </div>
          </div>

          <div class="scheduler-days-col">
            <div class="day-chip-group">
              <div
                v-for="d in dayOptions"
                :key="d.value"
                :class="['day-chip', { 'day-chip--on': row.selectedDays.includes(d.value), 'day-chip--disabled': !row.isActive }]"
                @click="toggleDay(row, d.value)"
                :title="d.full + (row.selectedDays.includes(d.value) ? ' (selected)' : '')"
              >{{ d.label }}</div>
            </div>
          </div>

          <div class="scheduler-strip-wrap">
            <div
              class="scheduler-strip relative-position"
              :style="stripContentStyle(row)"
              :data-total-minutes="rowStripMinutes(row)"
              @click="onStripClick(row, $event)"
              @mousedown="onBarMouseDown(row, $event)"
            >
              <div
                v-if="rowOverflowsMidnight(row)"
                class="scheduler-strip__tomorrow"
                :style="tomorrowZoneStyle(row)"
              ></div>
              <div
                v-if="rowOverflowsMidnight(row)"
                class="scheduler-strip__midnight-divider"
                :style="midnightDividerStyle(row)"
              ></div>
              <div
                v-if="row.startMinutesLocal != null"
                :class="['scheduler-bar', { 'scheduler-bar--disabled': !row.isActive, 'scheduler-bar--overflow': rowOverflowsMidnight(row) }]"
                :style="barStyle(row)"
                :title="barTooltip(row)"
                @mousedown.stop="onBarMouseDown(row, $event)"
              >
                <span class="bar-label">{{ formatTime(row.startMinutesLocal) }} → {{ formatTime((row.startMinutesLocal + row.timeoutMinutes) % MINUTES_PER_DAY) }}<span v-if="rowOverflowsMidnight(row)" class="bar-label-next"> (+1d)</span></span>
              </div>
            </div>
          </div>
        </q-card-section>
      </template>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted } from 'vue';
import { useQuery, useMutation } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { SPRINKLER_SCHEDULE_JOBS, JOB_UPDATE } from '@/graphql/queries/jobs';

const MINUTES_PER_DAY = 24 * 60;
const STEP_MINUTES = 5;
/** Last 5-min slot: 23:55 */
const MAX_SNAPPED_MINUTES = 23 * 60 + 55; // 1435
const QUARTZ_DOW = { SUN: 1, MON: 2, TUE: 3, WED: 4, THU: 5, FRI: 6, SAT: 7 };
const DEFAULT_DAYS = [QUARTZ_DOW.MON, QUARTZ_DOW.TUE, QUARTZ_DOW.WED, QUARTZ_DOW.THU, QUARTZ_DOW.FRI];

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

/**
 * Convert a click's clientX to minutes-of-day for the today portion only.
 * The strip can extend past 24h to visualize cross-midnight bars; clicks past
 * the midnight divider are returned as `null` so the caller can ignore them.
 *
 * The strip's content width is `100% * (totalMinutes / MINUTES_PER_DAY)` —
 * i.e. one MINUTES_PER_DAY-wide slice always corresponds to the visible 0-24h
 * portion regardless of how far the strip extends.
 */
function positionToMinutes(clientX, stripEl) {
  const { contentLeft, contentWidth } = getStripContentRect(stripEl);
  if (contentWidth <= 0) return null;
  const x = clientX - contentLeft;
  if (x < 0) return 0;
  // Pixels per minute is uniform across the strip. Use the strip's
  // "minutes-per-pixel" scale to convert: each ratio segment 1/N of the strip
  // = (totalMinutes/N) minutes; but since stripWidthMinutes >= 24h, we just
  // need to know where 24h ends. The DOM strip width corresponds to the
  // strip's own data-total-minutes attribute when present, otherwise 24h.
  const totalMinutes = Number.parseInt(stripEl?.dataset?.totalMinutes || '', 10) || MINUTES_PER_DAY;
  const minutes = (x / contentWidth) * totalMinutes;
  if (minutes > MINUTES_PER_DAY) return null; // past midnight — out of cron range
  return snapToStep(minutes);
}

/**
 * Parse a stored cron expression. Accepts both:
 *   - 5-field standard cron: `min hour dom month dow`         (dow = Unix 0..6, 0=Sun)
 *   - 6-field Quartz cron:   `sec min hour dom month dow`     (dow = Quartz 1..7, 1=Sun) — legacy data only
 *
 * Returns { hourUtc, minuteUtc, dayOfWeek } where `dayOfWeek` is normalized to the
 * **Quartz** form (1..7) so the chip toggles (which use QUARTZ_DOW) match.
 */
function parseCron(cron) {
  if (!cron || !cron.trim()) return null;
  const parts = cron.trim().split(/\s+/);
  let minute, hour, dayOfWeek, isStandard;
  if (parts.length === 5) {
    minute = Number.parseInt(parts[0], 10);
    hour = Number.parseInt(parts[1], 10);
    dayOfWeek = parts[4];
    isStandard = true;
  } else if (parts.length >= 6) {
    minute = Number.parseInt(parts[1], 10);
    hour = Number.parseInt(parts[2], 10);
    dayOfWeek = parts[5];
    isStandard = false;
  } else {
    return null;
  }
  if (Number.isNaN(minute) || Number.isNaN(hour)) return null;
  if (isStandard) dayOfWeek = unixDayOfWeekToQuartz(dayOfWeek);
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

/**
 * Build a 5-field standard cron expression: `min hour * * dow` with Unix DOW (0..6, 0=Sun).
 * The server's normalizeCronExpression converts this to the 6-field Quartz form Quartz needs.
 *
 * 5-field is also what `<cron-light>` (used in JobEdit) understands natively, so the same
 * trigger renders correctly in the generic job editor after being saved here.
 *
 * @param dayOfWeekStr DOW field already in Unix form (e.g., "0,1,2" = Sun,Mon,Tue), or "*"
 */
function buildCron(minuteUtc, hourUtc, dayOfWeekStr) {
  return `${minuteUtc} ${hourUtc} * * ${dayOfWeekStr}`;
}

/**
 * Convert a Unix-cron DOW field (0..6, 0=Sun) to Quartz (1..7, 1=Sun).
 * Supports comma lists and ranges. "*" / "?" / "" pass through untouched.
 */
function unixDayOfWeekToQuartz(dowStr) {
  if (!dowStr || dowStr === '*' || dowStr === '?') return dowStr;
  return dowStr.split(',').map(seg => {
    const s = seg.trim();
    if (s.includes('-')) {
      const [a, b] = s.split('-').map(p => Number.parseInt(p.trim(), 10));
      if (Number.isNaN(a) || Number.isNaN(b)) return s;
      return `${a + 1}-${b + 1}`;
    }
    const n = Number.parseInt(s, 10);
    return Number.isNaN(n) ? s : String(n + 1);
  }).join(',');
}

/**
 * Parse a Quartz-style day-of-week field (1..7, 1=Sun) into an array of numbers.
 * Supports "2,4,6", "1-5", "*", "?".
 */
function parseDayOfWeekToArray(dowStr) {
  if (!dowStr || !dowStr.trim()) return DEFAULT_DAYS.slice();
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
  return arr.length > 0 ? arr : DEFAULT_DAYS.slice();
}

export default defineComponent({
  name: 'SprinklerScheduler',
  setup() {
    const $q = useQuasar();
    const saving = ref(false);
    /** User overrides: jobId -> start minutes (0..1439). Persists across re-renders so drag works. */
    const startMinutesOverrides = ref({});
    /** User overrides: jobId -> sorted Quartz DOW array (1..7). */
    const dayOverrides = ref({});
    /**
     * "Default days" template for the bulk-apply button. Not auto-applied —
     * the user explicitly clicks "Apply to all" to copy this into every row's
     * dayOverrides. After that each row remains individually editable.
     */
    const defaultDays = ref(DEFAULT_DAYS.slice());
    const dragJobId = ref(null);
    const dragStripEl = ref(null);
    const dragStartX = ref(0);
    const dragStartMinutes = ref(0);

    const dayOptions = [
      { value: QUARTZ_DOW.SUN, label: 'S', full: 'Sunday' },
      { value: QUARTZ_DOW.MON, label: 'M', full: 'Monday' },
      { value: QUARTZ_DOW.TUE, label: 'T', full: 'Tuesday' },
      { value: QUARTZ_DOW.WED, label: 'W', full: 'Wednesday' },
      { value: QUARTZ_DOW.THU, label: 'T', full: 'Thursday' },
      { value: QUARTZ_DOW.FRI, label: 'F', full: 'Friday' },
      { value: QUARTZ_DOW.SAT, label: 'S', full: 'Saturday' },
    ];

    const { result: scheduleResult, loading, refetch } = useQuery(SPRINKLER_SCHEDULE_JOBS);
    const jobUpdateMutation = useMutation(JOB_UPDATE);

    const rows = computed(() => {
      const data = scheduleResult.value?.sprinklerScheduleJobs;
      const startOv = startMinutesOverrides.value;
      const dayOv = dayOverrides.value;
      if (!data || !Array.isArray(data)) return [];
      return data.map((entry) => {
        const trigger = entry.cronTriggers?.[0];
        let fromCron = null;
        let cronParsed = null;
        let daysFromCron = null;
        if (trigger?.expression) {
          cronParsed = parseCron(trigger.expression);
          if (cronParsed) {
            fromCron = utcToLocalMinutes(cronParsed.hourUtc, cronParsed.minuteUtc);
            if (cronParsed.dayOfWeek) {
              daysFromCron = parseDayOfWeekToArray(cronParsed.dayOfWeek);
            }
          }
        }
        const startMinutesLocal = startOv[entry.jobId] !== undefined
          ? startOv[entry.jobId]
          : (fromCron === null || fromCron === undefined ? 0 : fromCron);
        const selectedDays = dayOv[entry.jobId] !== undefined
          ? dayOv[entry.jobId]
          : (daysFromCron || DEFAULT_DAYS.slice());
        return {
          ...entry,
          isActive: entry.jobState === 'ACTIVE',
          triggerId: trigger?.id,
          triggerExpression: trigger?.expression,
          cronParsed,
          startMinutesLocal,
          selectedDays,
        };
      });
    });

    function formatTime(minutes) {
      const h = Math.floor(minutes / 60);
      const m = minutes % 60;
      return `${h}:${m.toString().padStart(2, '0')}`;
    }

    /** True when start + duration crosses the next midnight. */
    function rowOverflowsMidnight(row) {
      return (row.startMinutesLocal + row.timeoutMinutes) > MINUTES_PER_DAY;
    }

    /** Total minutes the strip needs to display (>= 24h, extra is the tomorrow tail). */
    function rowStripMinutes(row) {
      if (!rowOverflowsMidnight(row)) return MINUTES_PER_DAY;
      // Add a small visual padding past the bar end (15 min) for readability.
      const end = row.startMinutesLocal + row.timeoutMinutes + 15;
      return Math.max(MINUTES_PER_DAY, Math.ceil(end / STEP_MINUTES) * STEP_MINUTES);
    }

    /** Pixel width of the strip content, expressed as a percentage so it scales with the wrap. */
    function stripContentStyle(row) {
      const total = rowStripMinutes(row);
      const widthPct = (total / MINUTES_PER_DAY) * 100;
      return {
        width: `${widthPct}%`,
        // dataset attribute also set via :data-total-minutes binding below (consumed by positionToMinutes)
      };
    }

    function tomorrowZoneStyle(row) {
      const total = rowStripMinutes(row);
      const tomorrowPct = ((total - MINUTES_PER_DAY) / total) * 100;
      return {
        right: 0,
        width: `${tomorrowPct}%`,
      };
    }

    /** Place the dashed divider exactly at the 24h mark within the strip. */
    function midnightDividerStyle(row) {
      const total = rowStripMinutes(row);
      const todayPct = (MINUTES_PER_DAY / total) * 100;
      return { left: `${todayPct}%` };
    }

    function barTooltip(row) {
      const startStr = formatTime(row.startMinutesLocal);
      const endMin = (row.startMinutesLocal + row.timeoutMinutes) % MINUTES_PER_DAY;
      const endStr = formatTime(endMin);
      const suffix = rowOverflowsMidnight(row) ? ' (next day)' : '';
      return `${row.peripheralName} · ${startStr} → ${endStr}${suffix} · ${row.timeoutMinutes} min`;
    }

    function barStyle(row) {
      const total = rowStripMinutes(row);
      const left = (row.startMinutesLocal / total) * 100;
      const width = (row.timeoutMinutes / total) * 100;
      return {
        left: `${left}%`,
        width: `${Math.min(width, 100 - left)}%`,
      };
    }

    function onStripClick(row, ev) {
      const strip = ev.currentTarget;
      const m = positionToMinutes(ev.clientX, strip);
      if (m === null) return; // click was in the tomorrow tail — ignore
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
      if (m === null) return; // dragged into tomorrow tail — keep current value
      startMinutesOverrides.value = { ...startMinutesOverrides.value, [dragJobId.value]: m };
    }

    function onMouseUp() {
      dragJobId.value = null;
      dragStripEl.value = null;
    }

    function toggleDay(row, dayValue) {
      const current = (dayOverrides.value[row.jobId] ?? row.selectedDays).slice();
      const idx = current.indexOf(dayValue);
      if (idx >= 0) {
        current.splice(idx, 1);
      } else {
        current.push(dayValue);
      }
      current.sort((a, b) => a - b);
      dayOverrides.value = { ...dayOverrides.value, [row.jobId]: current };
    }

    function toggleDefaultDay(dayValue) {
      const current = defaultDays.value.slice();
      const idx = current.indexOf(dayValue);
      if (idx >= 0) {
        current.splice(idx, 1);
      } else {
        current.push(dayValue);
      }
      current.sort((a, b) => a - b);
      defaultDays.value = current;
    }

    function applyDefaultDaysToAll() {
      const template = defaultDays.value.slice().sort((a, b) => a - b);
      const next = { ...dayOverrides.value };
      for (const row of rows.value) {
        next[row.jobId] = template.slice();
      }
      dayOverrides.value = next;
    }

    /**
     * Build the DOW field for a 5-field standard cron: Unix numbers (0..6, 0=Sun).
     * `daysQuartz` holds Quartz numbers (1..7) so subtract 1 per entry.
     */
    function buildDowField(daysQuartz) {
      if (!daysQuartz || daysQuartz.length === 0) return '*';
      return daysQuartz
        .slice()
        .sort((a, b) => a - b)
        .map(d => d - 1)
        .join(',');
    }

    async function saveSchedule() {
      saving.value = true;
      const startOv = startMinutesOverrides.value;
      const dayOv = dayOverrides.value;
      try {
        for (const row of rows.value) {
          const dow = buildDowField(row.selectedDays);
          const { hourUtc, minuteUtc } = localMinutesToUtc(row.startMinutesLocal);
          const newExpression = buildCron(minuteUtc, hourUtc, dow);
          const hasTrigger = !!row.triggerId;
          const userTouched = startOv[row.jobId] !== undefined || dayOv[row.jobId] !== undefined;

          // Skip jobs that have no trigger AND the user didn't change anything —
          // we'd otherwise create an unintended 00:00 trigger every time the
          // schedule is saved for unrelated rows.
          if (!hasTrigger && !userTouched) continue;

          // Skip if nothing actually changed (only meaningful when a trigger exists).
          if (hasTrigger && row.triggerExpression === newExpression) continue;

          // For new triggers, omit `id` so GORM (cascade: all-delete-orphan)
          // creates a fresh CronTrigger for the job.
          const cronTriggerInput = hasTrigger
            ? [{ id: row.triggerId, expression: newExpression }]
            : [{ expression: newExpression }];

          const { mutate } = jobUpdateMutation;
          await mutate({
            id: row.jobId,
            job: { cronTriggers: cronTriggerInput },
          });
        }
        await refetch();
        startMinutesOverrides.value = {};
        dayOverrides.value = {};
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
      MINUTES_PER_DAY,
      loading,
      saving,
      rows,
      dayOptions,
      barStyle,
      barTooltip,
      stripContentStyle,
      tomorrowZoneStyle,
      midnightDividerStyle,
      rowOverflowsMidnight,
      rowStripMinutes,
      formatTime,
      onStripClick,
      onBarMouseDown,
      toggleDay,
      defaultDays,
      toggleDefaultDay,
      applyDefaultDaysToAll,
      saveSchedule,
    };
  },
});
</script>

<style scoped>
.scheduler-label-col {
  width: 200px;
  min-width: 200px;
}
.scheduler-days-col {
  width: 184px;
  min-width: 184px;
  padding-left: 8px;
  padding-right: 8px;
}
.day-chip-group {
  display: flex;
  gap: 3px;
  justify-content: flex-start;
}
.day-chip {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  color: #555;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s, color 0.15s;
}
.day-chip:hover {
  background: rgba(0, 0, 0, 0.12);
}
.day-chip--on {
  background: #26a69a;
  color: white;
}
.day-chip--on:hover {
  background: #00897b;
}
.day-chip--disabled {
  opacity: 0.55;
}
.scheduler-strip-wrap {
  flex: 1 1 0;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}
.scheduler-strip-header,
.scheduler-strip {
  height: 36px;
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
  width: 100%;
}
.scheduler-strip {
  position: relative;
}
.strip-hour {
  flex: 1 1 0;
  min-width: 0;
  font-size: 10px;
  text-align: center;
  color: #999;
  white-space: nowrap;
}
.scheduler-row {
  padding-top: 8px;
  padding-bottom: 8px;
}
.scheduler-row:hover .scheduler-strip {
  background-color: rgba(0, 0, 0, 0.02);
}
.scheduler-row--disabled {
  opacity: 0.55;
}
.scheduler-strip__tomorrow {
  position: absolute;
  top: 0;
  height: 100%;
  background: repeating-linear-gradient(
    45deg,
    rgba(244, 81, 30, 0.08),
    rgba(244, 81, 30, 0.08) 6px,
    rgba(244, 81, 30, 0.16) 6px,
    rgba(244, 81, 30, 0.16) 12px
  );
  border-radius: 0 4px 4px 0;
  pointer-events: none;
  z-index: 0;
}
.scheduler-strip__midnight-divider {
  position: absolute;
  top: -4px;
  bottom: -4px;
  /* `left` is set inline by midnightDividerStyle to MINUTES_PER_DAY/totalStripMinutes %. */
  width: 0;
  border-left: 2px dashed rgba(244, 81, 30, 0.85);
  pointer-events: none;
  z-index: 1;
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
  z-index: 2;
  overflow: hidden;
}
.scheduler-bar--disabled {
  background: linear-gradient(135deg, #9e9e9e 0%, #757575 100%);
}
.scheduler-bar--overflow {
  background: linear-gradient(135deg, #26a69a 0%, #00897b 60%, #ff7043 60%, #f4511e 100%);
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
  white-space: nowrap;
  padding: 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.bar-label-next {
  opacity: 0.85;
  font-style: italic;
  margin-left: 2px;
}
</style>
