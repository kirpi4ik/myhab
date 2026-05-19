import { useAppConfigStore } from 'src/store/app-config.store';

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
 */
export function useDashboardWidgets() {
	const appConfig = useAppConfigStore();
	const n = (key) => appConfig.getNumber(key);

	return [
		// ──────── Quick access: action cards (header + button row) ────────
		{
			id: 'lighting',
			label: 'Lighting Quick Access',
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			actionCard: {
				title: 'Iluminat',
				icon: 'fas fa-lightbulb',
				cardClass: 'card-light',
				actions: [
					{ label: 'Interior', icon: 'mdi-home-outline', route: `/zones/${n('specialZones.int.id')}?category=LIGHT` },
					{ label: 'Exterior', icon: 'mdi-home-city-outline', route: `/zones/${n('specialZones.ext.id')}?category=LIGHT` },
				],
			},
		},
		{
			id: 'climate',
			label: 'Climate Quick Access',
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			actionCard: {
				title: 'Climatizare',
				icon: 'fas fa-fire',
				cardClass: 'card-heat',
				actions: [
					{ label: 'Parter', icon: 'mdi-stairs-down', route: `/zones/${n('specialZones.parter.id')}?category=HEAT` },
					{ label: 'Etaj', icon: 'mdi-stairs-up', route: `/zones/${n('specialZones.etaj.id')}?category=HEAT` },
				],
			},
		},
		{
			id: 'switches',
			label: 'Switches Quick Access',
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			actionCard: {
				title: 'Prize',
				icon: 'mdi-electric-switch',
				cardClass: 'card-switch',
				actions: [
					{ label: 'Interior', icon: 'mdi-home-outline', route: `/zones/${n('specialZones.int.id')}?category=SWITCH` },
					{ label: 'Exterior', icon: 'mdi-home-city-outline', route: `/zones/${n('specialZones.ext.id')}?category=SWITCH` },
				],
			},
		},
		{
			id: 'temperature',
			label: 'Temperature Quick Access',
			section: 'quickAccess',
			kind: 'actionCard',
			defaultVisible: true,
			actionCard: {
				title: 'Temperatura',
				icon: 'fas fa-temperature-high',
				cardClass: 'card-temp',
				actions: [
					{ label: 'Interior', icon: 'mdi-home-thermometer-outline', route: `/zones/${n('specialZones.int.id')}?category=TEMP` },
					{ label: 'Exterior', icon: 'mdi-thermometer', route: `/zones/${n('specialZones.ext.id')}?category=TEMP` },
				],
			},
		},

		// ──────── Quick access: peripheral component cards ────────
		{
			id: 'peripheral_lock',
			label: 'Door Lock',
			section: 'quickAccess',
			kind: 'component',
			component: 'peripheral-lock',
			defaultVisible: true,
			props: () => ({}),
		},
		{
			id: 'sprinklers',
			label: 'Sprinklers',
			section: 'quickAccess',
			kind: 'component',
			component: 'sprinklers-dash-component',
			defaultVisible: true,
			props: () => ({ peripheral: { state: true } }),
		},
		{
			id: 'water_pump',
			label: 'Water Pump',
			section: 'quickAccess',
			kind: 'component',
			component: 'water-pump',
			defaultVisible: true,
			props: () => ({ peripheral: { state: true } }),
		},

		// ──────── Monitoring: device telemetry widgets ────────
		{
			id: 'meteo_station',
			label: 'Weather Station',
			section: 'monitoring',
			kind: 'component',
			component: 'meteo-station-card',
			defaultVisible: true,
			props: () => ({ deviceId: n('specialDevices.meteoStation.deviceId'), locationName: 'Halchiu, Romania' }),
		},
		{
			id: 'solar_plant',
			label: 'Solar Plant',
			section: 'monitoring',
			kind: 'component',
			component: 'solar-plant-widget',
			defaultVisible: true,
			props: () => ({
				deviceId: n('specialDevices.solarPlant.deviceId'),
				meterDeviceId: n('specialDevices.solarMeter.deviceId'),
			}),
		},
		{
			id: 'heat_pump',
			label: 'Heat Pump',
			section: 'monitoring',
			kind: 'component',
			component: 'nibe-heat-pump-widget',
			defaultVisible: true,
			// Different from specialDevices.heatPump.id (a peripheral) — this is the
			// myhab Device row id used by NibeHeatPumpWidget's GraphQL query.
			props: () => ({ deviceId: n('specialDevices.heatPump.deviceId') }),
		},
		{
			id: 'navimow',
			label: 'Navimow Mower',
			section: 'monitoring',
			kind: 'component',
			component: 'navimow-widget',
			defaultVisible: true,
			props: () => ({ deviceId: n('specialDevices.navimow.deviceId') }),
		},
	];
}
