package org.myhab.demo

import groovy.util.logging.Slf4j
import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig

/**
 * In-process MQTT broker for local runs (`./gradlew demoSim`), so developing the demo
 * needs no mosquitto install. The deployed stack uses the real eclipse-mosquitto
 * container instead, matching the broker production runs against.
 */
@Slf4j
class EmbeddedBroker {

    static void start(int port) {
        def props = new Properties()
        props.setProperty('host', '0.0.0.0')
        props.setProperty('port', String.valueOf(port))
        props.setProperty('allow_anonymous', 'true')
        // 0 disables the websocket listener; the demo only speaks plain TCP.
        props.setProperty('websocket_port', '0')
        props.setProperty('persistence_enabled', 'false')
        // Moquette still wants somewhere to stash its instance UUID; point it at the
        // build directory rather than letting it fail noisily against the CWD.
        props.setProperty('data_path', System.getProperty('java.io.tmpdir'))

        def server = new Server()
        server.startServer(new MemoryConfig(props))
        Runtime.runtime.addShutdownHook(new Thread({ server.stopServer() }))
        log.info("Embedded MQTT broker listening on ${port}")
    }
}
