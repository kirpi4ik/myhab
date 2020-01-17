package eu.devexpert.madhouse.domain.heating

/**
 *
 */
enum HeatMode {
  LOW(20),
  MANUAL(-1),
  HIGH(24)

  int digree
  HeatMode(digree) {
    this.digree = digree
  }
}
