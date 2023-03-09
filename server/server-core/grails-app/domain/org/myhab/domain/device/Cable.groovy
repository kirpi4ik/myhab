package org.myhab.domain.device


import org.myhab.domain.common.BaseEntity
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone

class Cable extends BaseEntity {
    String code
    String codeNew
    String codeOld
    String description
    Double maxAmp
    Rack rack
    CableCategory category
    Set<DevicePeripheral> peripherals
    Set<DevicePort> connectedTo
    Set<Zone> zones
    PatchPanel patchPanel
    String patchPanelPort
    Integer nrWires
    Integer rackRowNr
    Integer orderInRow

    static belongsTo = [rack: Rack, patchPanel: PatchPanel, category: CableCategory]
    static hasMany = [connectedTo: DevicePort, peripherals: DevicePeripheral, zones: Zone]

    static constraints = {
        orderInRow nullable: true
        rackRowNr nullable: true
        nrWires nullable: true
        rack nullable: true
        patchPanel nullable: true
        category nullable: true
        code nullable: false
        codeNew nullable: true
        codeOld nullable: true
        description nullable: false
        patchPanelPort nullable: true
        maxAmp nullable: true
    }
    static mapping = {
        table '`cables`'
        sort code: "asc"
        zones joinTable: [name: "zones_cables_join", key: 'cable_id']
        connectedTo joinTable: [name: "device_ports_cables_join", key: 'cable_id']
        peripherals joinTable: [name: "cables_peripherals_join", key: 'cable_id']
    }

    static graphql = GraphQLMapping.lazy {
        query('cableById', Cable) {
            argument('id', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Cable.findById(environment.getArgument('id'))
                }
            })
        }


        mutation('updateCable', Cable) {
            argument('id', Long)
            argument('cable', Cable.class)
            returns Cable
            dataFetcher(new UpdateEntityDataFetcher(Cable.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    def cableId = environment.getArgument("id")
                    Cable cable = environment.getArgument("cable")
                    def existingCable
                    withTransaction(false) {
                        existingCable = Cable.get(cableId)

                        //cleanup ports
                        existingCable.connectedTo.each { p ->
                            def portExistInUpdate = cable.connectedTo.find { newPort ->
                                newPort.id == p.id
                            }
                            if (portExistInUpdate == null) {
                                //delete from db
                                CablePortJoin.get(cableId, p.id).delete(failOnError: true, flush: true)
                            }
                        }
                        //cleanup zones
                        existingCable.zones.each { zn ->
                            def zoneExistInUpdate = cable.zones.find { newZone ->
                                newZone.id == zn.id
                            }
                            if (zoneExistInUpdate == null) {
                                //delete from db
                                CableZoneJoin.get(cableId, zn.id).delete(failOnError: true, flush: true)
                            }
                        }

                        //copy cable
                        existingCable.properties = cable

                        //save ports
                        existingCable.connectedTo.each {
                            if (CablePortJoin.get(cableId, it.id) == null) {
                                new CablePortJoin(cable: existingCable, port: DevicePort.get(it.id)).save()
                            }
                        }
                        //save zones
                        existingCable.zones.each {
                            if (CableZoneJoin.get(cableId, it.id) == null) {
                                new CableZoneJoin(cable: existingCable, zone: Zone.get(it.id)).save()
                            }
                        }

                        existingCable.save(failOnError: true, flush: true)
                    }
                    return existingCable
                }
            })
        }
    }
}
