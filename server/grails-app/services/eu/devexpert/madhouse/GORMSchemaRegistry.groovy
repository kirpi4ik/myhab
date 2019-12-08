package eu.devexpert.madhouse

import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType

class GORMSchemaRegistry {
    Set<GraphQLType> additionalTypes
    GraphQLCodeRegistry.Builder codeRegistry
    GraphQLObjectType.Builder query
    GraphQLObjectType.Builder mutation
}
