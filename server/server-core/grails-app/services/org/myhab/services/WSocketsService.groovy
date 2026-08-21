package org.myhab.services

import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import grails.plugin.springwebsocket.WebSocket
import groovy.json.JsonOutput
import org.joda.time.DateTime
import org.myhab.async.mqtt.MQTTMessage
import org.myhab.dto.WSocketEvent

@Transactional
class WSocketsService implements EventPublisher, WebSocket {

    int periodMs = 2000
    long lastEvent = DateTime.now().millis

    /**
     * Relay a raw inbound MQTT message to the SPA MQTT Explorer. Called directly
     * (not via the throttled @Subscriber path) for every received message so the
     * tree/raw-monitor see the full stream. {@code msg} carries the parsed
     * device/port fields when the topic matched a known pattern (null otherwise).
     * When no client is subscribed to /topic/mqtt the simple broker just drops it.
     */
    void broadcastRawMqtt(String topic, String payload, MQTTMessage msg) {
        convertAndSend("/topic/mqtt", [
                topic     : topic,
                payload   : payload,
                ts        : System.currentTimeMillis(),
                deviceCode: msg?.deviceCode as String,
                portType  : msg?.portType as String,
                portCode  : msg?.portCode as String
        ])
    }

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

    @Subscriber('evt_stat_value_changed')
    def evtStatChanged() {
        if (DateTime.now().millis - lastEvent > periodMs) {
            convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_stat_value_changed"))
            lastEvent = DateTime.now().millis
        }
    }

    @Subscriber('evt_ui_update_port_value')
    def mqttPortChanged(event) {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_port_value_persisted", jsonPayload: JsonOutput.toJson(event.data)))
    }

    /**
     * A new in-app notification was raised. Broadcast so open clients refetch their
     * own unread count — without this the bell badge only refreshes on remount.
     */
    @Subscriber('evt_user_notification')
    def userNotification() {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_user_notification"))
    }

    @Subscriber('evt_cfg_value_changed')
    def cfgChanged(event) {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_cfg_value_changed", jsonPayload: JsonOutput.toJson(event.data)))
    }

    /**
     * Forward git-backed app-config edits (mqtt.*, telegram.*, ui.*, …) to
     * the SPA so widgets reading from {@code useAppConfigStore} stay in sync
     * without a page reload. Published by the {@code appConfigUpdate}
     * GraphQL mutation after a successful {@code configProvider.setAndCommit}.
     */
    @Subscriber('evt_app_cfg_value_changed')
    def appCfgChanged(event) {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_app_cfg_value_changed", jsonPayload: JsonOutput.toJson(event.data)))
    }

    /**
     * Notify open /wui viewers and editors that a dashboard screen changed
     * (created/updated/deleted, incl. layout saves and background uploads).
     * Called directly from DashboardScreen GORM hooks (like broadcastRawMqtt,
     * not via the @Subscriber event bus) so every write path is covered.
     */
    void broadcastDashboardScreenChanged(Long screenId) {
        convertAndSend("/topic/events", new WSocketEvent(
                eventName: org.myhab.domain.events.TopicName.EVT_DASHBOARD_SCREEN_CHANGED.id(),
                jsonPayload: JsonOutput.toJson([p1: 'DASHBOARD_SCREEN', p2: screenId as String])))
    }

    @Subscriber('evt_heat')
    def evtHeat() {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_heat"))
    }

    @Subscriber('evt_device_status')
    def deviceStatusChanged() {
        convertAndSend("/topic/events", new WSocketEvent(eventName: "evt_device_status"))
    }

    /**
     * The demo sandbox was restored to its seed. Open tabs are holding ids that no
     * longer exist, so they must refetch rather than wait for the next navigation.
     * Called directly by DemoService - there is no event bus topic for this.
     */
    void broadcastDemoReset() {
        // Carries a payload because the client's useWebSocketListener composable
        // JSON.parse()s jsonPayload unconditionally.
        convertAndSend("/topic/events", new WSocketEvent(
                eventName: "evt_demo_reset",
                jsonPayload: JsonOutput.toJson([ts: System.currentTimeMillis()])))
    }


}
