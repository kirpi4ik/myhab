package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.infra.Zone
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class Cable extends BaseEntity {
  String code
  String codeNew
  String codeOld
  String description
  double maxAmp
  int nrWires
  Rack rack
  PeripheralCategory category
  Set<DevicePeripheral> peripherals
  Set<DevicePort> connectedTo
  Set<Zone> zones
  PatchPanel patchPanel
  String patchPanelPort

  static belongsTo = [DevicePort, DevicePeripheral, Zone, Rack,PatchPanel, PeripheralCategory]
  static hasMany = [connectedTo: DevicePeripheral, peripherals: DevicePeripheral, zones: Zone]
  static hasOne = [category: PeripheralCategory, rack: Rack, patchPanel: PatchPanel]

  static constraints = {
    rack nullable: true
    patchPanel nullable: true
    category nullable: true
    code nullable: true
    codeNew nullable: true
    codeOld nullable: true
    description nullable: true
    patchPanelPort nullable: true
  }
  static mapping = {
    table '`cables`'
    sort code: "asc"
    zones joinTable: [name: "zones_cables_join", key: 'cable_id']
    connectedTo joinTable: [name: "cables_ports_join", key: 'cable_id']
    peripherals joinTable: [name: "cables_peripherals_join", key: 'cable_id']
  }

  static graphql = GraphQLMapping.lazy {
    query('cableByUid', Cable) {
      argument('uid', String)
      dataFetcher(new DataFetcher() {
        @Override
        Object get(DataFetchingEnvironment environment) {
          Cable.findByUid(environment.getArgument('uid'))
        }
      })
    }
  }

}
