package org.myhab.graphql

import com.google.common.base.CaseFormat
import grails.util.Holders
import grails.util.Pair
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLTypeUtil
import graphql.schema.idl.FieldWiringEnvironment
import graphql.schema.idl.WiringFactory

import java.lang.reflect.Method

class WireFactory implements WiringFactory {
    @Override
    public final boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return getFetcherMethod(environment) != null;
    }

    @Override
    DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        def exec = getFetcherMethod(environment)
        if (exec.aValue != null && exec.bValue != null) {
            return exec.aValue."${exec.bValue.name}"()
        } else if (!GraphQLTypeUtil.isScalar(environment.fieldType) || (environment.parentType.name == GQLConstants.TYPE_QUERY || environment.parentType.name == GQLConstants.TYPE_MUTATION)) {
            return new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment env) throws Exception {
                    return new HashMap()
                }
            }
        } else {
            return null
        }


    }

    private static Pair<Object, Method> getFetcherMethod(FieldWiringEnvironment environment) {
        def context = Holders.grailsApplication.mainContext
        def beanName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, environment.parentType.name)
        if (context.containsBean(beanName)) {
            def typeFetcherService = context.getBean(beanName)
            if (typeFetcherService != null) {
                Method fetchMethod = typeFetcherService.class.methods.find { m -> m.name == environment.fieldDefinition.name }
                if (fetchMethod != null) {
                    return new Pair<Object, Method>(typeFetcherService, fetchMethod)
                } else if (!GraphQLTypeUtil.isScalar(environment.fieldType)) {
                    return new Pair<Object, Method>(null, null)
                }
            }
        }

        return null

    }
}
