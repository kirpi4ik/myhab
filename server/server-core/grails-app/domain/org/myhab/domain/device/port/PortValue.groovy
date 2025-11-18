package org.myhab.domain.device.port


import grails.events.EventPublisher
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.job.EventData

/**
 * Avoid scanning this data, search only by keys
 */
class PortValue extends BaseEntity implements EventPublisher {
    Long portId
    String value
    EventData event

    static constraints = {
        event nullable: true
        portId nullable: true
    }
    static mapping = {
        table '`port_values`'
        version false
        value sqlType: 'text'  // Support large values (e.g., JSON arrays for time-series data)
    }

    void beforeInsert() {
        // Call parent beforeInsert
        super.beforeInsert()
    }

//    void afterInsert() {
//        def pVal = this;
//        log.trace 'After insert  [' + this.entityType + ']... ' + this.uid
//    }
//
//    @Publisher
//    void afterUpdate() {
//        def pVal = this;
//        log.trace 'After update  [' + this.entityType + ']... ' + this.uid
//        publish(TopicName.EVT_PORT_VALUE_CHANGED.id(), new EventData().with {
//            p0 = TopicName.EVT_PORT_VALUE_CHANGED.id()
//            p1 = EntityType.PORT.name()
//            p2 = "${pVal.id}"
//            p3 = "${pVal.uid}"
//            p4 = "${pVal.value}"
//            p5 = "mqtt"
//            it
//        })
//    }
    static graphql = true
}
