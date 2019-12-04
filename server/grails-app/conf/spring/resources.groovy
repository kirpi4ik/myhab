import eu.devexpert.madhouse.GQLSchemaFactory
import eu.devexpert.madhouse.UserPasswordEncoderListener

beans = {
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

}