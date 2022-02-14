<template>
	<q-drawer v-model="isSidebarOpen" show-if-above bordered class="bg-grey-9 text-white">
		<q-scroll-area style="height: calc(100% - 150px); margin-top: 50px; border-right: 1px solid #ddd">
			<q-list>
				<q-item to="/" active-class="q-item-no-link-highlighting">
					<q-item-section avatar>
						<q-icon name="dashboard" />
					</q-item-section>
					<q-item-section>
						<q-item-label>Dashboard</q-item-label>
					</q-item-section>
				</q-item>
				<q-item to="/Dashboard2" active-class="q-item-no-link-highlighting">
					<q-item-section avatar>
						<q-icon name="dashboard" />
					</q-item-section>
					<q-item-section>
						<q-item-label>Users</q-item-label>
					</q-item-section>
				</q-item>
				<q-expansion-item icon="pages" label="Cables">
					<q-list class="q-pl-lg">
						<q-item to="/login" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="email" />
							</q-item-section>
							<q-item-section>
								<q-item-label>login</q-item-label>
							</q-item-section>
						</q-item>
						<q-item to="/Lock" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="lock" />
							</q-item-section>
							<q-item-section>
								<q-item-label>Lock Screen</q-item-label>
							</q-item-section>
						</q-item>
						<q-item to="/Lock-2" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="lock" />
							</q-item-section>
							<q-item-section>
								<q-item-label>Lock Screen - 2</q-item-label>
							</q-item-section>
						</q-item>
						<q-item to="/Pricing" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="list" />
							</q-item-section>
							<q-item-section>
								<q-item-label>Pricing</q-item-label>
							</q-item-section>
						</q-item>
						<q-item-label header class="text-weight-bolder text-white">Generic</q-item-label>
						<q-item to="/Profile" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="person" />
							</q-item-section>
							<q-item-section>
								<q-item-label>User Profile</q-item-label>
							</q-item-section>
						</q-item>
						<q-item to="/Maintenance" active-class="q-item-no-link-highlighting">
							<q-item-section avatar>
								<q-icon name="settings" />
							</q-item-section>
							<q-item-section>
								<q-item-label>Maintenance</q-item-label>
							</q-item-section>
						</q-item>
					</q-list>
				</q-expansion-item>
			</q-list>
		</q-scroll-area>
		<q-img class="absolute-top" src="~assets/background.jpg" style="height: 50px">
			<div class="absolute-bottom bg-transparent">
				<div class="text-weight-bold">{{ $t('actions.view') }}</div>
			</div>
		</q-img>
	</q-drawer>
</template>
<script>
import { defineComponent } from 'vue';
import { useUiState } from '@/composables';

export default defineComponent({
	name: 'SideBarLayout',
	setup() {
		const { isSidebarOpen } = useUiState();
		return {
			isSidebarOpen,
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
