package org.myhab.domain.device.port

enum PortInputMode {
  P(0),
  PR(1),
  R(2),
  C(3),
  int value

  PortInputMode(int value) {
    this.value = value
  }

  static PortInputMode fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}

