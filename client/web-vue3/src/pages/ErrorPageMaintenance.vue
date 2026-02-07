<template>
	<q-layout class="bg-image" v-cloak>
		<q-page-container>
			<q-page class="flex flex-center">
				<q-card class="bg-transparent no-border no-shadow">
					<div class="row items-center full-width justify-center q-col-gutter-lg">
						<q-icon name="settings" color="bg-grey-2 text-white" size="5rem"></q-icon>
						<q-icon name="warning" class="q-ml-lg" color="bg-grey-2 text-white" size="5rem"></q-icon>
					</div>
					<div class="row full-width">
						<div class="col-lg-12 col-md-12 cl-sm-12 col-xs-12">
							<div>
								<div class="text-h3 text-center text-white">{{ title }}</div>
								<div class="text-h6 text-center text-white">{{ message }}</div>
								<div class="text-center q-mt-md" v-if="showRetry">
									<q-btn 
										color="white" 
										text-color="primary" 
										label="Retry Connection" 
										icon="refresh"
										@click="retryConnection"
									/>
								</div>
							</div>
						</div>
					</div>
				</q-card>
			</q-page>
		</q-page-container>
	</q-layout>
</template>

<script>
import {defineComponent, computed} from 'vue';
import {useRoute, useRouter} from 'vue-router';


export default defineComponent({
	name: 'ErrorPageMaintenance',
	setup() {
		const route = useRoute();
		const router = useRouter();
		
		const title = computed(() => route.query.title || 'Under Maintenance');
		const message = computed(() => route.query.message || 'Our site is Under Maintenance. We will be back shortly.');
		const showRetry = computed(() => route.query.retry === 'true');
		
		const retryConnection = () => {
			// Try to go back or go to home
			if (window.history.length > 1) {
				router.go(-1);
			} else {
				router.push('/');
			}
		};
		
		return {
			title,
			message,
			showRetry,
			retryConnection
		};
	}
});

</script>

<style>
.bg-image {
	background-image: linear-gradient(135deg, #7028e4 0%, #e5b2ca 100%);
}

[v-cloak] {
	display: none !important;
}
</style>
