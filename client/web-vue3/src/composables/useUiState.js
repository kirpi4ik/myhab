import {computed, reactive, ref} from 'vue';

const state = reactive({
  isSidebarOpen: ref(localStorage.getItem('isSidebarOpen') === 'true') //? !Platform.is.mobile : true,
});

const useUiState = () => {
  const isSidebarOpen = computed({
    get: () => state.isSidebarOpen,
    set: (newValue) => state.isSidebarOpen = newValue
  });
  const toggleSideBar = () => {
    state.isSidebarOpen = !state.isSidebarOpen;
    localStorage.setItem('isSidebarOpen', (state.isSidebarOpen).toString());
  };
  return {
    isSidebarOpen,
    toggleSideBar,
  };
};
export default useUiState;
