<template>
	<q-card
		class="text-white"
		style="background: linear-gradient(#546e82, #7c919f); height: 150px"
		@click="router.push({ name: 'zoneById', params: { zoneId: `${zone.id}` }, query: { category: route.query.category } })"
		v-ripple
	>
		<q-item>
			<q-item-section>
				<q-item-label class="text-weight-bold text-h5">{{ zone.name }}</q-item-label>
			</q-item-section>

			<q-item-section side>
				<q-item-label>
					<q-btn size="sm" flat round icon="fab fa-facebook" class="bg-indigo-7 text-white" />
				</q-item-label>
				<q-item-label>
					<q-btn size="sm" flat round icon="fab fa-twitter" class="bg-info text-white" />
				</q-item-label>
			</q-item-section>
		</q-item>
	</q-card>
</template>
<script>
import { defineComponent } from 'vue';
import { useRoute, useRouter } from 'vue-router';

export default defineComponent({
	name: 'ZoneCard',
	props: {
		zone: Object,
	},
	setup(props) {
		const router = useRouter();
		const route = useRoute();
		return { router, route };
	},
});
</script>
