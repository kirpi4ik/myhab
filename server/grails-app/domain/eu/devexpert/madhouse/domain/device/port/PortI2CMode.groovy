package eu.devexpert.madhouse.domain.device.port

enum PortI2CMode {
  NOT_CONFIGURED(0),
  SDA(1),
  SCL(2),
  int value

  PortI2CMode(int value) {
    this.value = value
  }

  static PortI2CMode fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}
