package org.myhab.controller

import grails.converters.JSON
import grails.events.EventPublisher
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.services.ClientIpService

@Secured(['permitAll'])
class EventController implements EventPublisher {
    static responseFormats = ['json', 'xml']
    static allowedMethods = [pubGetEvent: 'GET']

    ClientIpService clientIpService

    /**
     * Device push endpoint. Anonymous, so it is restricted to callers that
     * demonstrably sit on the LAN — everything published here reaches the
     * automation bus, and no subscriber does its own authorization.
     */
    def pubGetEvent() {
        if (!clientIpService.isTrustedLan(request)) {
            log.warn("Rejected event publish from ${clientIpService.resolve(request) ?: request.remoteAddr}")
            response.status = 403
            render([success: false, error: 'Forbidden'] as JSON)
            return
        }

        EventData input = eventFromParams()

        if (input.p0) {
            publish(input.p0, input)
            publish(TopicName.EVT_LOG.id(), input)
        } else if (params.mdid && params.pt) {
            publish(TopicName.EVT_DEVICE_PUSH.id(), params)
        } else {
            log.info("Event triggered with:  $params")
            response.status = 204
        }
    }

    /**
     * Binds only the event fields. Wholesale binding would also accept
     * BaseEntity's tsCreated, letting a caller backdate the event_log row.
     */
    private EventData eventFromParams() {
        return new EventData(
                p0: params.p0, p1: params.p1, p2: params.p2, p3: params.p3,
                p4: params.p4, p5: params.p5, p6: params.p6,
                actionId: params.actionId, category: params.category
        )
    }
}
