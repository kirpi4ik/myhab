package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.dto.WSocketEvent
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import grails.plugin.springwebsocket.WebSocket
import org.joda.time.DateTime

@Transactional
class WSocketsService implements EventPublisher, WebSocket {

    int periodMs = 2000
    long lastEvent = DateTime.now().millis

    @Subscriber('evt_light')
    def evtLight() {
//        convertAndSendToUser("admin", "/queue/hello", "hello, target user!")
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_light"))
    }

    @Subscriber('evt_port_value_changed')
    def evtPortChanged() {
        if (DateTime.now().millis - lastEvent > periodMs) {
            convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_port_value_changed"))
            lastEvent = DateTime.now().millis
        }
    }

    @Subscriber('evt_heat')
    def evtHeat() {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_heat"))
    }

    @Subscriber('evt_device_status')
    def evtReadDeviceStatus() {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_device_status"))
    }
}
