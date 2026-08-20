<template>
	<q-banner v-if="demo.enabled" dense class="demo-banner">
		<template v-slot:avatar>
			<q-icon name="science" color="white" />
		</template>

		<span class="demo-banner__text">
			<strong>Demo</strong> — this is a shared sandbox with simulated devices.
			Everything you change here is visible to other visitors and is rolled back
			automatically after {{ demo.idleMinutes }} minutes of inactivity.
		</span>

		<template v-slot:action>
			<q-btn
				flat
				dense
				no-caps
				color="white"
				:loading="resetting"
				:disable="!demo.resetEnabled"
				label="Reset demo"
				@click="confirmReset = true"
			/>
		</template>

		<q-dialog v-model="confirmReset">
			<q-card style="min-width: 320px">
				<q-card-section class="text-h6">Reset the demo?</q-card-section>
				<q-card-section class="q-pt-none">
					This restores the sample home to its original state and discards every
					change made since the last reset — including changes made by other
					visitors who may be using the demo right now.
				</q-card-section>
				<q-card-actions align="right">
					<q-btn flat no-caps label="Cancel" v-close-popup />
					<q-btn flat no-caps color="negative" label="Reset" @click="doReset" v-close-popup />
				</q-card-actions>
			</q-card>
		</q-dialog>
	</q-banner>
</template>

<script>
import { computed, defineComponent, onMounted, onUnmounted, ref } from 'vue';
import { useQuasar } from 'quasar';
import { demoService } from '@/_services/demo.service';

/**
 * Tells a visitor what the demo is, and gives them a way out of any mess they make.
 *
 * Renders nothing at all unless the backend reports a demo deployment, so this can be
 * mounted unconditionally in the layout.
 */
export default defineComponent({
	name: 'DemoBanner',

	setup() {
		const $q = useQuasar();
		const demo = computed(() => demoService.state.value);
		const resetting = ref(false);
		const confirmReset = ref(false);

		let poll = null;
		let knownReset = null;

		onMounted(async () => {
			await demoService.load();
			if (!demo.value.enabled) return;
			knownReset = demo.value.lastReset;

			// A reset can also come from another visitor or from the idle timer, in
			// which case every id on screen has just been replaced. Poll rather than
			// relying on a push: this is a low-traffic demo, the check is one cheap
			// unauthenticated GET, and it keeps working regardless of the socket's
			// state (which is exactly when a stale page is most likely).
			poll = setInterval(async () => {
				const state = await demoService.refresh();
				if (knownReset != null && state.lastReset !== knownReset) {
					reloadAfterReset();
				}
			}, 30000);
		});

		onUnmounted(() => {
			if (poll) clearInterval(poll);
		});

		const reloadAfterReset = () => {
			$q.notify({
				type: 'info',
				message: 'The demo was reset to its original state.',
				timeout: 3000,
			});
			setTimeout(() => window.location.reload(), 1200);
		};

		const doReset = async () => {
			resetting.value = true;
			try {
				const result = await demoService.reset();
				if (result.ok) {
					// The initiator already knows it succeeded from this response; no
					// reason to wait for a round trip to hear about its own action.
					reloadAfterReset();
					return;
				}
				$q.notify({
					type: 'warning',
					message:
						result.status === 429
							? 'The demo was just reset — try again in a moment.'
							: 'Could not reset the demo.',
				});
			} catch (err) {
				$q.notify({ type: 'negative', message: 'Could not reset the demo.' });
			} finally {
				resetting.value = false;
			}
		};

		return { demo, resetting, confirmReset, doReset };
	},
});
</script>

<style scoped>
.demo-banner {
	background: #5c3d99;
	color: #fff;
	padding: 4px 12px;
}

.demo-banner__text {
	font-size: 0.85rem;
}
</style>
