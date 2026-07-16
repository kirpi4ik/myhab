/**
 * Pure helpers for dashboard screen widgets (viewer + editor).
 * Ports of the category/state rules previously in useSvgInteraction.js
 * (getAssetClass + the TEMP/LUMINOSITY text formatting).
 */

/**
 * CSS class for a widget based on peripheral category, state and device status.
 * Note the intentional MOTION inversion (legacy behavior).
 */
export function getWidgetClass(categoryName, state, deviceStatus) {
	switch (categoryName) {
		case 'LIGHT':
			return state ? 'bulb-on' : 'bulb-off';
		case 'HEAT':
			return state ? 'heat-on' : 'heat-off';
		case 'MOTION':
			return state ? 'motion-off' : 'motion-on';
		case 'TEMP':
			return deviceStatus === 'OFFLINE' ? 'device-offline' : '';
		case 'DOOR_LOCK':
			return 'lock';
		case 'LUMINOSITY':
			return 'luminosity-text';
		default:
			return '';
	}
}

/**
 * Live text for text widgets. TEMP: 3-digit integer values are tenths of a
 * degree (250 -> 25.0). LUMINOSITY: value is tenths of a percent.
 */
export function formatWidgetValue(categoryName, portValue) {
	if (portValue == null || portValue === '') return '';
	if (categoryName === 'TEMP') {
		let degree = portValue;
		if (String(degree).length === 3 && !String(degree).includes('.')) {
			degree = degree / 10;
		}
		return `${degree}℃`;
	}
	if (categoryName === 'LUMINOSITY') {
		return `${portValue / 10}%`;
	}
	return String(portValue);
}

/** Default widget kind when placing a peripheral of the given category. */
export function defaultKindForCategory(categoryName) {
	switch (categoryName) {
		case 'HEAT':
		case 'MOTION':
			return 'zone';
		case 'TEMP':
		case 'LUMINOSITY':
			return 'text';
		default:
			return 'marker';
	}
}

/** MDI icon name for marker widgets, per category. */
export function defaultIconForCategory(categoryName) {
	switch (categoryName) {
		case 'LIGHT':
			return 'mdi-lightbulb';
		case 'DOOR_LOCK':
			return 'mdi-lock';
		case 'MOTION':
			return 'mdi-motion-sensor';
		case 'HEAT':
			return 'mdi-radiator';
		default:
			return 'mdi-circle-medium';
	}
}

/** True when clicking the widget should trigger an action. */
export function isActionableCategory(categoryName) {
	return ['LIGHT', 'HEAT', 'DOOR_LOCK'].includes(categoryName);
}

/**
 * Resolve the effective MDI icon name for a marker widget:
 * widget override -> category default (PeripheralCategory.icon) -> built-in fallback.
 */
export function resolveIconName(widget, peripheral) {
	return widget?.icon || peripheral?.category?.icon || defaultIconForCategory(peripheral?.category?.name);
}

/** 'mdi-lightbulb-on' -> 'mdiLightbulbOn' (export name in @quasar/extras/mdi-v6). */
export function mdiNameToExport(name) {
	if (!name) return null;
	return name
		.split('-')
		.map((part, i) => (i === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
		.join('');
}

/** 'mdiLightbulbOn' -> 'mdi-lightbulb-on' (webfont class / stored name). */
export function mdiExportToName(exportName) {
	return exportName.replace(/([A-Z]|\d+)/g, (m) => `-${m.toLowerCase()}`);
}
