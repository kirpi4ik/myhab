package org.myhab.domain.device.port

enum PortAnalogicMode {
  NORM(0),
  GREATER(1),
  LOWER(2),
  DIFFERENT(3)
  int value

  PortAnalogicMode(int value) {
    this.value = value
  }

  static PortAnalogicMode fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}
