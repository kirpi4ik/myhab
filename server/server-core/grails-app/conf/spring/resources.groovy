import org.myhab.config.ConfigProvider
import org.myhab.async.mqtt.handlers.MQTTMessageHandler
import org.myhab.auth.ClaimProvider
import org.myhab.graphql.GQLSchemaFactory
import org.myhab.graphql.GraphQLGenerator
import org.myhab.graphql.fetchers.Mutation

import org.myhab.async.socket.WebSocketConfig
import org.myhab.listener.domain.UserPasswordEncoderListener
import org.myhab.telegram.TelegramBotHandler
import grails.util.Environment
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

beans = {
    configProvider(ConfigProvider) {
        repoURI = System.getenv("CFG_REPO_URI")
        username = System.getenv("CFG_USERNAME")
        password = System.getenv("CFG_PASSWORD")
        branch = (Environment.current == Environment.PRODUCTION ? "prod" : "dev")
    }

    passwordEncoder(BCryptPasswordEncoder)
    userPasswordEncoderListener(UserPasswordEncoderListener)
    graphQLSchemaGenerator(GQLSchemaFactory) {
        deleteResponseHandler = ref("graphQLDeleteResponseHandler")
        namingConvention = ref("graphQLEntityNamingConvention")
        typeManager = ref("graphQLTypeManager")
        dataBinderManager = ref("graphQLDataBinderManager")
        dataFetcherManager = ref("graphQLDataFetcherManager")
        interceptorManager = ref("graphQLInterceptorManager")
        paginationResponseHandler = ref("graphQLPaginationResponseHandler")
        serviceManager = ref("graphQLServiceManager")

        dateFormats = '#{grailsGraphQLConfiguration.getDateFormats()}'
        dateFormatLenient = '#{grailsGraphQLConfiguration.getDateFormatLenient()}'
        listArguments = '#{grailsGraphQLConfiguration.getListArguments()}'
        gqlSchema = "classpath:schema.graphqls"
    }
    mutation(Mutation)
    graphQLGenrator(GraphQLGenerator) {
        schema = ref("graphQLSchema")
    }
    graphQL(graphQLGenrator: "generate")
    customClaimProvider(ClaimProvider)

    threadMetrics(JvmThreadMetrics)

    webSocketConfig(WebSocketConfig) {
        configProvider = ref("configProvider")
    }
    mQTTMessageHandler(MQTTMessageHandler)
    mQTTMessageHandler(MQTTMessageHandler)

    telegramBotHandler(TelegramBotHandler) {
        configProvider = ref("configProvider")
    }
}