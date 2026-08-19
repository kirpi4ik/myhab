package org.myhab.init


import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = [
        "org.myhab.init.cache",
        "org.myhab.config", // Keep scanning for CustomPostgreSQLDelegate
        "org.myhab.async.socket",
        "org.myhab.async.mqtt",
        "org.myhab.async.mqtt.handlers",
        "org.myhab.graphql.fetchers"])
@CompileStatic
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        // Set default timezone to UTC for the entire application
        // This ensures all Date operations use UTC unless explicitly specified
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        
        GrailsApp.run(Application, args)
    }
}