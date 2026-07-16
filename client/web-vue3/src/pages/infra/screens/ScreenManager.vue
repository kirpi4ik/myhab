<template>
  <q-page padding>
    <q-card flat bordered>
      <q-card-section class="row items-center q-gutter-sm">
        <q-icon name="mdi-monitor-dashboard" size="28px" />
        <div class="text-h6">Dashboard screens</div>
        <q-space />
        <q-btn color="primary" icon="mdi-plus" label="Add screen" @click="openAddDialog" />
        <q-btn flat color="primary" icon="mdi-file-import" label="Import legacy SVG" @click="openImportDialog" />
      </q-card-section>

      <q-separator />

      <!-- Editor mode -->
      <q-card-section v-if="editingScreen">
        <div class="text-subtitle1 q-mb-sm">
          Editing layout: <b>{{ editingScreen.name }}</b>
          <span v-if="!editingScreen.backgroundWidth" class="text-orange q-ml-sm">
            (no background uploaded — using default canvas)
          </span>
        </div>
        <ScreenEditorCanvas
          :screen="editingScreen"
          :peripherals="peripherals"
          :bg-url="bgUrls[editingScreen.id] || null"
          @saved="loadScreens"
          @close="editingScreen = null"
        />
      </q-card-section>

      <!-- List mode -->
      <q-card-section v-else>
        <q-table
          :rows="screens"
          :columns="columns"
          row-key="id"
          :pagination="{ rowsPerPage: 0 }"
          hide-bottom
          :loading="loading"
          flat
        >
          <template #body-cell-preview="cellProps">
            <q-td :props="cellProps">
              <img
                v-if="bgUrls[cellProps.row.id]"
                :src="bgUrls[cellProps.row.id]"
                class="bg-thumb"
                alt="background"
              />
              <q-chip v-else dense color="grey-4" text-color="grey-8" label="no background" />
            </q-td>
          </template>

          <template #body-cell-ordinal="cellProps">
            <q-td :props="cellProps">
              <q-btn dense flat size="sm" icon="mdi-arrow-up" :disable="cellProps.rowIndex === 0" @click="move(cellProps.rowIndex, -1)" />
              <q-btn dense flat size="sm" icon="mdi-arrow-down" :disable="cellProps.rowIndex === screens.length - 1" @click="move(cellProps.rowIndex, 1)" />
            </q-td>
          </template>

          <template #body-cell-enabled="cellProps">
            <q-td :props="cellProps">
              <q-toggle
                :model-value="cellProps.row.enabled"
                dense
                @update:model-value="(val) => toggleEnabled(cellProps.row, val)"
              />
            </q-td>
          </template>

          <template #body-cell-widgets="cellProps">
            <q-td :props="cellProps">{{ widgetCount(cellProps.row) }}</q-td>
          </template>

          <template #body-cell-actions="cellProps">
            <q-td :props="cellProps" class="q-gutter-xs">
              <q-btn dense flat color="primary" icon="mdi-pencil-ruler" @click="editLayout(cellProps.row)">
                <q-tooltip>Edit layout</q-tooltip>
              </q-btn>
              <q-btn dense flat icon="mdi-rename" @click="openRenameDialog(cellProps.row)">
                <q-tooltip>Rename</q-tooltip>
              </q-btn>
              <q-btn dense flat icon="mdi-image-plus" @click="openBackgroundDialog(cellProps.row)">
                <q-tooltip>Upload background</q-tooltip>
              </q-btn>
              <q-btn dense flat color="negative" icon="mdi-delete" @click="confirmDelete(cellProps.row)">
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-td>
          </template>
        </q-table>
      </q-card-section>
    </q-card>

    <!-- Add / rename dialog -->
    <q-dialog v-model="nameDialog.show">
      <q-card style="min-width: 340px">
        <q-card-section class="text-h6">{{ nameDialog.screen ? 'Rename screen' : 'Add screen' }}</q-card-section>
        <q-card-section>
          <q-input v-model="nameDialog.name" label="Name" autofocus @keyup.enter="submitNameDialog" />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn color="primary" label="Save" :disable="!nameDialog.name?.trim()" @click="submitNameDialog" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Background upload dialog -->
    <q-dialog v-model="bgDialog.show">
      <q-card style="min-width: 340px">
        <q-card-section class="text-h6">Upload background — {{ bgDialog.screen?.name }}</q-card-section>
        <q-card-section>
          <q-file v-model="bgDialog.file" label="Image (jpeg/png/webp, max 5MB)" accept="image/jpeg,image/png,image/webp" outlined />
          <q-select
            v-model="bgDialog.preset"
            class="q-mt-sm"
            :options="presetOptions"
            label="Target canvas"
            outlined
            dense
            emit-value
            map-options
          />
          <div class="text-caption text-grey-7 q-mt-sm">
            With a device preset the image is scaled to exactly that canvas size (no borders;
            slight aspect distortion if the ratio differs), so screens stay consistent regardless
            of the original image size/ratio. When the canvas size changes, existing widgets are
            rescaled proportionally to keep their positions.
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn color="primary" label="Upload" :disable="!bgDialog.file" :loading="bgDialog.uploading" @click="submitBackground" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Import legacy SVG dialog -->
    <q-dialog v-model="importDialog.show">
      <q-card style="min-width: 340px">
        <q-card-section class="text-h6">Import legacy SVG</q-card-section>
        <q-card-section>
          <q-input v-model="importDialog.name" label="New screen name" class="q-mb-sm" />
          <q-file v-model="importDialog.file" label="Legacy .svg file" accept=".svg,image/svg+xml" outlined />
          <div class="text-caption text-grey-7 q-mt-sm">
            Extracts the embedded floor plan and seeds widgets from asset-* element ids.
            The screen is created disabled — review and adjust in the editor before enabling.
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn
            color="primary"
            label="Import"
            :disable="!importDialog.file || !importDialog.name?.trim()"
            :loading="importDialog.importing"
            @click="submitImport"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import {
  DASHBOARD_SCREENS,
  SCREEN_CREATE,
  SCREEN_UPDATE,
  SCREEN_DELETE,
  SCREEN_REORDER,
} from '@/graphql/queries';
import { screenService } from '@/_services';
import { useNotifications, usePeripheralState } from '@/composables';
import ScreenEditorCanvas from './ScreenEditorCanvas.vue';

const $q = useQuasar();
const { client } = useApolloClient();
const { notifyError, notifySuccess } = useNotifications();
const { peripherals, loadPeripherals } = usePeripheralState();

const screens = ref([]);
const bgUrls = ref({});
const loading = ref(false);
const editingScreen = ref(null);

const columns = [
  { name: 'preview', label: '', field: 'id', align: 'left' },
  { name: 'name', label: 'Name', field: 'name', align: 'left' },
  { name: 'widgets', label: 'Widgets', field: 'layoutJson', align: 'center' },
  { name: 'enabled', label: 'Enabled', field: 'enabled', align: 'center' },
  { name: 'ordinal', label: 'Order', field: 'ordinal', align: 'center' },
  { name: 'actions', label: 'Actions', field: 'id', align: 'right' },
];

const widgetCount = (screen) => {
  try {
    return JSON.parse(screen.layoutJson || '{}').widgets?.length || 0;
  } catch {
    return '?';
  }
};

const revokeBgUrls = () => {
  Object.values(bgUrls.value).forEach((url) => URL.revokeObjectURL(url));
  bgUrls.value = {};
};

const loadScreens = async () => {
  loading.value = true;
  try {
    const response = await client.query({
      query: DASHBOARD_SCREENS,
      variables: { enabledOnly: false },
      fetchPolicy: 'network-only',
    });
    screens.value = (response.data.dashboardScreens || []).map((s) => ({ ...s }));
    // refresh editor copy if open
    if (editingScreen.value) {
      editingScreen.value = screens.value.find((s) => s.id === editingScreen.value.id) || null;
    }
    revokeBgUrls();
    await Promise.all(
      screens.value.map(async (s) => {
        const url = await screenService.fetchBackgroundBlobUrl(s.id, s.tsUpdated);
        if (url) bgUrls.value[s.id] = url;
      })
    );
  } catch (err) {
    notifyError(`Failed to load screens: ${err.message}`);
  } finally {
    loading.value = false;
  }
};

// --- add / rename ---
const nameDialog = ref({ show: false, name: '', screen: null });
const openAddDialog = () => {
  nameDialog.value = { show: true, name: '', screen: null };
};
const openRenameDialog = (screen) => {
  nameDialog.value = { show: true, name: screen.name, screen };
};
const submitNameDialog = async () => {
  const { name, screen } = nameDialog.value;
  if (!name?.trim()) return;
  try {
    if (screen) {
      await client.mutate({
        mutation: SCREEN_UPDATE,
        variables: { id: screen.id, dashboardScreen: { name: name.trim() } },
      });
    } else {
      await client.mutate({
        mutation: SCREEN_CREATE,
        variables: {
          dashboardScreen: { name: name.trim(), ordinal: screens.value.length, enabled: false },
        },
      });
    }
    nameDialog.value.show = false;
    notifySuccess(screen ? 'Screen renamed' : 'Screen created');
    await loadScreens();
  } catch (err) {
    notifyError(err.message);
  }
};

// --- enable / reorder / delete ---
const toggleEnabled = async (screen, value) => {
  try {
    await client.mutate({
      mutation: SCREEN_UPDATE,
      variables: { id: screen.id, dashboardScreen: { enabled: value } },
    });
    screen.enabled = value;
  } catch (err) {
    notifyError(err.message);
  }
};

const move = async (index, delta) => {
  const ids = screens.value.map((s) => s.id);
  const [moved] = ids.splice(index, 1);
  ids.splice(index + delta, 0, moved);
  try {
    await client.mutate({ mutation: SCREEN_REORDER, variables: { ids } });
    await loadScreens();
  } catch (err) {
    notifyError(err.message);
  }
};

const confirmDelete = (screen) => {
  $q.dialog({
    title: 'Delete screen',
    message: `Delete "${screen.name}" and its layout? This cannot be undone.`,
    cancel: true,
    persistent: true,
  }).onOk(async () => {
    try {
      // gorm-graphql delete reports failures via {success,error} instead of throwing
      const response = await client.mutate({ mutation: SCREEN_DELETE, variables: { id: screen.id } });
      const result = response.data?.dashboardScreenDelete;
      if (!result?.success) {
        notifyError(`Failed to delete screen: ${result?.error || 'unknown error'}`);
        return;
      }
      notifySuccess('Screen deleted');
      await loadScreens();
    } catch (err) {
      notifyError(err.message);
    }
  });
};

// --- background upload ---
const presetOptions = [
  { label: 'Original size (no scaling)', value: null },
  { label: 'Mobile — 780 × 500', value: 'mobile' },
  { label: 'Tablet — 1280 × 800', value: 'tablet' },
];
const bgDialog = ref({ show: false, screen: null, file: null, preset: null, uploading: false });
const openBackgroundDialog = (screen) => {
  bgDialog.value = { show: true, screen, file: null, preset: null, uploading: false };
};
const submitBackground = async () => {
  bgDialog.value.uploading = true;
  try {
    const result = await screenService.uploadBackground(bgDialog.value.screen.id, bgDialog.value.file, bgDialog.value.preset);
    notifySuccess(`Background uploaded (${result.width}x${result.height})`);
    bgDialog.value.show = false;
    await loadScreens();
  } catch (err) {
    notifyError(err.message);
  } finally {
    bgDialog.value.uploading = false;
  }
};

// --- legacy import ---
const importDialog = ref({ show: false, name: '', file: null, importing: false });
const openImportDialog = () => {
  importDialog.value = { show: true, name: '', file: null, importing: false };
};
const submitImport = async () => {
  importDialog.value.importing = true;
  try {
    const result = await screenService.importLegacySvg(importDialog.value.file, importDialog.value.name.trim());
    notifySuccess(`Imported: ${result.widgets} widgets seeded — open the editor to adjust`);
    importDialog.value.show = false;
    await loadScreens();
  } catch (err) {
    notifyError(err.message);
  } finally {
    importDialog.value.importing = false;
  }
};

// --- edit layout ---
const editLayout = (screen) => {
  editingScreen.value = screen;
};

onMounted(async () => {
  await Promise.all([loadScreens(), loadPeripherals().catch(() => {})]);
});

onUnmounted(revokeBgUrls);
</script>

<style scoped>
.bg-thumb {
  max-width: 90px;
  max-height: 56px;
  border-radius: 3px;
  display: block;
}
</style>
