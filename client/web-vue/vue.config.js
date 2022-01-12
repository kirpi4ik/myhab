module.exports = {
    lintOnSave: false,
    runtimeCompiler: true,

    devServer: {
        port: 9999,
        compress: false,
        headers: {
            'X-myHAB-mode': 'dev'
        },
        liveReload: true

    },

    publicPath: "/",

    configureWebpack: {
        //Necessary to run npm link https://webpack.js.org/configuration/resolve/#resolve-symlinks
        resolve: {
            symlinks: true
        }
    },

    chainWebpack: (config) => {
        // GraphQL Loader
        config.module
            .rule('graphql')
            .test(/\.(graphql|gql)$/)
            .use('graphql-tag/loader')
            .loader('graphql-tag/loader')
            .end();
    },

    pluginOptions: {
      i18n: {
        locale: 'en',
        fallbackLocale: 'en',
        localeDir: 'locales',
        enableInSFC: false
      }
    }
}
