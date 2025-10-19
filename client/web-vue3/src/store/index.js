import { createPinia } from 'pinia';

/*
 * When using Pinia with Quasar, simply export the Pinia instance
 */

console.log('Store module loaded - about to export Pinia factory function');

export default function (/* { ssrContext } */) {
	console.log('Creating Pinia store...');

	const pinia = createPinia();

	console.log('Pinia store created:', pinia);
	return pinia;
}
