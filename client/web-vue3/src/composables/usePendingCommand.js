import { onUnmounted, ref } from 'vue';

/**
 * Tracks one in-flight device command so a control can show that something is
 * happening while the device takes its time to answer.
 *
 * The state a card renders is always the device-confirmed one: a command is sent, and
 * only an observed state moves the control. Between those two points the user otherwise
 * gets no signal at all — the Tuya valve takes around five seconds — so this fills the
 * gap without ever showing a state the device has not reported.
 *
 * Waiting is resolved by comparing what was asked for against what is observed, never by
 * "an event arrived": an echo carrying the opposite value means the command did not take,
 * and treating it as confirmation would hide exactly the failure worth reporting.
 *
 * A missed echo must not become a false accusation. State echoes are QoS 1 over MQTT and
 * then STOMP, and either hop can drop one — a broker reconnect is enough. So when the
 * window closes, `verify` gets the last word: it re-reads the real state, and only a
 * value that still disagrees with the request is reported as unconfirmed.
 *
 * Nothing here can leave a control stuck. The window always ends, and every path out of
 * it clears `pending`.
 *
 * `EXPIRE_MS` is deliberately well above the observed round-trip. A window equal to the
 * typical latency would race it on every command.
 */
const EXPIRE_MS = 10000;

/** How often `progress` is recomputed. Only drives a progress bar; 10fps is plenty. */
const TICK_MS = 100;

/**
 * @param {object}   [options]
 * @param {number}   [options.expireMs]  How long to wait for the device.
 * @param {function} [options.verify]    `async () => observedValue`, called once when the
 *                                       window closes. Its result is compared against the
 *                                       requested value. Without it, a closed window is
 *                                       always reported as unconfirmed.
 */
export function usePendingCommand(options = {}) {
	const { expireMs = EXPIRE_MS, verify = null } = options;

	/** A command is in flight and still inside its window. */
	const pending = ref(false);
	/** The last command ended without the device reaching the requested state. */
	const unconfirmed = ref(false);
	/** 0..1 across the window, for a determinate progress bar. */
	const progress = ref(0);

	/** What the last command asked the device to become. */
	let target;
	/**
	 * Identifies the current command. Every settle path checks it, so a slow `verify`
	 * belonging to a command that has already been resolved or superseded cannot write
	 * its outcome over the newer one.
	 */
	let seq = 0;

	let expireTimer = null;
	let tickTimer = null;

	const stopTimers = () => {
		clearTimeout(expireTimer);
		clearInterval(tickTimer);
		expireTimer = null;
		tickTimer = null;
	};

	const settle = (isUnconfirmed) => {
		stopTimers();
		pending.value = false;
		unconfirmed.value = isUnconfirmed;
		progress.value = 0;
	};

	/**
	 * Call immediately after handing the command to the backend.
	 *
	 * @param {*} requested The state the device is being asked to reach, in whatever
	 *                      shape `observe`/`verify` report it (a boolean, for a switch).
	 */
	const start = (requested) => {
		stopTimers();
		const mine = ++seq;
		target = requested;
		unconfirmed.value = false;
		pending.value = true;
		progress.value = 0;

		const startedAt = Date.now();
		tickTimer = setInterval(() => {
			progress.value = Math.min(1, (Date.now() - startedAt) / expireMs);
		}, TICK_MS);

		expireTimer = setTimeout(async () => {
			clearInterval(tickTimer);
			tickTimer = null;
			progress.value = 1;

			// The bar stays full while the last-word read is in flight, so the control
			// does not blink through a resolved-looking state and back.
			let observed;
			try {
				observed = verify ? await verify() : undefined;
			} catch {
				observed = undefined;
			}
			if (mine !== seq) return;
			settle(observed !== target);
		}, expireMs);
	};

	/**
	 * Report a state the device has just been seen in. Ends the wait only when it is the
	 * state that was asked for; anything else leaves the command outstanding.
	 */
	const observe = (value) => {
		if (pending.value && value === target) {
			seq++;
			settle(false);
		}
	};

	/** End the wait successfully without comparing anything. */
	const confirm = () => {
		seq++;
		settle(false);
	};

	/** The request never reached the backend — no point waiting out the window. */
	const fail = () => {
		seq++;
		settle(true);
	};

	/** Drop both the wait and the warning without claiming either outcome. */
	const reset = () => {
		seq++;
		settle(false);
	};

	onUnmounted(stopTimers);

	return { pending, unconfirmed, progress, start, observe, confirm, fail, reset };
}
