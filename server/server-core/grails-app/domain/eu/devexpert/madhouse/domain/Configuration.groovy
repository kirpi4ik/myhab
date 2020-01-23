package eu.devexpert.madhouse.domain

class Configuration {

    String key
    EntityType entityType
    String name
    String value
    String description

    static mapping = {
        table '`configurations`'
    }
}
