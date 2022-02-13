<template>
	<q-header elevated class="bg-grey-10">
		<q-toolbar>
			<q-btn flat dense round @click="toggleSideBar" icon="menu" aria-label="Menu" />
			<q-toolbar-title> myHAB</q-toolbar-title>
			<div class="q-gutter-sm row items-center no-wrap">
				<div>
					<q-icon name="mdi-wifi" class="float-right" color="green" size="40px" v-if="wsConnection == 'ONLINE'" />
					<q-icon name="mdi-wifi-off" class="float-right" color="red" size="40px" v-if="wsConnection == 'OFFLINE'" />
				</div>
				<q-btn
					round
					dense
					flat
					color="white"
					icon="fas fa-mobile-alt"
					type="a"
					href="https://github.com/kirpi4ik/myhab"
					target="_blank"
				>
				</q-btn>
				<q-btn
					round
					dense
					flat
					color="white"
					:icon="$q.fullscreen.isActive ? 'fullscreen_exit' : 'fullscreen'"
					@click="$q.fullscreen.toggle()"
					v-if="$q.screen.gt.sm"
				>
				</q-btn>
				<q-btn round dense flat color="white" icon="notifications">
					<q-badge color="red" text-color="white" floating> 5</q-badge>
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
				</q-btn>
			</div>
		</q-toolbar>
	</q-header>
</template>
<script>
import { defineComponent } from 'vue';
import UserMessages from './UserMessages';
import { useUiState } from '../composables';

export default defineComponent({
	name: 'HeaderLayout',
	components: {
		UserMessages,
	},
	computed: {
		wsConnection() {
			return this.$store.state.ws.connection;
		},
	},
	setup() {
		const { toggleSideBar } = useUiState();
		return {
			toggleSideBar,
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
