package eu.devexpert.madhouse

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.idl.*
import org.grails.datastore.mapping.model.MappingContext
import org.grails.gorm.graphql.Schema
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

import static graphql.language.ObjectTypeDefinition.newObjectTypeDefinition

class GQLSchemaFactory extends Schema implements GrailsConfigurationAware {
    Resource gqlSchema
    def schemaGenerator = new SchemaGenerator()
    def schemaParser = new SchemaParser();

    def schemaDefinitionRegistry = new TypeDefinitionRegistry();
    final List<FieldDefinition> queryDefList = new ArrayList<>();
    final List<FieldDefinition> mutationDefList = new ArrayList<>();
    final List<SDLDefinition> objectTypeExtensions = new ArrayList<>();

    private static final String TYPE_MUTATION = "Mutation";
    private static final String TYPE_QUERY = "Query";

    GQLSchemaFactory(MappingContext... mappingContext) {
        super(mappingContext)
    }

    def loadidl(GraphQLSchema initialSchemaBuilder) {
        def stream = new ClassPathResource("schema.graphqls").inputStream
        def schemasContent = [ stream.text, new SchemaPrinter().print(initialSchemaBuilder)]
        TypeDefinitionRegistry loadedFromFileDefRegistry
        schemasContent.each { idlSchemaContent ->
            loadedFromFileDefRegistry = schemaParser.parse(idlSchemaContent);
            loadedFromFileDefRegistry.types().forEach { typeName, definition ->
                //Add only content of the root elements to our own Query and Mutation
                if (typeName.equalsIgnoreCase(TYPE_QUERY)) {
                    queryDefList.addAll(childrenFieldsOfTypeDefinition((ObjectTypeDefinition) definition));
                } else if (typeName.equalsIgnoreCase(TYPE_MUTATION)) {
                    mutationDefList.addAll(childrenFieldsOfTypeDefinition((ObjectTypeDefinition) definition));
                } else {
                    schemaDefinitionRegistry.add(definition);
                }
            }
        }
        schemaDefinitionRegistry.add(newObjectTypeDefinition().fieldDefinitions(queryDefList).name(TYPE_QUERY).build());
        schemaDefinitionRegistry.add(newObjectTypeDefinition().fieldDefinitions(mutationDefList).name(TYPE_MUTATION).build());
        objectTypeExtensions.each {
            schemaDefinitionRegistry.add(it)
        }


        def schema = schemaGenerator.makeExecutableSchema(schemaDefinitionRegistry, RuntimeWiring.newRuntimeWiring().wiringFactory(new WireFactory()).codeRegistry(initialSchemaBuilder.codeRegistry).build())
        return schema
    }

    static List<FieldDefinition> childrenFieldsOfTypeDefinition(ObjectTypeDefinition definition) {
        return definition.getChildren().collect { node -> (FieldDefinition) node };
    }

    @Override
    GraphQLSchema generate() {
        def gormSchema = super.generate()
        return loadidl(gormSchema)

    }

    @Override
    void setConfiguration(Config co) {
        print("INJECT CONFIG")
    }
}
