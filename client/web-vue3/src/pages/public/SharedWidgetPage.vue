<template>
	<div v-if="loading" class="dispatch-page">
		<q-spinner-dots size="60px" color="white"/>
	</div>

	<div v-else-if="errorMessage" class="dispatch-page">
		<q-icon name="mdi-alert-circle-outline" size="80px" color="red-4"/>
		<div class="dispatch-title">Access Unavailable</div>
		<div class="dispatch-text">{{ errorMessage }}</div>
	</div>

	<gate-access-widget v-else-if="widgetType === 'GATE_ACCESS'"/>
	<shared-switch-widget v-else :widget-type="widgetType"/>
</template>

<script>
import { defineComponent, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { Utils } from '@/_helpers';
import GateAccessWidget from 'pages/public/GateAccessWidget';
import SharedSwitchWidget from 'pages/public/SharedSwitchWidget';

/**
 * Resolves which public widget a share token points at, then delegates. Each child
 * loads its own data, so the gate page stays untouched.
 */
export default defineComponent({
	name: 'SharedWidgetPage',
	components: { GateAccessWidget, SharedSwitchWidget },
	setup() {
		const route = useRoute();
		const loading = ref(true);
		const errorMessage = ref('');
		const widgetType = ref(null);

		onMounted(async () => {
			const token = route.params.token;
			if (!token) {
				errorMessage.value = 'Invalid share link';
				loading.value = false;
				return;
			}

			try {
				const res = await fetch(`${Utils.host()}/api/public/share/${token}`);
				if (res.status === 404) {
					errorMessage.value = 'This share link does not exist';
					return;
				}
				const data = await res.json();
				widgetType.value = data.widgetType;
			} catch {
				errorMessage.value = 'Failed to load share link';
			} finally {
				loading.value = false;
			}
		});

		return { loading, errorMessage, widgetType };
	}
});
</script>

<style lang="scss" scoped>
.dispatch-page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
	color: white;
	padding: 24px;
	text-align: center;
}

.dispatch-title {
	font-size: 1.5rem;
	font-weight: 600;
	margin-top: 16px;
	color: #ef9a9a;
}

.dispatch-text {
	font-size: 1rem;
	opacity: 0.8;
	margin-top: 8px;
}
</style>
