export const Utils = {
	host: function () {
		if (process.env.ENV_DEV == 'Development') {
			return process.env.BCK_SERVER_URL;
		} else {
			return '';
		}
	},
};
