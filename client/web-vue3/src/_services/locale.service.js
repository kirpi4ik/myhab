import {i18n} from '@/boot/i18n';

/**
 * Centralizes UI-language resolution and application.
 *
 * Resolution order (per product requirement):
 *   1. the user's explicitly chosen language (when set on their account),
 *   2. otherwise the browser language (navigator.language),
 *   3. otherwise the i18n fallback ('en').
 *
 * Only languages we actually ship translations for are considered.
 */

/** Native display labels for the locales we ship. */
const LOCALE_LABELS = {en: 'English', ro: 'Română'};

/** List of locale codes we have translations for (e.g. ['en','ro']). */
export function availableLocales() {
	const messages = i18n.global.messages?.value ?? i18n.global.messages ?? {};
	return Object.keys(messages);
}

/** Locale options for a select, including an "automatic (browser)" entry (value: null). */
export function localeOptions(autoLabel) {
	const opts = availableLocales().map(code => ({value: code, label: LOCALE_LABELS[code] || code}));
	return [{value: null, label: autoLabel}, ...opts];
}

/** The browser's preferred language reduced to a base code we support, or null. */
function browserLocale() {
	const avail = availableLocales();
	const base = (navigator.language || '').toLowerCase().split('-')[0];
	return avail.includes(base) ? base : null;
}

/**
 * Resolve the effective locale: explicit user language > browser > fallback.
 * @param {string|null} userLanguage value stored on the user's account (may be null)
 */
export function resolveLocale(userLanguage) {
	const avail = availableLocales();
	if (userLanguage && avail.includes(userLanguage)) {
		return userLanguage;
	}
	return browserLocale() || i18n.global.fallbackLocale?.value || 'en';
}

/** Apply a concrete locale code to the running app. */
export function applyLocale(localeCode) {
	if (localeCode) {
		i18n.global.locale.value = localeCode;
	}
}

/** Resolve from a user preference (with browser fallback) and apply it. */
export function applyUserLocale(userLanguage) {
	applyLocale(resolveLocale(userLanguage));
}
