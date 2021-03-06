package eu.devexpert.madhouse.init

import eu.devexpert.madhouse.init.socket.WebSocketConfig
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

@ComponentScan(basePackages = [
        "eu.devexpert.madhouse.init.cache",
        "eu.devexpert.madhouse.init.socket",
        "eu.devexpert.madhouse.init.configuration",
        "eu.devexpert.madhouse.graphql.fetchers"],
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSocketConfig))
//@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
@CompileStatic
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}