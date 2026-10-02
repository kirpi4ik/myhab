package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.Job
import org.myhab.services.voice.VoiceTools
import spock.lang.Specification

/**
 * buildCatalog against GORM: the catalog must serialise identically for an unchanged
 * installation, because the NLU sidecar retrains on every new catalog hash.
 */
class VoiceCatalogSpec extends Specification implements ServiceUnitTest<VoiceCommandService>, DataTest {

    Class[] getDomainClassesToMock() { [Device, DevicePort, DevicePeripheral, Zone, Configuration, Job] as Class[] }

    void "the catalog is stable across sessions: set-backed lists are sorted, aliases keep their row order"() {
        given:
            Device device = new Device(code: 'dev-1', name: 'Controller').save(validate: false, flush: true)
            DevicePort port = new DevicePort(internalRef: '1', name: 'out-1', device: device).save(validate: false, flush: true)
            Zone kitchen = new Zone(name: 'Kitchen').save(validate: false, flush: true)
            Zone hall = new Zone(name: 'Hall').save(validate: false, flush: true)
            ['Spot C', 'Spot A', 'Spot B'].each { name ->
                DevicePeripheral p = new DevicePeripheral(name: name, description: name)
                p.addToConnectedTo(port)
                p.addToZones(kitchen)
                p.addToZones(hall)
                p.save(validate: false, flush: true)
                kitchen.addToPeripherals(p)
                hall.addToPeripherals(p)
            }
            kitchen.save(validate: false, flush: true)
            hall.save(validate: false, flush: true)
            Long spotA = DevicePeripheral.findByName('Spot A').id
            new Configuration(key: VoiceCommandService.ALIAS_KEY, entityType: EntityType.PERIPHERAL, entityId: spotA,
                    value: 'reading light, lampa').save(validate: false, flush: true)
            new Configuration(key: VoiceCommandService.ALIAS_KEY, entityType: EntityType.PERIPHERAL, entityId: spotA,
                    value: 'desk lamp').save(validate: false, flush: true)

        when:
            Map first = service.buildCatalog()
            Zone.withSession { it.clear() }
            Map second = service.buildCatalog()

        then:
            VoiceTools.catalogJson(first) == VoiceTools.catalogJson(second)
            first.peripherals.every { it.zones == ['Hall', 'Kitchen'] }
            first.zones.every { it.peripherals == ['Spot A', 'Spot B', 'Spot C'] }
            first.peripherals.find { it.id == spotA }.aliases == ['reading light', 'lampa', 'desk lamp']
    }
}
