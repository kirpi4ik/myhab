package eu.devexpert.madhouse.domain.device.port

/**
 *
 */
enum PortOutputState {
  OFF(0),
  ON(1),
  UNKNOW(2),
  int value;

  PortOutputState(int value) {
    this.value = value;
  }

  static PortOutputState fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : UNKNOW
  }
}