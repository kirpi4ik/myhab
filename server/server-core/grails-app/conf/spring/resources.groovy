import eu.devexpert.config.ConfigProvider
import eu.devexpert.madhouse.async.mqtt.handlers.MQTTMessageHandler
import eu.devexpert.madhouse.auth.ClaimProvider
import eu.devexpert.madhouse.graphql.GQLSchemaFactory
import eu.devexpert.madhouse.graphql.GraphQLGenerator
import eu.devexpert.madhouse.graphql.fetchers.Mutation

import eu.devexpert.madhouse.async.socket.WebSocketConfig
import eu.devexpert.madhouse.listener.domain.UserPasswordEncoderListener
import eu.devexpert.madhouse.telegram.TelegramBotHandler
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