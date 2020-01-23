package eu.devexpert.madhouse.domain

/**
 *
 */
enum ConfigKey {
    HEAT_DELAY("heat.delay"),
    COLD_DELAY("cold.delay"),
    PERIPHERAL_ON_DELAY("peripheral.on.delay")
    def key

    ConfigKey(key) {
        this.key = key
    }
}
