package org.myhab.services

import org.myhab.domain.device.port.PortAction
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import org.myhab.services.dsl.action.PowerService
import org.springframework.transaction.annotation.Propagation

@Transactional
class PresenceService {
    PowerService powerService

    @Subscriber("evt_presence")
    @Transactional(propagation = Propagation.REQUIRED)
    def sensor_presence_on(event) {
//        powerService.execute([portIds: [2241], action: PortAction.TOGGLE])
    }
}
