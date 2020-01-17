package server.grails

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class FromPluginSpec extends Specification implements DomainUnitTest<FromPlugin> {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        expect:"fix me"
            true == false
    }
}
