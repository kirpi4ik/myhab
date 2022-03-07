<template>
	<q-layout view="lHh Lpr lFf">
		<header-layout />
		<side-bar-layout />
		<q-page-container class="bg-grey-1">
			<router-view />
		</q-page-container>
	</q-layout>
</template>

<script>
import HeaderLayout from 'layouts/components/HeaderLayout.vue';
import SideBarLayout from 'layouts/components/SideBarLayout.vue';

import { defineComponent } from 'vue';

export default defineComponent({
	name: 'MainLayout',

	components: {
		SideBarLayout,
		HeaderLayout,
	},
	setup() {},
});
</script>
