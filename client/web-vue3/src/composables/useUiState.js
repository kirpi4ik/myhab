import { computed, reactive } from 'vue';
import { Platform } from 'quasar';

const state = reactive({
	isSidebarOpen: !Platform.is.mobile,
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
