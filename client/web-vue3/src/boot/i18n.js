import {boot} from 'quasar/wrappers';
import {createI18n} from 'vue-i18n';

const i18n = createI18n({
	legacy: false, // Use Composition API mode (v11+ recommended)
	globalInjection: true, // Inject $t, $d, $n, $tm globally (allows $t() in templates)
	locale: process.env.VUE_APP_I18N_LOCALE || 'en',
	fallbackLocale: process.env.VUE_APP_I18N_FALLBACK_LOCALE || 'en',
	messages: loadLocaleMessages(),
});

function loadLocaleMessages() {
	const locales = require.context('../i18n/locales', true, /[A-Za-z0-9-_,\s]+\.json$/i);
	const messages = {};
	locales.keys().forEach(key => {
		const matched = key.match(/([A-Za-z0-9-_]+)\./i);
		if (matched && matched.length > 1) {
			const locale = matched[1];
			messages[locale] = locales(key);
		}
	});
	return messages;
}
export default boot(({ app }) => {
	// Set i18n instance on app
	app.use(i18n);
});

export { i18n };
