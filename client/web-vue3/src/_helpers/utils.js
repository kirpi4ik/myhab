export const Utils = {
	host: function () {
		if (process.env.ENV_NAME == 'Development') {
			return process.env.BCK_SERVER_URL;
		} else {
			return '';
		}
	},
};
