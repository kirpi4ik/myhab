package org.myhab.domain.device.port

/**
 *
 */
enum PortAction {
  OFF(0),
  ON(1),
  TOGGLE(2),
  SAME_IN(3),
  REVERSE_OUT(4),
  int value;

  PortAction(int value) {
    this.value = value;
  }

  static PortAction fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}