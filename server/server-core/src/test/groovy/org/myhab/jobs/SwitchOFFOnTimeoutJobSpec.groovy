package org.myhab.jobs

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import grails.testing.gorm.DataTest
import org.myhab.domain.MessageLevel
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.TopicName
import org.myhab.services.MegaDriverService
import org.myhab.services.NotificationService
import spock.lang.Specification

/**
 * Sweep contract: an expired deadline entry is never dropped at publish time —
 * it survives (with a bumped retry deadline) until the device confirms OFF,
 * which PortValueService observes and removes. Repeated unconfirmed attempts
 * escalate to the direct-HTTP MegaD command plus an admin alert, so a light
 * physically stuck ON cannot go unnoticed.
 */
class SwitchOFFOnTimeoutJobSpec extends Specification implements DataTest {

    SwitchOFFOnTimeoutJob job
    IMap expireMap
    List published
    Device device
    DevicePort port

    void setupSpec() {
        mockDomains(Device, DevicePort)
    }

    def setup() {
        job = new SwitchOFFOnTimeoutJob()
        expireMap = Mock(IMap)
        job.hazelcastInstance = Mock(HazelcastInstance) {
            getMap(_) >> expireMap
        }
        job.megaDriverService = Mock(MegaDriverService)
        job.notificationService = Mock(NotificationService)
        published = []
        job.metaClass.publish = { String ns, Object d -> published << [topic: ns, data: d] }

        device = new Device(code: 'mp50', model: DeviceModel.MEGAD_2561_RTC)
        port = new DevicePort(internalRef: '12', value: 'ON')
        device.addToPorts(port)
        device.save(flush: true, failOnError: true)
    }

    private static Map.Entry cacheEntry(def key, Map value) {
        new AbstractMap.SimpleEntry(String.valueOf(key), value)
    }

    private static long past() { System.currentTimeMillis() - 1000 }

    private static long future() { System.currentTimeMillis() + 100_000 }

    void "an expired entry publishes OFF and is re-armed for retry instead of being dropped"() {
        when:
            job.checkCacheAndSwitchOffAfterTimeout(null)

        then:
            1 * expireMap.entrySet() >> [cacheEntry(port.id, [expireOn: past(), peripheralId: 7L])]
            published.size() == 1
            published[0].topic == TopicName.EVT_LIGHT.id()
            published[0].data.p2 == 7L
            published[0].data.p4 == 'off'
            1 * expireMap.put(String.valueOf(port.id),
                    { it.offAttempts == 1 && it.peripheralId == 7L && it.expireOn > System.currentTimeMillis() })
            0 * expireMap.remove(_)
            0 * job.megaDriverService._
            0 * job.notificationService._
    }

    void "an expired entry whose port is already OFF in the DB is removed without publishing"() {
        given:
            port.value = 'OFF'
            port.save(flush: true, failOnError: true)

        when:
            job.checkCacheAndSwitchOffAfterTimeout(null)

        then:
            1 * expireMap.entrySet() >> [cacheEntry(port.id, [expireOn: past(), peripheralId: 7L])]
            1 * expireMap.remove(String.valueOf(port.id))
            0 * expireMap.put(*_)
            published.isEmpty()
    }

    void "repeated unconfirmed attempts escalate to direct HTTP OFF and an admin alert"() {
        when:
            job.checkCacheAndSwitchOffAfterTimeout(null)

        then:
            1 * expireMap.entrySet() >> [cacheEntry(port.id,
                    [expireOn: past(), peripheralId: 7L, offAttempts: SwitchOFFOnTimeoutJob.ESCALATE_AFTER_ATTEMPTS])]
            published.size() == 1
            1 * job.megaDriverService.writePortValues({ it.code == 'mp50' }, ['12': PortAction.OFF])
            1 * job.notificationService.notifyAdmins(MessageLevel.ERROR, _, _, _, "autooff.${port.id}", _)
            1 * expireMap.put(String.valueOf(port.id),
                    { it.offAttempts == SwitchOFFOnTimeoutJob.ESCALATE_AFTER_ATTEMPTS + 1 })
    }

    void "an unexpired entry is left untouched"() {
        when:
            job.checkCacheAndSwitchOffAfterTimeout(null)

        then:
            1 * expireMap.entrySet() >> [cacheEntry(port.id, [expireOn: future(), peripheralId: 7L])]
            0 * expireMap.put(*_)
            0 * expireMap.remove(_)
            published.isEmpty()
    }

    void "an entry without a peripheralId is removed so it cannot stick forever"() {
        when:
            job.checkCacheAndSwitchOffAfterTimeout(null)

        then:
            1 * expireMap.entrySet() >> [cacheEntry(port.id, [expireOn: past()])]
            1 * expireMap.remove(String.valueOf(port.id))
            0 * expireMap.put(*_)
            published.isEmpty()
    }
}
