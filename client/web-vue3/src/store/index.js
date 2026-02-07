import { createPinia } from 'pinia';

/*
 * When using Pinia with Quasar, simply export the Pinia instance
 */

export default function (/* { ssrContext } */) {
	const pinia = createPinia();
	return pinia;
}
