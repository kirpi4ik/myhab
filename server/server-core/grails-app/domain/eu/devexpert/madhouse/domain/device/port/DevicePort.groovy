package eu.devexpert.madhouse.domain.device.port

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.CablePortJoin
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.Scenario
import eu.devexpert.madhouse.domain.job.EventSubscription
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher

class DevicePort extends BaseEntity implements Configurable<DevicePort> {
    String internalRef
    String name
    String description
    Device device
    PortType type = PortType.UNKNOW
    PortState state = PortState.UNKNOW
    Set<DevicePeripheral> peripherals
    Set<Cable> cables
    Set<Scenario> scenarios
    Set<EventSubscription> subscriptions
    String value

    static hasMany = [cables: Cable, peripherals: DevicePeripheral, scenarios: Scenario, subscriptions: EventSubscription]
    static belongsTo = [device: Device]


    static constraints = {
        internalRef nullable: false
        name nullable: true
        type nullable: true
        state nullable: true
        description nullable: true
        value nullable: true
    }
    static mapping = {
        table '`device_ports`'
        cables joinTable: [name: "device_ports_cables_join", key: 'port_id']
        peripherals joinTable: [name: "device_ports_peripherals_join", key: 'port_id']
        scenarios joinTable: [name: "device_ports_scenarios_join", key: 'port_id']
        sort name: "asc"
    }
    static graphql = GraphQLMapping.lazy {
        mutation('updatePort', DevicePort) {
            argument('id', Long)
            argument('port', DevicePort.class)
            returns DevicePort
            dataFetcher(new UpdateEntityDataFetcher(DevicePort.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    def portId = environment.getArgument("id")
                    DevicePort port = environment.getArgument("port")
                    def existingPort
                    withTransaction(false) {
                        existingPort = DevicePort.get(portId)
                        //cleanup ports
                        existingPort.peripherals.each { p ->
                            def peripheralsExistInUpdate = port.peripherals.find { newPeriph ->
                                newPeriph.id == p.id
                            }
                            if (peripheralsExistInUpdate == null) {
                                //delete from db
                                PortPeripheralJoin.get(portId, p.id).delete(failOnError: true, flush: true)
                            }
                        }
                        existingPort.cables.each { cb ->
                            def cableExistInUpdate = port.cables.find { newCable ->
                                newCable.id == cb.id
                            }
                            if (cableExistInUpdate == null) {
                                //delete from db
                                CablePortJoin.get(cb.id, portId).delete(failOnError: true, flush: true)
                            }
                        }
                        //copy port
                        existingPort.properties = port

                        //save ports
                        existingPort.peripherals.each {
                            if (PortPeripheralJoin.get(portId, it.id) == null) {
                                new PortPeripheralJoin(port: existingPort, peripheral: DevicePeripheral.get(it.id)).save()
                            }
                        }
                        existingPort.cables.each {
                            if (CablePortJoin.get(it.id, portId) == null) {
                                new CablePortJoin(port: existingPort, cable: Cable.get(it.id)).save()
                            }
                        }

                        existingPort.save(failOnError: true, flush: true)
                    }
                    return existingPort
                }
            })
        }
        add('configurations', [Configuration]) {
            dataFetcher { BaseEntity entity ->
                Configuration.where {
                    entityId == entity.id && entityType == EntityType.get(entity)
                }.order("name", "desc")
            }
            input false
        }
        query('portTypes', [String]) {
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    def types = []
                    PortType.values().each {
                        types << it.name()
                    }
                    return types
                }
            })
        }
        query('portStates', [String]) {
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    def states = []
                    PortState.values().each {
                        states << it.name()
                    }
                    return states
                }
            })
        }
    }
}

