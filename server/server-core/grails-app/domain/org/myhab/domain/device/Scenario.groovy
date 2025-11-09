package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.port.DevicePort
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.CreateEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher
import org.myhab.domain.device.port.PortScenarioJoin
import org.myhab.domain.job.Job

class Scenario extends BaseEntity {
  String name
  String body
  Set<DevicePort> ports

  static hasMany = [ports: DevicePort]
  static belongsTo = DevicePort

  static constraints = {
    name nullable: true
    body nullable: true
  }

  static mapping = {
    table '`scenarios`'
    ports joinTable: [name: "device_ports_scenarios_join", key: 'scenario_id']
  }

  static graphql = GraphQLMapping.lazy {
    mutation('scenarioCreate', Scenario) {
      argument('scenario', Scenario.class)
      returns Scenario
      dataFetcher(new CreateEntityDataFetcher<Scenario>(Scenario.gormPersistentEntity) {
        @Override
        Scenario get(DataFetchingEnvironment environment) throws Exception {
          def scenario = environment.getArgument("scenario")
          Scenario dbScenario = new Scenario()
          withTransaction(false) {
            // Copy basic properties
            scenario.entrySet().findAll { entry -> !(entry.value instanceof Collection) }.each { entry ->
              if (Scenario.metaClass.hasProperty(dbScenario, entry.key) && entry.key != 'metaClass' && entry.key != 'class') {
                dbScenario.setProperty(entry.key, entry.value)
              }
            }
            
            // Add ports
            if (scenario.ports) {
              scenario.ports.each { port ->
                if (dbScenario.ports.find { p -> p.id == port.id } == null) {
                  dbScenario.addToPorts(DevicePort.get(port.id))
                }
              }
            }
            
            dbScenario.save(failOnError: true, flush: true)
          }
          return dbScenario
        }
      })
    }

    mutation('updateScenario', Scenario) {
      argument('id', Long)
      argument('scenario', Scenario.class)
      returns Scenario
      dataFetcher(new UpdateEntityDataFetcher(Scenario.gormPersistentEntity) {
        @Override
        Object get(DataFetchingEnvironment environment) throws Exception {
          def scenarioId = environment.getArgument("id")
          Scenario scenario = environment.getArgument("scenario")
          def existingScenario
          withTransaction(false) {
            existingScenario = Scenario.get(scenarioId)
            
            // Cleanup ports that are no longer connected
            existingScenario.ports.each { p ->
              def portExistInUpdate = scenario.ports?.find { newPort ->
                newPort.id == p.id
              }
              if (portExistInUpdate == null) {
                // Delete from join table
                PortScenarioJoin.get(p.id, scenarioId)?.delete(failOnError: true, flush: true)
              }
            }
            
            // Update only specific properties (exclude uid, id, timestamps)
            if (scenario.name != null) existingScenario.name = scenario.name
            if (scenario.body != null) existingScenario.body = scenario.body
            
            // Add new ports
            scenario.ports?.each { port ->
              if (PortScenarioJoin.get(port.id, scenarioId) == null) {
                new PortScenarioJoin(port: DevicePort.get(port.id), scenario: existingScenario).save()
              }
            }
            
            existingScenario.save(failOnError: true, flush: true)
          }
          return existingScenario
        }
      })
    }

  }
}
