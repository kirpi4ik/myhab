package eu.devexpert.madhouse.init

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = "eu.devexpert.madhouse.init.cache")
@ComponentScan(basePackages = "eu.devexpert.madhouse.graphql.fetchers")
@CompileStatic
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}