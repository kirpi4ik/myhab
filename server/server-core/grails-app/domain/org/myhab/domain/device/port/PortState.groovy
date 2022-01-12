package org.myhab.domain.device.port

/**
 *
 */
enum PortState {
  UNKNOW(0),
  CONFIGURED(1),
  ACTIVE(2),
  INACTIVE(3)
  int value;

  PortState(int value) {
    this.value = value;
  }

}