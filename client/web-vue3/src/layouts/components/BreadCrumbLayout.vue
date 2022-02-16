<template>
	<q-separator class="bg-blue-grey-2" />
	<q-breadcrumbs class="text-blue-grey bg-blue-grey-1" style="padding-top: 2px; padding-left: 5px">
		<template v-slot:separator>
			<q-icon size="1.5em" name="chevron_right" color="primary" />
		</template>
		<q-breadcrumbs-el :label="$t('navigation.home')" icon="home" to="/" />
		<q-breadcrumbs-el :label="path.name" icon="widgets" :to="path.path" v-for="path in paths" v-bind:key="path" />
	</q-breadcrumbs>
</template>
<script>
import { defineComponent } from 'vue';

export default defineComponent({
	name: 'BreadCrumbLayout',
	components: {},
	data() {
		return {
			paths: [],
		};
	},
	mounted() {
		this.init();
	},
	watch: {
		$route() {
			this.paths = this.$route.meta.breadcrumb;
		},
	},
	methods: {
		init() {},
		onResize() {},
	},
});
</script>
