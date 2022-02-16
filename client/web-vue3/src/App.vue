<template>
	<router-view :key="$route.fullPath"/>
</template>
<script>
import { defineComponent } from 'vue';
import { mapActions } from 'vuex';

export default defineComponent({
	name: 'App',
	methods: {
		...mapActions(['connect']),
	},
	mounted() {
		this.connect();
	},
});
</script>
