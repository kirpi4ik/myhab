package org.myhab.services

import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.domain.MessageLevel
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction

/**
 * Raises an in-app notification when a peripheral in a watched category changes state.
 *
 * <p>Driven by {@code evt_port_value_persisted} — the device-confirmed value, not the
 * command. A valve that fails to actuate produces no message; one moved from the vendor's
 * own app, or auto-closed by {@code key.on.timeout}, produces one just the same.</p>
 *
 * <p>Only VALVE today: unattended water is the case worth an inbox row, and lights and
 * switches would drown it. Adding a category to {@link #WATCHED_CATEGORIES} is the whole
 * change needed to cover another one.</p>
 */
@Slf4j
@Transactional
class PeripheralStateNotificationService {

    /** Peripheral categories whose ON/OFF transitions raise a notification. */
    static final Set<String> WATCHED_CATEGORIES = ['VALVE'].toSet()

    /**
     * Long enough to swallow a duplicate echo, short enough that a real
     * open-close-open cycle still reports every leg.
     */
    static final int COOLDOWN_MINUTES = 1

    NotificationService notificationService

    @Subscriber('evt_port_value_persisted')
    void portValuePersisted(event) {
        // publish() is synchronous, so this runs on the thread that persisted the port
        // value — a notification bug must not break that pipeline.
        try {
            String value = event.data.p4
            if (value != PortAction.ON.name() && value != PortAction.OFF.name()) {
                return
            }
            DevicePort port = DevicePort.get(Long.valueOf(event.data.p2 as String))
            port?.peripherals?.each { DevicePeripheral peripheral ->
                if (peripheral.category?.name in WATCHED_CATEGORIES) {
                    notifyStateChange(peripheral, value)
                }
            }
        } catch (Exception ex) {
            log.error("Failed to raise state-change notification: ${ex.message}", ex)
        }
    }

    private void notifyStateChange(DevicePeripheral peripheral, String value) {
        boolean open = value == PortAction.ON.name()
        String zone = peripheral.zones?.find()?.name
        String where = zone ? "${peripheral.name} (${zone})" : peripheral.name
        notificationService.notifyAdmins(
                MessageLevel.INFO,
                "${peripheral.name} ${open ? 'opened' : 'closed'}".toString(),
                "${where} is now ${open ? 'OPEN' : 'CLOSED'}.".toString(),
                peripheral.category.name.toLowerCase(),
                // `peripheral.` prefix so a KEY_PREFIX notification rule can mute either
                // this one peripheral or every state-change message at once.
                "peripheral.${peripheral.id}.${value.toLowerCase()}".toString(),
                COOLDOWN_MINUTES)
    }
}
