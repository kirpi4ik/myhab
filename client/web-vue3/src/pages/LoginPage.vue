<template>
	<q-layout>
		<q-page-container>
			<q-page class="flex bg-image flex-center">
				<q-card v-bind:style="$q.screen.lt.sm ? { width: '80%' } : { width: '30%' }">
					<q-card-section>
						<q-avatar size="103px" class="absolute-center shadow-10">
							<img src="profile.svg" />
						</q-avatar>
					</q-card-section>
					<q-card-section>
						<div class="text-center q-pt-lg">
							<div class="col text-h6 ellipsis">Log in</div>
						</div>
					</q-card-section>
					<q-card-section>
						<q-form class="q-gutter-md" @submit="login">
							<q-input filled v-model="username" label="Username" lazy-rules />
							<q-input type="password" filled v-model="password" label="Password" lazy-rules />
							<div>
								<q-btn label="Login" type="submit" color="secondary" />
							</div>
						</q-form>
					</q-card-section>

					<!--
						Demo deployments publish their own credentials: there is nothing to
						protect, and making a visitor hunt for a password is the fastest way
						to lose them. Hidden entirely everywhere else.
					-->
					<q-card-section v-if="demo.enabled" class="q-pt-none">
						<q-separator class="q-mb-md" />
						<div class="text-caption text-grey-7 q-mb-sm">
							This is a public demo with simulated devices. Sign in with:
						</div>
						<div
							v-for="cred in demo.credentials"
							:key="cred.username"
							class="row items-center q-gutter-sm q-mb-xs"
						>
							<q-btn
								dense
								flat
								no-caps
								color="secondary"
								:label="`${cred.username} / ${cred.password}`"
								@click="useDemoCredentials(cred)"
							/>
							<q-badge outline color="grey-7" :label="cred.role.replace('ROLE_', '')" />
						</div>
						<div class="text-caption text-grey-6 q-mt-sm">
							Data is shared between visitors and resets when the demo goes idle.
						</div>
					</q-card-section>
				</q-card>
			</q-page>
		</q-page-container>
	</q-layout>
</template>

<script>
import {defineComponent, ref} from 'vue';

import {useRoute, useRouter} from 'vue-router';

import {authzService, demoService} from '@/_services';
import {useWebSocketStore} from '@/store/websocket.store';



export default defineComponent({
	setup() {
		const wsStore = useWebSocketStore();
		const route = useRoute();
		const router = useRouter();

		const username = ref('');
		const password = ref('');
		const demo = demoService.state;

		// Answers "is this the demo?" before any login has happened.
		demoService.load();

		const returnUrl = ref(route.query.returnUrl || '/');
		const loading = ref(false);
		const error = ref(null);

		const login = () => {
			loading.value = true;
			authzService.login(username.value, password.value).then(
				user => {
					wsStore.connect();
					router.push(returnUrl.value);
				},
				err => {
					error.value = err;
					loading.value = false;
				},
			);
		};

		/** Fill the form from a published demo account and sign straight in. */
		const useDemoCredentials = (cred) => {
			username.value = cred.username;
			password.value = cred.password;
			login();
		};

		return {
			username,
			password,
			returnUrl,
			loading,
			error,
			login,
			demo,
			useDemoCredentials,
		};
	},
});

</script>

<style>
.bg-image {
	background-image: linear-gradient(135deg, #7028e4 0%, #e5b2ca 100%);
}
</style>
