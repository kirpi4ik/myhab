package org.myhab.domain.common

class Event extends BaseEntity {
    String p0 // topic name
    String p1 // target type entity
    String p2 // target id
    String p3 // source of event
    String p4 // event parameter( like port action : on/off/rev  - see PortAction.class)
    String p5 // optional json parameter
    String p6 // event producer reference(username)
}
