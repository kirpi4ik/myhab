import { createPinia } from 'pinia';

/*
 * When using Pinia with Quasar, simply export the Pinia instance
 */

if (process.env.DEV) {
	console.log('Store module loaded - about to export Pinia factory function');
}

export default function (/* { ssrContext } */) {
	if (process.env.DEV) {
		console.log('Creating Pinia store...');
	}

	const pinia = createPinia();

	if (process.env.DEV) {
		console.log('Pinia store created:', pinia);
	}
	return pinia;
}
