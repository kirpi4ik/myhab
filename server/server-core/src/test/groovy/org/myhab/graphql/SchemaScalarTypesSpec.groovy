package org.myhab.graphql

import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards the SDL against a regression that graphql-java 19+ made fatal: built-in
 * scalars no longer auto-coerce between Integer and String, so any argument that
 * receives a numeric entity id must be declared {@code ID} (which accepts both)
 * rather than {@code String}.
 *
 * @see <a href="https://www.graphql-java.com/documentation/upgrade-notes/">graphql-java upgrade notes</a>
 */
class SchemaScalarTypesSpec extends Specification {

    private static TypeDefinitionRegistry registry() {
        def sdl = SchemaScalarTypesSpec.classLoader.getResourceAsStream(GQLConstants.SCHEMA_FILE_NAME)
        assert sdl != null: "${GQLConstants.SCHEMA_FILE_NAME} not found on the test classpath"
        return new SchemaParser().parse(sdl.text)
    }

    private static String argType(String typeName, String fieldName, String argName) {
        ObjectTypeDefinition type = registry().getType(typeName, ObjectTypeDefinition).get()
        def field = type.fieldDefinitions.find { it.name == fieldName }
        assert field != null: "${typeName}.${fieldName} not found"
        def arg = field.inputValueDefinitions.find { it.name == argName }
        assert arg != null: "${typeName}.${fieldName}(${argName}) not found"
        return arg.type.toString()
    }

    void "the SDL parses"() {
        expect:
            registry().types().size() > 0
    }

    @Unroll
    void "#type.#field(#arg) is an ID so numeric ids still coerce"() {
        expect:
            argType(type, field, arg).contains('ID')

        where:
            type       | field         | arg
            'Query'    | 'cache'       | 'cacheKey'
            'Mutation' | 'cacheDelete' | 'cacheKey'
    }

    void "EventDatInput.p2 is an ID so numeric entity ids still coerce"() {
        given:
            InputObjectTypeDefinition input = registry()
                    .getType('EventDatInput', InputObjectTypeDefinition).get()

        expect:
            input.inputValueDefinitions.find { it.name == 'p2' }.type.toString().contains('ID')
    }
}
