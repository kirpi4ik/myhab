module.exports = {
    lintOnSave: false,
    runtimeCompiler: true,
    devServer: {port: 9999},
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
    }
}