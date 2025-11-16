package org.myhab.domain.device.port


import grails.events.EventPublisher
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.job.EventData

/**
 * Avoid scanning this data, search only by keys
 */
class PortValue extends BaseEntity implements EventPublisher {
    /**
     * @deprecated Use 'portId' instead. This field is kept for backward compatibility only.
     * Will be removed in a future version.
     */
    @Deprecated
    String portUid
    Long portId
    String value
    EventData event

    static constraints = {
        event nullable: true
        portId nullable: true
        portUid nullable: true
    }
    static mapping = {
        table '`port_values`'
        version false
        value sqlType: 'text'  // Support large values (e.g., JSON arrays for time-series data)
    }

    void beforeInsert() {
        // Call parent beforeInsert
        super.beforeInsert()
        // Set portUid from portId for backward compatibility with database NOT NULL constraint
        // Always set portUid - use portId if available, otherwise use empty string
        if (!portUid) {
            portUid = portId ? portId.toString() : ''
        }
    }

    void afterInsert() {
        // No longer needed - uid column is now nullable
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
