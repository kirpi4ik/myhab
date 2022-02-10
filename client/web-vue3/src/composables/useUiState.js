import { computed, reactive } from 'vue';

const state = reactive({
	isSidebarOpen: true,
});

const useUiState = () => {
	const isSidebarOpen = computed({ get: () => state.isSidebarOpen, set: value => (state.isSidebarOpen = !state.isSidebarOpen) });
	const toggleSideBar = () => {
		state.isSidebarOpen = !state.isSidebarOpen;
	};
	return {
		isSidebarOpen,
		toggleSideBar,
	};
};
export default useUiState;
