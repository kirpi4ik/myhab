package org.myhab.graphql

import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType

class GORMSchemaRegistry {
    Set<GraphQLType> additionalTypes
    GraphQLCodeRegistry.Builder codeRegistry
    GraphQLObjectType.Builder query
    GraphQLObjectType.Builder mutation
}
