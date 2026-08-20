package org.myhab.services

import grails.testing.services.ServiceUnitTest
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification
import spock.lang.Unroll

class ClientIpServiceSpec extends Specification implements ServiceUnitTest<ClientIpService> {

    private static final String PROXY = '192.168.1.200'

    private MockHttpServletRequest req(String peer, Map headers = [:]) {
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.remoteAddr = peer
        headers.each { k, v -> request.addHeader(k as String, v as String) }
        return request
    }

    void "a request through the proxy resolves to the address the proxy recorded"() {
        expect:
            service.resolve(req(PROXY, ['X-Real-IP': '192.168.1.44'])) == '192.168.1.44'
            service.isTrustedLan(req(PROXY, ['X-Real-IP': '192.168.1.44']))
    }

    void "a public caller through the proxy is not LAN"() {
        given:
            def request = req(PROXY, ['X-Real-IP': '203.0.113.7'])

        expect:
            service.resolve(request) == '203.0.113.7'
            !service.isTrustedLan(request)
    }

    void "a prepended X-Forwarded-For entry cannot fake a LAN origin"() {
        given: 'nginx appends the real peer, so the caller-supplied entry comes first'
            def request = req(PROXY, [
                    'X-Forwarded-For': '192.168.1.5, 203.0.113.7',
                    'X-Real-IP'      : '203.0.113.7'
            ])

        expect:
            service.resolve(request) == '203.0.113.7'
            !service.isTrustedLan(request)
    }

    void "without X-Real-IP only the last X-Forwarded-For entry counts"() {
        expect:
            service.resolve(req(PROXY, ['X-Forwarded-For': '192.168.1.5, 203.0.113.7'])) == '203.0.113.7'
            !service.isTrustedLan(req(PROXY, ['X-Forwarded-For': '192.168.1.5, 203.0.113.7']))
    }

    void "a LAN device hitting the backend directly is trusted"() {
        expect:
            service.resolve(req('192.168.1.44')) == '192.168.1.44'
            service.isTrustedLan(req('192.168.1.44'))
    }

    void "forwarding headers from a peer that is not a configured proxy are refused"() {
        given: 'a LAN host cannot promote itself by inventing headers'
            def request = req('192.168.1.44', ['X-Real-IP': '192.168.1.44'])

        expect:
            service.resolve(request) == null
            !service.isTrustedLan(request)
    }

    void "a proxied request carrying no forwarding header is not attributable"() {
        given:
            def request = req(PROXY)

        expect:
            service.resolve(request) == null
            !service.isTrustedLan(request)
    }

    @Unroll
    void "a non-IP forwarded value (#value) is denied rather than throwing"() {
        given:
            def request = req(PROXY, ['X-Real-IP': value])

        expect:
            !service.isTrustedLan(request)

        where:
            value << ['evil.example.com', 'not-an-ip', '999.999.999.999', '192.168.1.1; rm -rf /']
    }

    void "loopback is LAN over both address families"() {
        expect:
            service.isTrustedLan(req('127.0.0.1'))
            service.isTrustedLan(req('0:0:0:0:0:0:0:1'))
    }

    void "the proxy prefix is not enough on its own - public 192 space is refused"() {
        given: 'the old startsWith(\"192.\") check accepted these'
            def request = req(PROXY, ['X-Real-IP': '192.0.2.15'])

        expect:
            !service.isTrustedLan(request)
    }
}
