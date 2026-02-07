import {computed, reactive, ref} from 'vue';
import {Platform} from 'quasar';

/**
 * Get initial sidebar mini state from localStorage or default to mobile detection
 */
const getInitialMiniState = () => {
  const stored = localStorage.getItem('sidebarMiniState');
  if (stored !== null) {
    return stored === 'true';
  }
  return Platform.is.mobile;
};

const state = reactive({
  isSidebarOpen: ref(localStorage.getItem('isSidebarOpen') === 'true'), //? !Platform.is.mobile : true,
  isSidebarMini: ref(getInitialMiniState())
});

const useUiState = () => {
  const isSidebarOpen = computed({
    get: () => state.isSidebarOpen,
    set: (newValue) => {
      state.isSidebarOpen = newValue;
      localStorage.setItem('isSidebarOpen', newValue.toString());
    }
  });
  
  const isSidebarMini = computed({
    get: () => state.isSidebarMini,
    set: (newValue) => {
      state.isSidebarMini = newValue;
      localStorage.setItem('sidebarMiniState', newValue.toString());
    }
  });
  
  const toggleSideBar = () => {
    state.isSidebarOpen = !state.isSidebarOpen;
    localStorage.setItem('isSidebarOpen', (state.isSidebarOpen).toString());
  };
  
  const toggleSidebarMini = () => {
    state.isSidebarMini = !state.isSidebarMini;
    localStorage.setItem('sidebarMiniState', (state.isSidebarMini).toString());
  };
  
  return {
    isSidebarOpen,
    isSidebarMini,
    toggleSideBar,
    toggleSidebarMini,
  };
};
export default useUiState;
