<template>
	<q-breadcrumbs class="text-brown" style="margin-top: 5px">
		<template v-slot:separator>
			<q-icon size="1.5em" name="chevron_right" color="primary" />
		</template>
		<q-breadcrumbs-el label="Home" icon="home" to="/" />
		<q-breadcrumbs-el label="Components" icon="widgets" to="/" />
		<q-breadcrumbs-el label="Breadcrumbs" icon="navigation" />
	</q-breadcrumbs>
</template>
<script>
import { defineComponent } from 'vue';

export default defineComponent({
	name: 'BreadCrumbLayout',
	components: {},
	setup() {},
	mounted() {
		this.init();
	},
	methods: {
		init() {},
		onResize() {},
	},
});
</script>
