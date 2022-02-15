<template>
	<q-header elevated class="bg-primary">
		<q-toolbar>
			<q-btn flat dense round @click="toggleSideBar" icon="menu" aria-label="Menu" color="secondary"/>
			<q-toolbar-title></q-toolbar-title>
			<div class="q-gutter-sm row items-center no-wrap">
				<div>
					<clock-component v-if="$q.screen.gt.sm" />
					<q-btn
						round
						dense
						flat
						color="secondary"
						icon="mdi-cctv"
						type="a"
						:href="result.config"
						v-if="result && result.config"
					></q-btn>
					<q-icon name="mdi-wifi" class="float-right" color="positive" size="40px" v-if="wsConnection == 'ONLINE'" />
					<q-icon name="mdi-wifi-off" class="float-right" color="negative" size="40px" v-if="wsConnection == 'OFFLINE'" />
				</div>
				<q-btn round dense flat color="secondary" icon="fas fa-mobile-alt" type="a" to="/wui"></q-btn>
				<q-btn
					round
					dense
					flat
					color="secondary"
					:icon="$q.fullscreen.isActive ? 'fullscreen_exit' : 'fullscreen'"
					@click="$q.fullscreen.toggle()"
					v-if="$q.screen.gt.sm"
				>
				</q-btn>
				<q-btn round dense flat color="secondary" icon="notifications">
					<q-badge color="negative" text-color="primary" floating> 5</q-badge>
					<q-menu>
						<q-list style="min-width: 100px">
							<user-messages></user-messages>
							<q-card class="text-center no-shadow no-border">
								<q-btn label="View All" style="max-width: 120px !important" flat dense class="text-indigo-8"></q-btn>
							</q-card>
						</q-list>
					</q-menu>
				</q-btn>
				<q-btn round flat>
					<q-avatar size="42px">
						<img src="~assets/avatar.png" />
					</q-avatar>
					<q-menu>
						<q-list style="min-width: 100px">
							<q-item style="max-width: 420px" clickable v-ripple @click="authenticationService.logout()">
								<q-item-section avatar>
									<q-avatar>
										<q-icon name="mdi-logout" />
									</q-avatar>
								</q-item-section>
								<q-item-section>
									<q-item-label>Logout</q-item-label>
								</q-item-section>
							</q-item>
						</q-list>
					</q-menu>
				</q-btn>
			</div>
		</q-toolbar>
	</q-header>
</template>
<script>
import { defineComponent } from 'vue';
import UserMessages from './UserMessages';
import { useUiState } from '@/composables';
import { useQuery } from '@vue/apollo-composable';
import { CONFIG_GLOBAL_GET_STRING_VAL } from '@/graphql/queries';
import ClockComponent from 'components/ClockComponent';
import { authenticationService } from '@/_services';

export default defineComponent({
	name: 'HeaderLayout',
	components: {
		UserMessages,
		ClockComponent,
	},
	computed: {
		wsConnection() {
			return this.$store.state.ws.connection;
		},
	},
	setup() {
		const { toggleSideBar } = useUiState();
		const { result } = useQuery(CONFIG_GLOBAL_GET_STRING_VAL, { key: 'surveillance.url' });
		return {
			toggleSideBar,
			result,
			authenticationService,
		};
	},
	mounted() {
		this.init();
	},
	methods: {
		init() {},
		onResize() {},
	},
});
</script>
