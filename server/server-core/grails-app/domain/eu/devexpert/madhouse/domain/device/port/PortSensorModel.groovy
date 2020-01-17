package eu.devexpert.madhouse.domain.device.port

/**
 *
 */
enum PortSensorModel {
  DHT11(1),
  DHT22(2),
  W1(3),
  W1BUS(5),
  IB(4)
  int value

  PortSensorModel(int value) {
    this.value = value
  }

  static PortSensorModel fromValue(String intValue) {
    return intValue != null ? values().find {
      it.value == Integer.valueOf(intValue)
    } : null
  }
}