import {boot} from 'quasar/wrappers';
import {createI18n} from 'vue-i18n';

const messages = loadLocaleMessages();
const fallback = process.env.VUE_APP_I18N_FALLBACK_LOCALE || 'en';

const i18n = createI18n({
	legacy: false, // Use Composition API mode (v11+ recommended)
	globalInjection: true, // Inject $t, $d, $n, $tm globally (allows $t() in templates)
	locale: initialLocale(Object.keys(messages), fallback),
	fallbackLocale: fallback,
	messages,
});

function loadLocaleMessages() {
	const locales = require.context('../i18n/locales', true, /[A-Za-z0-9-_,\s]+\.json$/i);
	const loaded = {};
	locales.keys().forEach(key => {
		const matched = key.match(/([A-Za-z0-9-_]+)\./i);
		if (matched && matched.length > 1) {
			const locale = matched[1];
			loaded[locale] = locales(key);
		}
	});
	return loaded;
}

/**
 * Initial locale on app load: the stored user preference wins; otherwise the
 * browser language; otherwise the env/default fallback. (Kept inline here to
 * avoid an import cycle with locale.service, which imports this module.)
 */
function initialLocale(available, fallbackLocale) {
	try {
		const stored = JSON.parse(localStorage.getItem('currentUser') || 'null');
		if (stored?.language && available.includes(stored.language)) {
			return stored.language;
		}
	} catch (e) {
		// ignore malformed storage
	}
	const browser = (navigator.language || '').toLowerCase().split('-')[0];
	if (available.includes(browser)) {
		return browser;
	}
	return process.env.VUE_APP_I18N_LOCALE || fallbackLocale;
}
export default boot(({ app }) => {
	// Set i18n instance on app
	app.use(i18n);
});

export { i18n };
