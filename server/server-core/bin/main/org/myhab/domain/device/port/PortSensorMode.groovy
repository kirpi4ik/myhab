package org.myhab.domain.device.port

enum PortSensorMode {
  NORM(0),
  GREATER(1),
  LOWER(2),
  DIFFERENT(3)
  int value

  PortSensorMode(int value) {
    this.value = value
  }

  static PortSensorMode fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}
