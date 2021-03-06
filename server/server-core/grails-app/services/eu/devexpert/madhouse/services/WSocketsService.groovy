package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.dto.WSocketEvent
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import grails.plugin.springwebsocket.WebSocket

@Transactional
class WSocketsService implements EventPublisher, WebSocket {

    @Subscriber('evt_light')
    def hello() {
//        convertAndSendToUser("admin", "/queue/hello", "hello, target user!")
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_light"))
    }
}
