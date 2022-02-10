<template>
	<q-page class="q-pa-sm">
		<dashboard-actions />
	</q-page>
</template>

<script>
import { defineComponent, defineAsyncComponent } from 'vue';

export default defineComponent({
	name: 'PageIndex',
	components: {
		DashboardActions: defineAsyncComponent(() => import('components/cards/DashboardActions')),
	},
	setup() {},
});
</script>
