package org.myhab.domain.device.port

enum PortOutputMode {
  SW(0),
  PWM(1),
  DS2413(2)
  int value

  PortOutputMode(int value) {
    this.value = value
  }

  static PortOutputMode fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}

