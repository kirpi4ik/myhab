package eu.devexpert.madhouse.init.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("peripheral")
class ConfigPeripherals {
    String categories
}
