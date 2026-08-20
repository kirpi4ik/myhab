import { ref } from 'vue';
import { Utils } from '@/_helpers';

/**
 * Client-side view of the public demo deployment.
 *
 * `GET /api/public/demo` is anonymous and returns 404 on a normal deployment, so a
 * single call answers "is this the demo?" before anyone has logged in. Every demo
 * affordance in the UI keys off `state.enabled`, which means a production build is
 * visually unchanged without needing a build-time flag.
 *
 * The state is module-level and fetched once: whether a deployment is the demo cannot
 * change while the tab is open.
 */
const state = ref({
	enabled: false,
	loaded: false,
	idleMinutes: 20,
	credentials: [],
	lastReset: null,
	resetEnabled: false,
});

let inFlight = null;

async function load() {
	if (state.value.loaded) return state.value;
	if (inFlight) return inFlight;

	inFlight = (async () => {
		try {
			const response = await fetch(`${Utils.host()}/api/public/demo`, {
				headers: { Accept: 'application/json' },
			});
			if (response.ok) {
				const body = await response.json();
				state.value = { ...body, loaded: true };
			} else {
				// 404 is the normal answer everywhere except the demo.
				state.value = { ...state.value, enabled: false, loaded: true };
			}
		} catch (err) {
			// Never let this break app start-up; the demo extras simply stay hidden.
			state.value = { ...state.value, enabled: false, loaded: true };
		} finally {
			inFlight = null;
		}
		return state.value;
	})();

	return inFlight;
}

/**
 * Restore the shared sandbox. Returns the server's response so the caller can
 * distinguish "done" from "another visitor just reset it" (429).
 */
async function reset() {
	const response = await fetch(`${Utils.host()}/api/public/demo/reset`, {
		method: 'POST',
		headers: { Accept: 'application/json' },
	});
	const body = await response.json().catch(() => ({}));
	return { ok: response.ok, status: response.status, ...body };
}

/** Re-read the idle/dirty counters shown in the banner. */
async function refresh() {
	state.value = { ...state.value, loaded: false };
	return load();
}

export const demoService = {
	state,
	load,
	reset,
	refresh,
};
