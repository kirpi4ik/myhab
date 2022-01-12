package org.myhab.graphql


import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.SDLDefinition
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import javassist.Modifier
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.gorm.graphql.GraphQLEntityHelper
import org.grails.gorm.graphql.Schema
import org.grails.gorm.graphql.binding.DataBinderNotFoundException
import org.grails.gorm.graphql.binding.GraphQLDataBinder
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.entity.operations.CustomOperation
import org.grails.gorm.graphql.entity.operations.ListOperation
import org.grails.gorm.graphql.entity.operations.ProvidedOperation
import org.grails.gorm.graphql.fetcher.BindingGormDataFetcher
import org.grails.gorm.graphql.fetcher.DeletingGormDataFetcher
import org.grails.gorm.graphql.fetcher.PaginatingGormDataFetcher
import org.grails.gorm.graphql.fetcher.impl.*
import org.grails.gorm.graphql.fetcher.interceptor.InterceptingDataFetcher
import org.grails.gorm.graphql.fetcher.interceptor.InterceptorInvoker
import org.grails.gorm.graphql.fetcher.interceptor.MutationInterceptorInvoker
import org.grails.gorm.graphql.fetcher.interceptor.QueryInterceptorInvoker
import org.grails.gorm.graphql.interceptor.GraphQLSchemaInterceptor
import org.grails.gorm.graphql.types.GraphQLPropertyType
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

import static graphql.language.ObjectTypeDefinition.newObjectTypeDefinition
import static graphql.schema.GraphQLArgument.newArgument
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import static graphql.schema.GraphQLList.list
import static graphql.schema.GraphQLObjectType.newObject
import static org.grails.gorm.graphql.fetcher.GraphQLDataFetcherType.*

class GQLSchemaFactory extends Schema implements GrailsConfigurationAware {
    Resource gqlSchema
    def schemaGenerator = new SchemaGenerator()
    def schemaParser = new SchemaParser();

    TypeDefinitionRegistry schemaDefinitionRegistry = new TypeDefinitionRegistry();
    final List<FieldDefinition> queryDefList = new ArrayList<>();
    final List<FieldDefinition> mutationDefList = new ArrayList<>();
    final List<SDLDefinition> objectTypeExtensions = new ArrayList<>();


    GQLSchemaFactory(MappingContext... mappingContext) {
        super(mappingContext)
    }

    def loadidl() {
        GORMSchemaRegistry gormSchemaReg = gSchemaReg()
        def stream = new ClassPathResource(GQLConstants.SCHEMA_FILE_NAME).inputStream
        def schemasContent = [stream.text]
        TypeDefinitionRegistry loadedFromFileDefRegistry
        schemasContent.each { idlSchemaContent ->
            loadedFromFileDefRegistry = schemaParser.parse((String)idlSchemaContent);
            loadedFromFileDefRegistry.types().forEach { typeName, definition ->
                //Add only content of the root elements to our own Query and Mutation
                if (typeName.equalsIgnoreCase(GQLConstants.TYPE_QUERY)) {
                    queryDefList.addAll(childrenFieldsOfTypeDefinition((ObjectTypeDefinition) definition));
                } else if (typeName.equalsIgnoreCase(GQLConstants.TYPE_MUTATION)) {
                    mutationDefList.addAll(childrenFieldsOfTypeDefinition((ObjectTypeDefinition) definition));
                } else {
                    schemaDefinitionRegistry.add(definition);
                }
            }
        }
        schemaDefinitionRegistry.add(newObjectTypeDefinition().name(GQLConstants.TYPE_QUERY).fieldDefinitions(queryDefList).build());
        schemaDefinitionRegistry.add(newObjectTypeDefinition().name(GQLConstants.TYPE_MUTATION).fieldDefinitions(mutationDefList).build());
        objectTypeExtensions.each {
            schemaDefinitionRegistry.add(it)
        }


        def wiringFactory = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(new WireFactory())
                .build()
        def schema = schemaGenerator.makeExecutableSchema(schemaDefinitionRegistry, wiringFactory)
        gormSchemaReg.additionalTypes.addAll(schema.additionalTypes.toArray())
        return GraphQLSchema.newSchema()
                .codeRegistry( gormSchemaReg.codeRegistry.dataFetchers(schema.codeRegistry).build())
                .additionalTypes(gormSchemaReg.additionalTypes)
                .query(gormSchemaReg.query.fields(schema.queryType.fieldDefinitions).build())
                .mutation(gormSchemaReg.mutation.fields(schema.mutationType.fieldDefinitions).build())
                .build()
    }

    static List<FieldDefinition> childrenFieldsOfTypeDefinition(ObjectTypeDefinition definition) {
        return definition.getChildren().collect { node -> (FieldDefinition) node };
    }

    @Override
    GraphQLSchema generate() {
        def schema = loadidl()
        return schema
    }

    private GORMSchemaRegistry gSchemaReg() {

        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry()

        GraphQLObjectType.Builder queryType = newObject().name(GQLConstants.TYPE_QUERY)
        GraphQLObjectType.Builder mutationType = newObject().name(GQLConstants.TYPE_MUTATION)

        Set<PersistentEntity> childrenNotMapped = []

        for (MappingContext mappingContext : mappingContexts) {
            for (PersistentEntity entity : mappingContext.persistentEntities) {

                GraphQLMapping mapping = GraphQLEntityHelper.getMapping(entity)
                if (mapping == null) {
                    if (!entity.root) {
                        childrenNotMapped.add(entity)
                    }
                    continue
                }

                List<GraphQLFieldDefinition.Builder> queryFields = []
                List<GraphQLFieldDefinition.Builder> mutationFields = []

                GraphQLOutputType objectType = typeManager.getQueryType(entity, GraphQLPropertyType.OUTPUT)

                List<GraphQLFieldDefinition.Builder> requiresIdentityArguments = []
                List<Closure> postIdentityExecutables = []
                InterceptorInvoker queryInterceptorInvoker = new QueryInterceptorInvoker()

                ProvidedOperation getOperation = mapping.operations.get
                if (getOperation.enabled) {

                    DataFetcher getFetcher = dataFetcherManager.getReadingFetcher(entity, GET).orElse(new SingleEntityDataFetcher(entity))
                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_QUERY, namingConvention.getGet(entity)), new InterceptingDataFetcher(entity, serviceManager, queryInterceptorInvoker, GET, getFetcher))
                    GraphQLFieldDefinition.Builder queryOne = newFieldDefinition()
                            .name(namingConvention.getGet(entity))
                            .type(objectType)
                            .description(getOperation.description)
                            .deprecate(getOperation.deprecationReason)

                    requiresIdentityArguments.add(queryOne)
                    queryFields.add(queryOne)
                }

                ListOperation listOperation = mapping.operations.list
                if (listOperation.enabled) {

                    DataFetcher listFetcher = dataFetcherManager.getReadingFetcher(entity, LIST).orElse(null)

                    GraphQLFieldDefinition.Builder queryAll = newFieldDefinition()
                            .name(namingConvention.getList(entity))
                            .description(listOperation.description)
                            .deprecate(listOperation.deprecationReason)

                    if (listOperation.paginate) {
                        if (listFetcher == null) {
                            listFetcher = new PaginatedEntityDataFetcher(entity)
                        }
                        queryAll.type(typeManager.getQueryType(entity, GraphQLPropertyType.OUTPUT_PAGED))
                    } else {
                        if (listFetcher == null) {
                            listFetcher = new EntityDataFetcher(entity)
                        }
                        queryAll.type(list(objectType))
                    }

                    if (listFetcher instanceof PaginatingGormDataFetcher) {
                        ((PaginatingGormDataFetcher) listFetcher).responseHandler = paginationResponseHandler
                    }
                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_QUERY, namingConvention.getList(entity)), new InterceptingDataFetcher(entity, serviceManager, queryInterceptorInvoker, LIST, listFetcher))
                    queryFields.add(queryAll)

                    for (Map.Entry<String, GraphQLInputType> argument : listArguments) {
                        queryAll.argument(newArgument()
                                .name(argument.key)
                                .type(argument.value))
                    }
                }

                ProvidedOperation countOperation = mapping.operations.count
                if (countOperation.enabled) {

                    DataFetcher countFetcher = dataFetcherManager.getReadingFetcher(entity, COUNT).orElse(new CountEntityDataFetcher(entity))

                    GraphQLFieldDefinition.Builder queryCount = newFieldDefinition()
                            .name(namingConvention.getCount(entity))
                            .type((GraphQLOutputType) typeManager.getType(Integer))
                            .description(countOperation.description)
                            .deprecate(countOperation.deprecationReason)

                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_QUERY, namingConvention.getCount(entity)), new InterceptingDataFetcher(entity, serviceManager, queryInterceptorInvoker, COUNT, countFetcher))
                    queryFields.add(queryCount)
                }

                InterceptorInvoker mutationInterceptorInvoker = new MutationInterceptorInvoker()

                GraphQLDataBinder dataBinder = dataBinderManager.getDataBinder(entity.javaClass)

                ProvidedOperation createOperation = mapping.operations.create
                if (createOperation.enabled && !Modifier.isAbstract(entity.javaClass.modifiers)) {
                    if (dataBinder == null) {
                        throw new DataBinderNotFoundException(entity)
                    }
                    GraphQLInputType createObjectType = typeManager.getMutationType(entity, GraphQLPropertyType.CREATE, true)

                    BindingGormDataFetcher createFetcher = dataFetcherManager.getBindingFetcher(entity, CREATE).orElse(new CreateEntityDataFetcher(entity))

                    createFetcher.dataBinder = dataBinder

                    GraphQLFieldDefinition.Builder create = newFieldDefinition()
                            .name(namingConvention.getCreate(entity))
                            .type(objectType)
                            .description(createOperation.description)
                            .deprecate(createOperation.deprecationReason)
                            .argument(newArgument()
                                    .name(entity.decapitalizedName)
                                    .type(createObjectType))
                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_MUTATION, namingConvention.getCreate(entity)), new InterceptingDataFetcher(entity, serviceManager, mutationInterceptorInvoker, CREATE, createFetcher))

                    mutationFields.add(create)
                }

                ProvidedOperation updateOperation = mapping.operations.update
                if (updateOperation.enabled) {
                    if (dataBinder == null) {
                        throw new DataBinderNotFoundException(entity)
                    }
                    GraphQLInputType updateObjectType = typeManager.getMutationType(entity, GraphQLPropertyType.UPDATE, true)

                    BindingGormDataFetcher updateFetcher = dataFetcherManager.getBindingFetcher(entity, UPDATE).orElse(new UpdateEntityDataFetcher(entity))

                    updateFetcher.dataBinder = dataBinder

                    GraphQLFieldDefinition.Builder update = newFieldDefinition()
                            .name(namingConvention.getUpdate(entity))
                            .type(objectType)
                            .description(updateOperation.description)
                            .deprecate(updateOperation.deprecationReason)

                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_MUTATION, namingConvention.getUpdate(entity)), new InterceptingDataFetcher(entity, serviceManager, mutationInterceptorInvoker, UPDATE, updateFetcher))

                    postIdentityExecutables.add {
                        update.argument(newArgument()
                                .name(entity.decapitalizedName)
                                .type(updateObjectType))
                    }

                    requiresIdentityArguments.add(update)
                    mutationFields.add(update)
                }

                ProvidedOperation deleteOperation = mapping.operations.delete
                if (deleteOperation.enabled) {

                    DeletingGormDataFetcher deleteFetcher = dataFetcherManager.getDeletingFetcher(entity).orElse(new DeleteEntityDataFetcher(entity))

                    deleteFetcher.responseHandler = deleteResponseHandler

                    GraphQLFieldDefinition.Builder delete = newFieldDefinition()
                            .name(namingConvention.getDelete(entity))
                            .type(deleteResponseHandler.getObjectType(typeManager))
                            .description(deleteOperation.description)
                            .deprecate(deleteOperation.deprecationReason)

                    codeRegistry.dataFetcher(FieldCoordinates.coordinates(GQLConstants.TYPE_MUTATION, namingConvention.getDelete(entity)), new InterceptingDataFetcher(entity, serviceManager, mutationInterceptorInvoker, DELETE, deleteFetcher))

                    requiresIdentityArguments.add(delete)
                    mutationFields.add(delete)
                }

                populateIdentityArguments(entity, requiresIdentityArguments.toArray(new GraphQLFieldDefinition.Builder[0]))

                for (Closure c : postIdentityExecutables) {
                    c.call()
                }

                for (CustomOperation operation : mapping.customQueryOperations) {
                    queryFields.add(operation.createField(entity, serviceManager, mappingContext, listArguments))
                }

                for (CustomOperation operation : mapping.customMutationOperations) {
                    mutationFields.add(operation.createField(entity, serviceManager, mappingContext, Collections.emptyMap()))
                }

                for (GraphQLSchemaInterceptor schemaInterceptor : interceptorManager.interceptors) {
                    schemaInterceptor.interceptEntity(entity, queryFields, mutationFields)
                }

                queryType.fields(queryFields*.build())

                mutationType.fields(mutationFields*.build())
            }
        }

        Set<GraphQLType> additionalTypes = []

        for (PersistentEntity entity : childrenNotMapped) {
            GraphQLMapping mapping = GraphQLEntityHelper.getMapping(entity.rootEntity)
            if (mapping == null) {
                continue
            }

            additionalTypes.add(typeManager.getQueryType(entity, GraphQLPropertyType.OUTPUT))
        }

        for (GraphQLSchemaInterceptor schemaInterceptor : interceptorManager.interceptors) {
            schemaInterceptor.interceptSchema(queryType, mutationType, additionalTypes)
        }
        return new GORMSchemaRegistry(query: queryType, mutation: mutationType, additionalTypes: additionalTypes, codeRegistry: codeRegistry)
    }

    @Override
    void setConfiguration(Config co) {
        print("INJECT CONFIG")
    }
}
