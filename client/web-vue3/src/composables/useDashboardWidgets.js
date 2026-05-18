import { useAppConfigStore } from 'src/store/app-config.store';

/**
 * Canonical catalog of every widget that can appear on the dashboard.
 *
 * Single source of truth used by:
 *   - DashboardActions.vue → iterates and renders the visible ones
 *   - Settings.vue         → renders a checkbox per item, bound to the
 *                            per-user `hiddenWidgets` set
 *
 * Each entry:
 *   - id            stable identifier persisted in user prefs (never rename)
 *   - label         human-readable name shown in Settings
 *   - section       'quickAccess' (top row) | 'monitoring' (bottom row)
 *   - kind          'actionCard' (header + button group) | 'component' (single Vue component)
 *   - component     for kind === 'component': component name in the parent's `components` map
 *   - props         () => object — factory so values stay reactive when appConfig updates
 *   - actionCard    for kind === 'actionCard': {title, icon, cardClass, actions:[{label,icon,route}]}
 *   - roleGate      optional list of roles; the parent applies the gate (no per-widget auth here)
 *   - defaultVisible if false, widget is hidden by default (currently unused — all default to true)
 *
 * Adding a new dashboard widget = adding an entry here. No further wiring needed
 * beyond registering the component in DashboardActions.vue's `components` map.
 */
export function useDashboardWidgets() {
	const appConfig = useAppConfigStore();

	const z = (key) => appConfig.getNumber(key);

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
					{ label: 'Interior', icon: 'mdi-home-outline', route: `/zones/${z('ui.zone.int.id')}?category=LIGHT` },
					{ label: 'Exterior', icon: 'mdi-home-city-outline', route: `/zones/${z('ui.zone.ext.id')}?category=LIGHT` },
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
					{ label: 'Parter', icon: 'mdi-stairs-down', route: `/zones/${z('ui.zone.parter.id')}?category=HEAT` },
					{ label: 'Etaj', icon: 'mdi-stairs-up', route: `/zones/${z('ui.zone.etaj.id')}?category=HEAT` },
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
					{ label: 'Interior', icon: 'mdi-home-outline', route: `/zones/${z('ui.zone.int.id')}?category=SWITCH` },
					{ label: 'Exterior', icon: 'mdi-home-city-outline', route: `/zones/${z('ui.zone.ext.id')}?category=SWITCH` },
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
					{ label: 'Interior', icon: 'mdi-home-thermometer-outline', route: `/zones/${z('ui.zone.int.id')}?category=TEMP` },
					{ label: 'Exterior', icon: 'mdi-thermometer', route: `/zones/${z('ui.zone.ext.id')}?category=TEMP` },
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
			props: () => ({ deviceId: z('ui.device.meteo_station.id'), locationName: 'Halchiu, Romania' }),
		},
		{
			id: 'solar_plant',
			label: 'Solar Plant',
			section: 'monitoring',
			kind: 'component',
			component: 'solar-plant-widget',
			defaultVisible: true,
			props: () => ({
				deviceId: z('ui.device.solar_plant.id'),
				meterDeviceId: z('ui.device.solar_meter.id'),
			}),
		},
		{
			id: 'heat_pump',
			label: 'Heat Pump',
			section: 'monitoring',
			kind: 'component',
			component: 'nibe-heat-pump-widget',
			defaultVisible: true,
			props: () => ({ deviceId: z('ui.device.heat_pump.id') }),
		},
		{
			id: 'navimow',
			label: 'Navimow Mower',
			section: 'monitoring',
			kind: 'component',
			component: 'navimow-widget',
			defaultVisible: true,
			props: () => ({ deviceId: z('ui.device.navimow.id') }),
		},
	];
}
