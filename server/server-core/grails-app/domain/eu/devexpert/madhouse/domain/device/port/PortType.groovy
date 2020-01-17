package eu.devexpert.madhouse.domain.device.port

/**
 *
 */
enum PortType {
  UNKNOW(-1, 60000),
  IN(0, -1),
  OUT(1, 5000),
  ADC(2, -1),
  DSEN(3, 600000),
  I2C(4, 1000),
  NOT_CONFIGURED(255, -1),
  int value
  int syncMs

  PortType(int value, int syncMs) {
    this.value = value;
    this.syncMs = syncMs;
  }

  static PortType fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}