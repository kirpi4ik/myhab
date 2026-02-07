import { defineStore } from 'pinia';
import { LocalStorage } from 'quasar';

export const useSettingsStore = defineStore('settings', {
	state: () => ({
		settings: {
			show12HourTimeFormat: false,
			showTasksInOneList: false,
		},
	}),

	getters: {
		getSettings: (state) => state.settings,
	},

	actions: {
		setShow12HourTimeFormat(value) {
			this.settings.show12HourTimeFormat = value;
			this.saveSettings();
		},

		setShowTasksInOneList(value) {
			this.settings.showTasksInOneList = value;
			this.saveSettings();
		},

		setSettings(settings) {
			Object.assign(this.settings, settings);
		},

		saveSettings() {
			LocalStorage.set('settings', this.settings);
		},

		getSavedSettings() {
			const settings = LocalStorage.getItem('settings');
			if (settings) {
				this.setSettings(settings);
			}
		},
	},
});

