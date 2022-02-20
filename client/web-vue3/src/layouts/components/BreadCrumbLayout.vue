<template>
	<q-separator class="bg-blue-grey-2" />
	<q-breadcrumbs
		class="text-blue-grey bg-blue-grey-1"
		style="padding-top: 2px; padding-left: 5px"
		v-if="breadcrumb && breadcrumb.navigation"
	>
		<template v-slot:separator>
			<q-icon size="1.5em" name="chevron_right" color="text-blue-grey" />
		</template>
		<q-breadcrumbs-el :label="$t('navigation.home')" icon="home" to="/" />
		<q-breadcrumbs-el
			:label="link.name"
			:to="'/zones/' + link.id + '?category=' + route.query.category"
			v-for="link in breadcrumb.navigation.breadcrumb"
			v-bind:key="link.zoneId"
		/>
	</q-breadcrumbs>
</template>
<script>
import { defineComponent } from 'vue';
import { useQuery, useQueryLoading } from '@vue/apollo-composable';
import { NAV_BREADCRUMB } from '@/graphql/queries';
import { useRoute } from 'vue-router';

export default defineComponent({
	name: 'BreadCrumbLayout',
	components: {},
	setup() {
		const route = useRoute();

		if (route.meta.navigation) {
			let type = route.meta.navigation.type;
			let id = route.params[route.meta.navigation.id];
			const { result: breadcrumb } = useQuery(NAV_BREADCRUMB, { id: id, type: type });
			const loading = useQueryLoading();
			return { route, breadcrumb };
		} else {
			return { route, breadcrumb: '' };
		}
	},
	watch: {
		$route() {
			this.paths = this.$route.meta.breadcrumb;
		},
	},
});
</script>
