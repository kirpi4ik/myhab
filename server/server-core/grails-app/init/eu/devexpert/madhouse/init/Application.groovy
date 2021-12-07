package eu.devexpert.madhouse.init

import eu.devexpert.madhouse.async.socket.WebSocketConfig
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@ComponentScan(basePackages = [
        "eu.devexpert.madhouse.init.cache",
        "eu.devexpert.madhouse.async.socket",
        "eu.devexpert.madhouse.async.mqtt",
        "eu.devexpert.madhouse.async.mqtt.handlers",
        "eu.devexpert.madhouse.graphql.fetchers"],
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSocketConfig))
@CompileStatic
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}