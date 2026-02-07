export const Utils = {
	host: function () {
		if (process.env.ENV_NAME === 'Production') {
			return process.env.BCK_SERVER_URL;
		}
		return process.env.BCK_SERVER_URL || 'http://localhost:8181';
	},
};
