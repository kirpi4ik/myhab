export const Utils = {
    host: function () {
        if (process.env.VUE_APP_ENV_NAME == "development") {
            return process.env.VUE_APP_SERVER_URL;
        } else {
            //            return window.location.origin;
            return "";
        }
    }
}
