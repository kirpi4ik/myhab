package org.myhab.domain

/**
 * Each shared widget type is bound to the event topic it publishes on. The switch types
 * (everything but the gate) accept on/off; the gate accepts a single "open" pulse.
 *
 * <p>WATER_PUMP publishes on {@code evt_light} because that is what the water-pump card
 * already sends for the same peripheral — {@code UIMessageService} routes evt_light,
 * evt_switch and evt_sprinkler through the same handler, so this only keeps parity.</p>
 */
enum SharedWidgetType {
    GATE_ACCESS('evt_intercom_door_lock'),
    LIGHT('evt_light'),
    WATER_PUMP('evt_light'),
    SPRINKLER('evt_sprinkler')

    final String topic

    SharedWidgetType(String topic) {
        this.topic = topic
    }

    boolean isSwitch() {
        return this != GATE_ACCESS
    }
}
