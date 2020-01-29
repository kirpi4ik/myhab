package eu.devexpert.madhouse.graphql

import com.google.common.collect.ImmutableList
import graphql.GraphQL
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.tracing.TracingInstrumentation
import graphql.schema.GraphQLSchema

class GraphQLGenerator {
    GraphQLSchema schema

    GraphQL generate() {
        TracingInstrumentation tracingIntrumentation = new TracingInstrumentation(TracingInstrumentation.Options.newOptions().includeTrivialDataFetchers(true))
        ChainedInstrumentation chainedInstrumentation = new ChainedInstrumentation(ImmutableList.of(tracingIntrumentation))
       return GraphQL.newGraphQL(schema)
                .instrumentation(chainedInstrumentation)
                .build()
    }
}
