package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.services.dsl.action.PowerService
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation

@Transactional
class PresenceService {
    PowerService powerService

    @Subscriber("evt_presence")
    @Transactional(propagation = Propagation.REQUIRED)
    def sensor_presence_on(event) {
        powerService.execute([portIds: [2241], action: PortAction.TOGGLE])
    }
}
