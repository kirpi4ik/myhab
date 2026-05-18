<template>
	<div class="oauth-callback-page">
		<div class="card">
			<div class="icon" :class="status">{{ iconChar }}</div>
			<h1>{{ title }}</h1>
			<p v-if="status === 'pending'">Finishing the sign-in… one moment.</p>
			<p v-else-if="status === 'ok'">
				Device <code>{{ deviceCode }}</code> now has an access token. You can close this window.
			</p>
			<p v-else>
				<strong>Error:</strong> {{ errorMessage }}
			</p>
			<button v-if="status !== 'pending'" @click="closeWindow">Close</button>
		</div>
	</div>
</template>

<script>
import { defineComponent, ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { apolloClient } from 'boot/graphql';
import { NAVIMOW_OAUTH_COMPLETE } from '@/graphql/queries';

/**
 * SPA-side OAuth2 callback for the Navimow integration. Receives `code` and
 * `state` query params from Navimow's redirect, calls the
 * `navimowOAuthComplete` GraphQL mutation to exchange them for a token, and
 * posts a `navimow-oauth` message back to the opener window so DeviceEdit
 * can refresh its data.
 *
 * Used in place of the legacy controller-rendered HTML page because the
 * SPA path is universally proxied in deployments where backend paths like
 * `/auth/external/*` may not be forwarded to Grails. The path stays exactly
 * the same so Navimow's redirect_uri whitelist still matches.
 */
export default defineComponent({
	name: 'NavimowOAuthCallback',
	setup() {
		const route = useRoute();
		const status = ref('pending'); // 'pending' | 'ok' | 'fail'
		const deviceId = ref(null);
		const deviceCode = ref(null);
		const errorMessage = ref('');

		const iconChar = computed(() => {
			if (status.value === 'ok') return '✔';
			if (status.value === 'fail') return '✖';
			return '⋯'; // "…" placeholder while pending
		});
		const title = computed(() => {
			if (status.value === 'ok') return 'Navimow account connected';
			if (status.value === 'fail') return 'Navimow connection failed';
			return 'Connecting your Navimow…';
		});

		const postToOpener = () => {
			try {
				if (window.opener) {
					window.opener.postMessage({
						type: 'navimow-oauth',
						success: status.value === 'ok',
						deviceId: deviceId.value,
						error: status.value === 'ok' ? null : errorMessage.value,
					}, window.location.origin);
				}
			} catch (e) {
				// Parent on a different origin — user just sees this page and clicks Close.
			}
		};

		const closeWindow = () => {
			try {
				window.close();
			} catch (e) {
				// Some browsers block window.close on tabs not opened by JS — no-op fallback.
			}
		};

		onMounted(async () => {
			// Navimow may pass `error=...` instead of `code` if the user denies.
			const upstreamError = route.query.error;
			if (upstreamError) {
				status.value = 'fail';
				errorMessage.value = String(upstreamError) +
					(route.query.error_description ? ' — ' + route.query.error_description : '');
				postToOpener();
				return;
			}

			const code = route.query.code;
			const state = route.query.state;
			if (!code || !state) {
				status.value = 'fail';
				errorMessage.value = 'Missing code or state in callback URL';
				postToOpener();
				return;
			}

			try {
				const { data } = await apolloClient.mutate({
					mutation: NAVIMOW_OAUTH_COMPLETE,
					variables: { code: String(code), state: String(state) },
					fetchPolicy: 'no-cache',
				});
				const result = data?.navimowOAuthComplete;
				if (result?.success) {
					status.value = 'ok';
					deviceId.value = result.deviceId;
					deviceCode.value = result.deviceCode;
					postToOpener();
					setTimeout(closeWindow, 1500);
				} else {
					status.value = 'fail';
					errorMessage.value = result?.error || 'Unknown error';
					postToOpener();
				}
			} catch (err) {
				status.value = 'fail';
				errorMessage.value = err?.message || String(err);
				postToOpener();
			}
		});

		return {
			status,
			deviceCode,
			errorMessage,
			iconChar,
			title,
			closeWindow,
		};
	},
});
</script>

<style scoped>
.oauth-callback-page {
	font-family: -apple-system, system-ui, sans-serif;
	max-width: 480px;
	margin: 80px auto;
	padding: 24px;
	color: #222;
}
.card {
	border: 1px solid #ddd;
	border-radius: 12px;
	padding: 32px;
	text-align: center;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
	background: #fff;
}
.icon {
	font-size: 48px;
	line-height: 1;
	margin-bottom: 8px;
}
.icon.pending {
	color: #999;
}
.icon.ok {
	color: #0a7;
}
.icon.fail {
	color: #c33;
}
h1 {
	margin: 0 0 16px;
	font-size: 20px;
}
p {
	margin: 0;
	line-height: 1.5;
	color: #444;
}
code {
	background: #f3f3f5;
	padding: 1px 6px;
	border-radius: 4px;
}
button {
	margin-top: 24px;
	padding: 8px 16px;
	border: 0;
	border-radius: 6px;
	background: #444;
	color: white;
	cursor: pointer;
}
button:hover {
	background: #222;
}
</style>
