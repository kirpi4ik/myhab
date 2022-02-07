package org.myhab.domain.device.port

/**
 *
 */
enum PortInputAction {
  OPEN_CLOSE(0),
  OPEN_CLOSE_OPEN(1),
  CLOSE_OPEN(2),
  int value;

  PortInputAction(int value) {
    this.value = value;
  }

}