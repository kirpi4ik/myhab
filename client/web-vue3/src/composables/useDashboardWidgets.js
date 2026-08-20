import { useAppConfigStore } from 'src/store/app-config.store';
import { i18n } from 'boot/i18n';

/**
 * Canonical catalog of every widget that can appear on the dashboard.
 *
 * Single source of truth used by:
 *   - DashboardActions.vue → iterates and renders the visible ones
 *   - Settings.vue         → renders a checkbox per item, bound to the
 *                            per-user `hiddenWidgets` set
 *
 * Keys are read from the git-backed ConfigProvider via `useAppConfigStore`.
 * They live in the same namespaces the backend already uses
 * (`specialDevices.*`, `grafana.*`, `ui.*`) so a single config repo entry
 * serves both sides — no duplication.
 *
 * Adding a new dashboard widget = adding an entry here + (if it references
 * a new entity) extending the config.yaml in the deployment's config repo.
 *
 * `requiredConfig` declares the configuration a widget cannot work without. It is what
 * lets the rest of the UI treat "unconfigured" as a first-class state instead of
 * discovering it as a failed GraphQL query:
 *   - DashboardActions renders a "not configured" card instead of mounting the widget,
 *     so no query with a null id is ever issued;
 *   - Settings disables the widget's checkbox and says which setting is missing;
 *   - the placeholder offers an inline picker to set it.
 * A new widget gets all three by declaring its keys here. `kind` selects the picker:
 * 'device' | 'peripheral' | 'zone' | 'text'.
 *
 * Every string here is translated through the global i18n instance rather than the
 * `useI18n()` composable: this runs inside a `computed`, not a component `setup()`.
 * Reading `t()` there still tracks the locale ref, so the whole registry re-evaluates
 * when the language changes.
 */
export function useDashboardWidgets() {
	const appConfig = useAppConfigStore();
	const n = (key) => appConfig.getNumber(key);
	const s = (key) => appConfig.get(key);
	const t = (key) => i18n.global.t(key);

	return [
		// ──────── Quick access: action cards (header + button row) ────────
		{
			id: 'lighting',
			label: t('dashboard.widgets.lighting'),
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialZones.int.id', label: t('dashboard.config.zone_int'), kind: 'zone' },
				{ key: 'specialZones.ext.id', label: t('dashboard.config.zone_ext'), kind: 'zone' },
			],
			actionCard: {
				title: t('dashboard.cards.lighting'),
				icon: 'fas fa-lightbulb',
				cardClass: 'card-light',
				actions: [
					{ label: t('dashboard.cards.interior'), icon: 'mdi-home-outline', route: `/zones/${n('specialZones.int.id')}?category=LIGHT` },
					{ label: t('dashboard.cards.exterior'), icon: 'mdi-home-city-outline', route: `/zones/${n('specialZones.ext.id')}?category=LIGHT` },
				],
			},
		},
		{
			id: 'climate',
			label: t('dashboard.widgets.climate'),
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialZones.parter.id', label: t('dashboard.config.zone_ground'), kind: 'zone' },
				{ key: 'specialZones.etaj.id', label: t('dashboard.config.zone_upper'), kind: 'zone' },
			],
			actionCard: {
				title: t('dashboard.cards.climate'),
				icon: 'fas fa-fire',
				cardClass: 'card-heat',
				actions: [
					{ label: t('dashboard.cards.ground_floor'), icon: 'mdi-stairs-down', route: `/zones/${n('specialZones.parter.id')}?category=HEAT` },
					{ label: t('dashboard.cards.upper_floor'), icon: 'mdi-stairs-up', route: `/zones/${n('specialZones.etaj.id')}?category=HEAT` },
				],
			},
		},
		{
			id: 'switches',
			label: t('dashboard.widgets.switches'),
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialZones.int.id', label: t('dashboard.config.zone_int'), kind: 'zone' },
				{ key: 'specialZones.ext.id', label: t('dashboard.config.zone_ext'), kind: 'zone' },
			],
			actionCard: {
				title: t('dashboard.cards.switches'),
				icon: 'mdi-electric-switch',
				cardClass: 'card-switch',
				actions: [
					{ label: t('dashboard.cards.interior'), icon: 'mdi-home-outline', route: `/zones/${n('specialZones.int.id')}?category=SWITCH` },
					{ label: t('dashboard.cards.exterior'), icon: 'mdi-home-city-outline', route: `/zones/${n('specialZones.ext.id')}?category=SWITCH` },
				],
			},
		},
		{
			id: 'temperature',
			label: t('dashboard.widgets.temperature'),
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialZones.int.id', label: t('dashboard.config.zone_int'), kind: 'zone' },
				{ key: 'specialZones.ext.id', label: t('dashboard.config.zone_ext'), kind: 'zone' },
			],
			actionCard: {
				title: t('dashboard.cards.temperature'),
				icon: 'fas fa-temperature-high',
				cardClass: 'card-temp',
				actions: [
					{ label: t('dashboard.cards.interior'), icon: 'mdi-home-thermometer-outline', route: `/zones/${n('specialZones.int.id')}?category=TEMP` },
					{ label: t('dashboard.cards.exterior'), icon: 'mdi-thermometer', route: `/zones/${n('specialZones.ext.id')}?category=TEMP` },
				],
			},
		},

		// ──────── Quick access: peripheral component cards ────────
		{
			id: 'peripheral_lock',
			label: t('dashboard.widgets.peripheral_lock'),
			section: 'quickAccess',
			kind: 'component',
			component: 'peripheral-lock',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.doorLockMain.peripheral.id', label: t('dashboard.config.peripheral_door_lock'), kind: 'peripheral' },
			],
			props: () => ({}),
		},
		{
			id: 'sprinklers',
			label: t('dashboard.widgets.sprinklers'),
			section: 'quickAccess',
			kind: 'component',
			component: 'sprinklers-dash-component',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialZones.lan.id', label: t('dashboard.config.zone_lawn'), kind: 'zone' },
				{ key: 'specialZones.garden.id', label: t('dashboard.config.zone_garden'), kind: 'zone' },
			],
			props: () => ({ peripheral: { state: true } }),
		},
		{
			id: 'water_pump',
			label: t('dashboard.widgets.water_pump'),
			section: 'quickAccess',
			kind: 'component',
			component: 'water-pump',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.water.peripheral.id', label: t('dashboard.config.peripheral_water_pump'), kind: 'peripheral' },
			],
			props: () => ({ peripheral: { state: true } }),
		},

		// ──────── Monitoring: device telemetry widgets ────────
		{
			id: 'meteo_station',
			label: t('dashboard.widgets.meteo_station'),
			section: 'monitoring',
			kind: 'component',
			component: 'meteo-station-card',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.meteoStation.deviceId', label: t('dashboard.config.device_meteo'), kind: 'device' },
			],
			// Config-driven: the location belongs to whoever installed myHAB. With no
			// value the card simply shows no place name.
			props: () => ({
				deviceId: n('specialDevices.meteoStation.deviceId'),
				locationName: s('ui.meteo.locationName'),
			}),
		},
		{
			id: 'solar_plant',
			label: t('dashboard.widgets.solar_plant'),
			section: 'monitoring',
			kind: 'component',
			component: 'solar-plant-widget',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.solarPlant.deviceId', label: t('dashboard.config.device_solar_inverter'), kind: 'device' },
				{ key: 'specialDevices.solarMeter.deviceId', label: t('dashboard.config.device_solar_meter'), kind: 'device' },
			],
			props: () => ({
				deviceId: n('specialDevices.solarPlant.deviceId'),
				meterDeviceId: n('specialDevices.solarMeter.deviceId'),
			}),
		},
		{
			id: 'heat_pump',
			label: t('dashboard.widgets.heat_pump'),
			section: 'monitoring',
			kind: 'component',
			component: 'nibe-heat-pump-widget',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.heatPump.deviceId', label: t('dashboard.config.device_heat_pump'), kind: 'device' },
			],
			// Different from specialDevices.heatPump.id (a peripheral) — this is the
			// myhab Device row id used by NibeHeatPumpWidget's GraphQL query.
			props: () => ({ deviceId: n('specialDevices.heatPump.deviceId') }),
		},
		{
			id: 'navimow',
			label: t('dashboard.widgets.navimow'),
			section: 'monitoring',
			kind: 'component',
			component: 'navimow-widget',
			defaultVisible: true,
			requiredConfig: [
				{ key: 'specialDevices.navimow.deviceId', label: t('dashboard.config.device_navimow'), kind: 'device' },
			],
			props: () => ({ deviceId: n('specialDevices.navimow.deviceId') }),
		},
	];
}
