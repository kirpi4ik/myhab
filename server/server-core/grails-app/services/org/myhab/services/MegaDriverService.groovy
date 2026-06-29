package org.myhab.services

import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceAccount
import org.myhab.domain.device.DeviceBackup
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.NetworkAddress
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.device.port.PortType
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.utils.DeviceHttpService
import org.myhab.utils.MegaDUdpTransport

import java.util.regex.Matcher

@Slf4j
class MegaDriverService implements EventPublisher {
    def dslService
    def mqttTopicService

    // ==================== UDP Discovery ====================

    /**
     * Broadcast UDP scan to discover MegaD controllers on the local network.
     * Returns list of maps: [ip: "x.x.x.x", bootloaderMode: true/false, mqttId: "..."]
     * Optionally enriches with MQTT device ID by reading cf=2 page via HTTP.
     */
    List<Map<String, Object>> discoverDevices(boolean enrichWithHttp = false) {
        def devices = MegaDUdpTransport.discoverDevices()

        if (enrichWithHttp) {
            devices.each { device ->
                if (!device.bootloaderMode) {
                    try {
                        // Try to read MQTT ID from device — needs password, try common defaults
                        def tempDevice = new Device(
                                model: DeviceModel.MEGAD_2561_RTC,
                                networkAddress: new NetworkAddress(ip: device.ip, port: "80")
                        )
                        tempDevice.authAccounts = [new DeviceAccount(password: "sec")] as Set
                        def page = httpClient(tempDevice, "?cf=2").readState()
                        def fields = extractFormFields(page)
                        if (fields.mdid) {
                            device.mqttId = fields.mdid
                        }
                    } catch (Exception e) {
                        log.debug("Could not read MQTT ID from {}: {}", device.ip, e.message)
                    }
                }
            }
        }

        return devices
    }

    /**
     * Ping a specific device via UDP to check reachability.
     */
    boolean pingDevice(String ip) {
        return MegaDUdpTransport.pingDevice(ip)
    }

    // ==================== Device Initialization ====================

    /**
     * Initialize a Device domain object from an exported MegaD configuration (temp.cf format).
     * Returns an unsaved Device — caller is responsible for persisting.
     *
     * @param configContent the raw temp.cf content (lines of URL query strings)
     * @param deviceCode optional device code; if null, extracted from cf=2 mdid field
     */
    Device initializeFromConfig(String configContent, String deviceCode = null) {
        def lines = configContent.split('\n').collect { it.trim() }.findAll { it }

        Map<String, String> generalConfig = [:]
        Map<String, String> mqttConfig = [:]
        List<Map<String, String>> portConfigs = []

        lines.each { line ->
            Map<String, String> fields = parseConfigLine(line)
            if (fields.cf == '1') {
                generalConfig = fields
            } else if (fields.cf == '2') {
                mqttConfig = fields
            } else if (fields.pn != null) {
                portConfigs << fields
            }
            // cf=7 (cron), cf=8 (keys), cf=10 (programs) are stored in backup but not mapped to domain
        }

        if (!deviceCode) {
            deviceCode = mqttConfig.mdid ?: "megad-${generalConfig.eip ?: 'unknown'}"
        }

        def device = new Device(
                code: deviceCode,
                name: deviceCode,
                model: DeviceModel.MEGAD_2561_RTC,
                status: DeviceStatus.OFFLINE,
                networkAddress: new NetworkAddress(
                        ip: generalConfig.eip ?: '',
                        gateway: generalConfig.gw ?: '',
                        port: '80'
                )
        )

        def account = new DeviceAccount(
                username: '',
                password: generalConfig.pwd ?: 'sec',
                isDefault: true,
                device: device
        )
        device.authAccounts = [account] as Set

        portConfigs.each { Map<String, String> fields ->
            String portNr = fields.pn
            String ptyValue = fields.pty
            PortType portType = PortType.fromValue(ptyValue) ?: PortType.NOT_CONFIGURED

            if (portType == PortType.NOT_CONFIGURED) {
                return // skip unconfigured ports
            }

            def port = new DevicePort(
                    internalRef: portNr,
                    name: "P${portNr}",
                    type: portType,
                    description: '',
                    device: device
            )

            // Store all config fields as Configuration entries
            def configKeys = ['ecmd', 'af', 'eth', 'naf', 'misc', 'd', 'mt', 'm', 'hst', 'grp']
            configKeys.each { key ->
                if (fields.containsKey(key)) {
                    port.configurations << new Configuration(
                            entityType: EntityType.PORT,
                            key: "megad.port.${key}",
                            value: fields[key] ?: ''
                    )
                }
            }

            device.addToPorts(port)
        }

        return device
    }

    // ==================== Read Configuration ====================

    /**
     * Read full device configuration from a live MegaD controller.
     * Returns a structured map with all config sections.
     */
    Map<String, Object> readFullConfig(Device device) {
        Map<String, Object> config = [:]

        // Required pages — failures here mean the device is unreachable.
        config.general = readConfigPage(device, "?cf=1")
        config.mqtt = readConfigPage(device, "?cf=2")

        // Optional pages — older / minimal firmwares simply hang the request.
        // Use a short timeout and quiet logging so init doesn't drag for 40+ seconds.
        config.cron = readConfigPage(device, "?cf=7", true)
        config.keys = readConfigPage(device, "?cf=8", true)

        // Probe program slot 0 first; if it isn't supported, skip 1..9
        // (otherwise we'd burn 9 × OPTIONAL_TIMEOUT_MS waiting for nothing).
        def slot0 = readConfigPage(device, "?cf=10&prn=0", true)
        config.programs = [slot0]
        if (slot0) {
            config.programs.addAll((1..9).collect { n -> readConfigPage(device, "?cf=10&prn=${n}", true) })
        } else {
            log.debug("Device {} does not respond on cf=10 (programs); skipping slots 1..9", device.code)
        }

        // Detect port count from main page and read each port
        def portCount = detectPortCount(device)
        config.ports = (0..<portCount).collect { n ->
            def portConfig = readConfigPage(device, "?pt=${n}")
            portConfig.put('_portNr', "${n}")
            portConfig
        }

        return config
    }

    /**
     * Read a single port's configuration from the device.
     * Returns raw map of form fields (pty, m, d, ecmd, eth, misc, naf, af, hst, etc.).
     */
    Map<String, String> readPortConfig(Device device, String internalRef) {
        return readConfigPage(device, "?pt=${internalRef}")
    }

    /**
     * Read multiple port configurations by list of internal refs.
     * Returns map keyed by internalRef.
     */
    Map<String, Map<String, String>> readPortConfigs(Device device, List<String> internalRefs) {
        Map<String, Map<String, String>> result = [:]
        internalRefs.each { ref ->
            try {
                result[ref] = readPortConfig(device, ref)
            } catch (Exception e) {
                log.error("Failed to read port config for ref={}", ref, e)
            }
        }
        return result
    }

    /**
     * Read firmware version from the cf=1 page header.
     */
    String readFirmwareVersion(Device device) {
        try {
            def page = httpClient(device, "?cf=1").readState()
            // MegaD firmware version appears in the page title (e.g., "MegaD-2561-RTC v3.33c")
            def title = page.title()?.trim()
            if (title) return title.take(250)
            def body = page.body()?.text()
            if (body) {
                def matcher = (body =~ /MegaD[^\s,<]*/)
                if (matcher.find()) return matcher.group(0).take(250)
            }
            return "unknown"
        } catch (Exception e) {
            log.error("Failed to read firmware version", e)
            return "unknown"
        }
    }

    // ==================== Read Port Values ====================

    /**
     * Read all port values from the device via HTTP (?cmd=all).
     * Returns map of portNr -> value string.
     */
    Map<String, String> readPortValues(Device deviceController) throws UnavailableDeviceException {
        Map<String, String> response = [:]
        def allStringStatus = httpClient(deviceController, "?cmd=all").readState()
        allStringStatus.text().split(";").eachWithIndex { String status, int index ->
            response[index.toString()] = status
        }
        return response
    }

    /**
     * Read a single port value from the device.
     */
    def readPortValue(Device deviceController, String portNr) {
        try {
            if (deviceController) {
                return httpClient(deviceController, "?pt=${portNr}&cmd=get").readState()
            }
        } catch (Exception ex) {
            log.error("Read port value failed device=[${deviceController?.code}], portNr=[${portNr}]", ex.message)
        }
    }

    /**
     * @deprecated Use readPortValue(Device, String) instead
     */
    def readPortValue(deviceId, portNr) {
        try {
            Device deviceController = Device.findById(deviceId)
            if (deviceController) {
                return readPortValue(deviceController, "${portNr}")
            }
        } catch (Exception ex) {
            log.error("Read port value failed deviceId=[${deviceId}], portNr=[${portNr}]", ex.message)
        }
    }

    // ==================== Write Operations ====================

    /**
     * Write a single port value. Uses MQTT as primary transport, HTTP as fallback.
     */
    void writePortValue(Device device, DevicePort port, def action) {
        try {
            mqttTopicService.publish(port, buildActionPayload(port, action))
            log.debug("Port value written via MQTT: device={}, port={}, action={}", device.code, port.internalRef, action)
        } catch (Exception mqttEx) {
            log.warn("MQTT write failed for port {}, falling back to HTTP: {}", port.internalRef, mqttEx.message)
            try {
                httpClient(device, "?cmd=${port.internalRef}:${resolveActionValue(action)}").readState()
                log.debug("Port value written via HTTP fallback: device={}, port={}, action={}", device.code, port.internalRef, action)
            } catch (Exception httpEx) {
                log.error("HTTP fallback also failed for port {}", port.internalRef, httpEx)
            }
        }
    }

    /**
     * Write multiple port values at once via HTTP multi-command.
     * portActions is a map of portRef -> action value (e.g., ["0": 1, "1": 0])
     */
    void writePortValues(Device device, Map<String, Object> portActions) {
        def cmdParts = portActions.collect { ref, action -> "${ref}:${resolveActionValue(action)}" }
        def cmdStr = cmdParts.join(";")
        httpClient(device, "?cmd=${cmdStr}").readState()
        log.debug("Multi-port write via HTTP: device={}, cmd={}", device.code, cmdStr)
    }

    /**
     * Write port configuration to the device.
     * Sends config fields as a query string with nr=1 (no restart).
     */
    void writePortConfig(Device device, String internalRef, Map<String, String> config) {
        def fields = new LinkedHashMap<String, String>()
        fields.pn = internalRef
        fields.putAll(config)
        fields.nr = '1' // no restart

        def queryString = buildConfigLine(fields)
        new DeviceHttpService(device: device).sendRawQuery(queryString)
        log.info("Port config written: device={}, port={}", device.code, internalRef)
    }

    // ==================== Backup / Restore ====================

    /**
     * Serialize a full-config map (as returned by readFullConfig) into the
     * temp.cf line-per-section format consumed by initializeFromConfig and
     * stored in DeviceBackup.configuration.
     */
    String serializeFullConfig(Map<String, Object> fullConfig) {
        def lines = []

        // cf=1 — general config (no &nr=1)
        if (fullConfig.general) {
            def fields = new LinkedHashMap<String, String>()
            fields.cf = '1'
            fields.putAll(fullConfig.general as Map)
            lines << buildConfigLine(fields)
        }

        // cf=2 — MQTT config
        if (fullConfig.mqtt) {
            def fields = new LinkedHashMap<String, String>()
            fields.cf = '2'
            fields.putAll(fullConfig.mqtt as Map)
            fields.nr = '1'
            lines << buildConfigLine(fields)
        }

        // cf=7 — cron config
        if (fullConfig.cron) {
            def fields = new LinkedHashMap<String, String>()
            fields.cf = '7'
            fields.putAll(fullConfig.cron as Map)
            fields.nr = '1'
            lines << buildConfigLine(fields)
        }

        // cf=8 — keys config
        if (fullConfig.keys) {
            def fields = new LinkedHashMap<String, String>()
            fields.cf = '8'
            fields.putAll(fullConfig.keys as Map)
            fields.nr = '1'
            lines << buildConfigLine(fields)
        }

        // cf=10 — programs
        fullConfig.programs?.eachWithIndex { Map progConfig, int idx ->
            if (progConfig) {
                def fields = new LinkedHashMap<String, String>()
                fields.cf = '10'
                fields.prn = "${idx}"
                fields.putAll(progConfig)
                fields.nr = '1'
                lines << buildConfigLine(fields)
            }
        }

        // Port configs
        fullConfig.ports?.each { Map portConfig ->
            def fields = new LinkedHashMap<String, String>()
            def portNr = portConfig.remove('_portNr')
            fields.pn = portNr
            fields.putAll(portConfig)
            fields.nr = '1'
            lines << buildConfigLine(fields)
        }

        return lines.join('\n')
    }

    /**
     * Backup the full device configuration into a DeviceBackup record.
     * Config is stored in temp.cf format (one URL query string per line).
     */
    @Transactional
    DeviceBackup backupConfiguration(Device device) {
        def fullConfig = readFullConfig(device)
        def configContent = serializeFullConfig(fullConfig)
        int lineCount = configContent ? configContent.split('\n').length : 0
        def frmVersion = readFirmwareVersion(device)

        def backup = new DeviceBackup(
                device: device,
                configuration: configContent,
                frmVersion: frmVersion
        )
        device.addToBackups(backup)
        device.save(flush: true, failOnError: true)

        log.info("Configuration backed up for device {}: {} lines, firmware={}", device.code, lineCount, frmVersion)
        return backup
    }

    /**
     * Push backup configuration to the physical controller via HTTP.
     * Sends each config line to the device, then optionally restarts it.
     * This does NOT modify the database — only the physical device.
     */
    void restoreToController(Device device, DeviceBackup backup, boolean restartAfter = true) {
        def lines = backup.configuration.split('\n').collect { it.trim() }.findAll { it }
        if (!lines) {
            log.warn("Empty backup configuration for device {}", device.code)
            return
        }

        String currentPassword = device.authAccounts?.first()?.password ?: 'sec'
        def restoreDevice = device

        log.info("Pushing configuration to controller {}: {} lines", device.code, lines.size())

        lines.each { line ->
            try {
                def fields = parseConfigLine(line)
                if (fields.cf == '1' && fields.pwd) {
                    currentPassword = fields.pwd
                }
                if (fields.cf == '1' && fields.eip && fields.eip != restoreDevice.networkAddress?.ip) {
                    log.info("Device IP changing during restore: {} -> {}", restoreDevice.networkAddress?.ip, fields.eip)
                }

                new DeviceHttpService(device: restoreDevice).sendRawQuery(line)
                Thread.sleep(100) // EEPROM write delay
            } catch (Exception e) {
                log.error("Failed to push config line: {}", line, e)
            }
        }

        if (restartAfter) {
            restartDevice(device)
        }

        log.info("Push to controller completed for device {}", device.code)
    }

    /**
     * Sync the database Device/DevicePort model from a backup's configuration.
     * Parses the backup config, updates device network settings, and
     * creates/updates/removes DevicePorts to match the backup.
     * This does NOT touch the physical controller — only the database.
     */
    @Transactional
    void syncFromBackup(Device device, DeviceBackup backup) {
        def lines = backup.configuration.split('\n').collect { it.trim() }.findAll { it }
        if (!lines) {
            log.warn("Empty backup configuration for device {}", device.code)
            return
        }

        Map<String, String> generalConfig = [:]
        Map<String, String> mqttConfig = [:]
        List<Map<String, String>> portConfigs = []

        lines.each { line ->
            Map<String, String> fields = parseConfigLine(line)
            if (fields.cf == '1') {
                generalConfig = fields
            } else if (fields.cf == '2') {
                mqttConfig = fields
            } else if (fields.pn != null) {
                portConfigs << fields
            }
        }

        // Update device network address from general config
        if (generalConfig.eip) {
            if (!device.networkAddress) {
                device.networkAddress = new NetworkAddress()
            }
            device.networkAddress.ip = generalConfig.eip
            device.networkAddress.port = device.networkAddress.port ?: '80'
        }
        if (generalConfig.gw) {
            device.networkAddress.gateway = generalConfig.gw
        }

        // Update device password from general config
        if (generalConfig.pwd && device.authAccounts) {
            def defaultAccount = device.authAccounts.first()
            if (defaultAccount) {
                defaultAccount.password = generalConfig.pwd
            }
        }

        // Build a map of existing ports by internalRef for efficient lookup
        Map<String, DevicePort> existingPorts = [:]
        device.ports?.each { DevicePort port ->
            existingPorts[port.internalRef] = port
        }

        Set<String> seenRefs = [] as Set

        portConfigs.each { Map<String, String> fields ->
            String portNr = fields.pn
            String ptyValue = fields.pty
            PortType portType = PortType.fromValue(ptyValue) ?: PortType.NOT_CONFIGURED

            if (portType == PortType.NOT_CONFIGURED) {
                return // skip unconfigured ports
            }

            seenRefs << portNr

            DevicePort existingPort = existingPorts[portNr]
            if (existingPort) {
                // Update existing port
                existingPort.type = portType
                updatePortConfigurations(existingPort, fields)
            } else {
                // Create new port
                def port = new DevicePort(
                        internalRef: portNr,
                        name: "P${portNr}",
                        type: portType,
                        description: '',
                        device: device
                )
                updatePortConfigurations(port, fields)
                device.addToPorts(port)
            }
        }

        device.save(flush: true, failOnError: true)

        int updated = seenRefs.intersect(existingPorts.keySet()).size()
        int created = seenRefs.size() - updated
        log.info("Sync from backup completed for device {}: {} ports updated, {} ports created",
                device.code, updated, created)
    }

    /**
     * Update Configuration entries on a port from raw config fields.
     */
    private void updatePortConfigurations(DevicePort port, Map fields) {
        def configKeys = [
                'ecmd': 'cfg.key.port.action',
                'af'  : 'cfg.key.port.actionFlag',
                'eth' : 'cfg.key.port.httpAction',
                'naf' : 'cfg.key.port.netActionFlag',
                'misc': 'cfg.key.port.miscValue',
                'd'   : 'cfg.key.port.outputState',
                'mt'  : 'cfg.key.port.muteTimer',
                'm'   : 'cfg.key.port.mode',
                'hst' : 'cfg.key.port.HystDeviationValue',
                'grp' : 'cfg.key.port.group'
        ]

        configKeys.each { rawKey, cfgKey ->
            if (fields.containsKey(rawKey)) {
                def existing = port.configurations?.find { it.key == cfgKey }
                if (existing) {
                    existing.value = fields[rawKey] ?: ''
                } else {
                    port.configurations << new Configuration(
                            entityType: EntityType.PORT,
                            key: cfgKey,
                            value: fields[rawKey] ?: ''
                    )
                }
            }
        }
    }

    // ==================== Device Management ====================

    /**
     * Restart the device via HTTP.
     */
    void restartDevice(Device device) {
        try {
            new DeviceHttpService(device: device).sendRawQuery("cmd=rst")
            log.info("Device restart sent: {}", device.code)
        } catch (Exception e) {
            log.error("Failed to restart device {}", device.code, e)
        }
    }

    /**
     * Change device IP address via UDP protocol.
     * Updates the device domain object on success.
     */
    @Transactional
    boolean changeDeviceIp(Device device, String newIp) {
        String password = device.authAccounts?.first()?.password ?: 'sec'
        String oldIp = device.networkAddress?.ip

        boolean success = MegaDUdpTransport.changeIp(oldIp, newIp, password)
        if (success) {
            device.networkAddress.ip = newIp
            device.save(flush: true)
            log.info("Device {} IP changed: {} -> {}", device.code, oldIp, newIp)
        }
        return success
    }

    // ==================== Legacy / Backward Compatible ====================

    /**
     * Read device configuration from a live controller and populate Device ports.
     * This is the legacy method kept for backward compatibility with DeviceService.importDevice().
     */
    def readConfig(String deviceCode) {
        // Stub for string-based lookup — not currently used with meaningful implementation
        return null
    }

    /**
     * Read device configuration from a live controller and populate Device ports.
     * Refactored to use readPortConfig internally.
     */
    @Transactional
    def readConfig(Device deviceController) {
        def deviceMainPage = httpClient(deviceController).readState()

        // Process XP (expansion) port links
        deviceMainPage.select("a").findAll { it.text().matches("XP[0-9]+") }.each { link ->
            def portListPage = httpClient(deviceController, link.attr('href')).readState()
            portListPage.select("a").findAll {
                it.text().matches("P[0-9]{1,2}.+")
            }.each { portLink ->
                def portPage = httpClient(deviceController, portLink.attr('href')).readState()
                Matcher m = (portLink.text() =~ /P[0-9]{1,2}.+\)|P[0-9]{1,2}/)
                m.find()
                def portName = m.group(0)
                def portNr = portPage.select("form input").first().attr("value")
                deviceController.addToPorts(readPortConfigFromController(deviceController.code, portNr, portName))
            }
        }

        // Process direct port links on main page
        deviceMainPage.select("a").findAll { it.text().matches("P[0-9]{1,2}.+") }.each { portLink ->
            def portPage = httpClient(deviceController, portLink.attr('href')).readState()
            Matcher m = (portLink.text() =~ /P[0-9]{1,2}.+\)|P[0-9]{1,2}/)
            m.find()
            def portName = m.group(0)
            def portNr = portPage.select("form input").first().attr("value")
            deviceController.addToPorts(readPortConfigFromController(deviceController.code, portNr, portName))
        }

        deviceController.save(failOnError: true)
    }

    /**
     * Read a single port config from controller and return a DevicePort.
     * Kept for backward compatibility with DeviceService.importPort().
     */
    def readPortConfigFromController(code, internalRef, portName) {
        DevicePort port = null
        try {
            Device deviceController = Device.findByCode(code)
            def fields = readPortConfig(deviceController, "${internalRef}")

            PortType portType = PortType.fromValue(fields.pty)

            port = new DevicePort(
                    internalRef: internalRef,
                    name: portName,
                    type: portType,
                    description: "",
                    device: deviceController
            )

            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.portType", value: fields.pty ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.miscValue", value: fields.misc ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.HystDeviationValue", value: fields.hst ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.action", value: fields.ecmd ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.mode", value: fields.m ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.outputState", value: fields.d ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.analogMode", value: fields.m ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.sensorMode", value: fields.m ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.sensorModel", value: fields.d ?: '')
            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.i2cMode", value: fields.m ?: '')

        } catch (Exception ex) {
            log.error("Read port from controller", ex.message)
        }
        return port
    }

    // ==================== Event Subscribers ====================

    @Subscriber('2561.run.action')
    def runAction(event) {
        log.debug("call action")
        Long deviceId = event?.data?.deviceId
        if (deviceId) {
            Device device = Device.findById(deviceId)
            if (device) {
                httpClient(device, '?cmd=' + event?.data?.actionBody).readState()
            }
        }
    }

    @Subscriber('2561.run.scenario')
    def runScenario(event) {
        dslService.execute(event?.data?.scenarioBody, 'EVENT')
    }

    // ==================== Private Helpers ====================

    /**
     * Generic form field extractor from a Jsoup HTML page.
     * Extracts all input, select, and textarea fields from forms.
     */
    private Map<String, String> extractFormFields(Document page) {
        def fields = new LinkedHashMap<String, String>()

        page.select("form input").each { Element input ->
            String name = input.attr("name")
            if (!name) return

            if (input.attr("type") == "checkbox") {
                fields[name] = input.hasAttr("checked") ? "1" : ""
            } else {
                fields[name] = input.attr("value") ?: ""
            }
        }

        page.select("form select").each { Element select ->
            String name = select.attr("name")
            if (!name) return

            def selectedOption = select.select("option[selected]").first()
            fields[name] = selectedOption?.attr("value") ?: ""
        }

        page.select("form textarea").each { Element textarea ->
            String name = textarea.attr("name")
            if (!name) return
            fields[name] = textarea.text() ?: ""
        }

        return fields
    }

    /**
     * Parse a temp.cf config line into a map of key-value pairs.
     * Handles values containing '=' (e.g., eth=192.168.1.50/sec/?cmd=1:2).
     */
    private Map<String, String> parseConfigLine(String line) {
        def fields = new LinkedHashMap<String, String>()
        line.split("&").each { segment ->
            def eqIdx = segment.indexOf('=')
            if (eqIdx > 0) {
                fields[segment.substring(0, eqIdx)] = segment.substring(eqIdx + 1)
            } else if (segment) {
                fields[segment] = ''
            }
        }
        return fields
    }

    /**
     * Build a config line from a map of key-value pairs.
     */
    private String buildConfigLine(Map<String, String> fields) {
        return fields.collect { k, v -> "${k}=${v ?: ''}" }.join("&")
    }

    /** Short timeout for optional firmware features (cron / keys / programs). */
    private static final int OPTIONAL_PAGE_TIMEOUT_MS = 1500

    /**
     * Shorthand factory for DeviceHttpService.
     */
    private DeviceHttpService httpClient(Device device, String uri = null, Integer timeoutMs = null) {
        return new DeviceHttpService(device: device, uri: uri, timeoutMs: timeoutMs)
    }

    /**
     * Read a config page and extract form fields.
     *
     * @param optional when true, use a short timeout and demote failures to debug
     *                 — pages cf=7/cf=8/cf=10 aren't compiled into every firmware
     *                 and the device just hangs the request when they're absent.
     */
    private Map<String, String> readConfigPage(Device device, String uri, boolean optional = false) {
        try {
            def page = httpClient(device, uri, optional ? OPTIONAL_PAGE_TIMEOUT_MS : null).readState()
            return extractFormFields(page)
        } catch (Exception e) {
            if (optional) {
                log.debug("Optional config page {} not available on device {}: {}", uri, device.code, e.message)
            } else {
                log.error("Failed to read config page {}: {}", uri, e.message)
            }
            return [:]
        }
    }

    /**
     * Detect the number of ports on the device from the main page.
     */
    private int detectPortCount(Device device) {
        try {
            def mainPage = httpClient(device).readState()
            // Count all port links on main page and XP pages
            int maxPort = -1
            def portPattern = ~/\?pt=(\d+)/

            // Check main page links
            mainPage.select("a").each { link ->
                def href = link.attr("href")
                def matcher = portPattern.matcher(href)
                if (matcher.find()) {
                    int pn = Integer.parseInt(matcher.group(1))
                    if (pn > maxPort) maxPort = pn
                }
            }

            // Check XP pages
            mainPage.select("a").findAll { it.text().matches("XP[0-9]+") }.each { xpLink ->
                try {
                    def xpPage = httpClient(device, xpLink.attr('href')).readState()
                    xpPage.select("a").each { link ->
                        def href = link.attr("href")
                        def matcher = portPattern.matcher(href)
                        if (matcher.find()) {
                            int pn = Integer.parseInt(matcher.group(1))
                            if (pn > maxPort) maxPort = pn
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not read XP page: {}", e.message)
                }
            }

            // Default to 37 ports for MEGAD_2561_RTC if detection fails
            return maxPort >= 0 ? maxPort + 1 : 37
        } catch (Exception e) {
            log.warn("Port count detection failed, defaulting to 37: {}", e.message)
            return 37
        }
    }

    /**
     * Build MQTT action payload for a port write.
     */
    private String buildActionPayload(DevicePort port, def action) {
        return "${port.internalRef}:${resolveActionValue(action)}"
    }

    /**
     * Resolve action to its numeric value for command strings.
     */
    private String resolveActionValue(def action) {
        if (action instanceof PortAction) {
            return "${action.value}"
        }
        return "${action}"
    }
}
