import { createStore } from 'vuex';

import settings from './store.settings';
import ws from './store.ws';

// import example from './module-example'

/*
 * When using sourceFiles in quasar.config.js,
 * do NOT use the store() wrapper - export the factory function directly
 */

console.log('Store module loaded - about to export factory function');

export default function ({ router } = {}) {
	console.log('Creating Vuex store...');
	
	// Plugin to inject router into store (Vuex 4 compatible)
	const routerPlugin = (store) => {
		if (router) {
			store.router = router;
		}
	};
	
	const Store = createStore({
		modules: {
			settings,
			ws,
		},
		
		plugins: [routerPlugin],

		// enable strict mode (adds overhead!)
		// for dev mode and --debug builds only
		strict: process.env.DEV !== undefined ? process.env.DEV === 'true' : false,
	});
	
	// Vuex 4 compatibility shim: add a dummy .use() method
	// Quasar's generated code tries to call store.use() which doesn't exist in Vuex 4
	Store.use = function(plugin) {
		console.log('Store.use() called (compatibility shim for Vuex 4)');
		// In Vuex 4, plugins are registered during createStore
		// This is just a no-op to prevent errors
		if (typeof plugin === 'function') {
			plugin({ store: Store });
		}
		return Store;
	};
	
	console.log('Vuex store created:', Store);
	return Store;
}
