<template>
	<q-card class="text-white" style="background: linear-gradient(#546e82, #7c919f); height: 150px">
		<q-item>
			<q-item-section
				@click="router.push({ name: 'zoneById', params: { zoneId: `${zone.id}` }, query: { category: route.query.category } })"
				v-ripple
			>
				<q-item-label class="text-weight-bold text-h5">{{ zone.name }}</q-item-label>
			</q-item-section>
			<q-item-section side>
				<q-item-label>
					<heat-scheduler :zone="zone" v-if="['HEAT', 'TEMP'].includes(route.query.category)" />
				</q-item-label>
			</q-item-section>
		</q-item>
	</q-card>
</template>
<script>
import { defineComponent } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import HeatScheduler from '@/components/HeatScheduler';

export default defineComponent({
	name: 'ZoneCard',
	props: {
		zone: Object,
	},
	components: {
		HeatScheduler,
	},
	setup(props) {
		const router = useRouter();
		const route = useRoute();
		return { router, route };
	},
});
</script>
