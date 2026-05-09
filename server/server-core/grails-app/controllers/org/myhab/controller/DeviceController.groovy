package org.myhab.controller

import grails.converters.JSON
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceAccount
import org.myhab.domain.device.DeviceBackup
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.NetworkAddress
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.services.MegaDriverService

/**
 * REST controller for MegaD device discovery, init-from-controller and
 * configuration backup/restore. Backed by {@link MegaDriverService}.
 */
@Slf4j
@Secured(['ROLE_ADMIN'])
class DeviceController {

    MegaDriverService megaDriverService

    static responseFormats = ['json']
    static allowedMethods = [
            discover           : 'GET',
            initFromDevice     : 'POST',
            listBackups        : 'GET',
            readFullConfig     : 'GET',
            backupConfig       : 'POST',
            restoreToController: 'POST',
            syncFromBackup     : 'POST'
    ]

    /**
     * GET /api/devices/discover
     * UDP-broadcast scan, optionally enriched with each device's MQTT id.
     */
    def discover() {
        try {
            def devices = megaDriverService.discoverDevices(true)
            render([devices: devices] as JSON)
        } catch (Exception e) {
            log.error("Device discovery failed", e)
            response.status = 500
            render([error: "Discovery failed: ${e.message}"] as JSON)
        }
    }

    /**
     * POST /api/devices/init-from-device
     * Body: {ip, password}
     * Reads full config from a live MegaD controller and creates a Device row.
     */
    @Transactional
    def initFromDevice() {
        def body = request.JSON
        String ip = body?.ip
        String password = body?.password ?: 'sec'

        if (!ip) {
            response.status = 400
            render([error: 'ip is required'] as JSON)
            return
        }

        try {
            Device temp = new Device(
                    code: "tmp-${ip}",
                    name: "tmp-${ip}",
                    model: DeviceModel.MEGAD_2561_RTC,
                    status: DeviceStatus.OFFLINE,
                    networkAddress: new NetworkAddress(ip: ip, port: '80')
            )
            temp.authAccounts = [new DeviceAccount(password: password, isDefault: true, device: temp)] as Set

            def fullConfig = megaDriverService.readFullConfig(temp)
            String configContent = megaDriverService.serializeFullConfig(fullConfig)
            if (!configContent) {
                response.status = 502
                render([error: "Could not read configuration from device at ${ip}"] as JSON)
                return
            }

            Device device = megaDriverService.initializeFromConfig(configContent)

            // Code derives from the controller's mdid (cf=2). If a Device row already
            // exists with that code, refuse with 409 instead of hitting the DB unique
            // constraint and surfacing a Hibernate stack trace to the UI.
            Device existing = Device.findByCode(device.code)
            if (existing) {
                response.status = 409
                render([
                        error            : "A device with code '${device.code}' already exists",
                        existingDeviceId : existing.id,
                        existingDeviceCode: existing.code
                ] as JSON)
                return
            }

            // Use the password supplied by the user (may differ from cf=1 pwd field)
            if (device.authAccounts) {
                device.authAccounts.first().password = password
            }
            device.save(flush: true, failOnError: true)

            render([
                    deviceId  : device.id,
                    deviceCode: device.code,
                    portCount : device.ports?.size() ?: 0
            ] as JSON)
        } catch (UnavailableDeviceException e) {
            log.warn("initFromDevice unreachable: ip={} error={}", ip, e.message)
            response.status = 502
            render([error: "Device at ${ip} is unreachable: ${e.message}"] as JSON)
        } catch (Exception e) {
            log.error("initFromDevice failed for ip={}", ip, e)
            response.status = 500
            render([error: "Init failed: ${e.message}"] as JSON)
        }
    }

    /**
     * GET /api/devices/{id}/backups
     */
    def listBackups() {
        Long id = params.id as Long
        Device device = Device.get(id)
        if (!device) {
            response.status = 404
            render([error: "Device not found: ${id}"] as JSON)
            return
        }

        def backups = (device.backups ?: [])
                .sort { a, b -> (b?.tsCreated ?: 0) <=> (a?.tsCreated ?: 0) }
                .collect { DeviceBackup b ->
                    [
                            id         : b.id,
                            frmVersion : b.frmVersion,
                            configLines: b.configuration ? b.configuration.split('\n').length : 0,
                            tsCreated  : b.tsCreated
                    ]
                }
        render([backups: backups] as JSON)
    }

    /**
     * GET /api/devices/{id}/config
     */
    def readFullConfig() {
        Long id = params.id as Long
        Device device = Device.get(id)
        if (!device) {
            response.status = 404
            render([error: "Device not found: ${id}"] as JSON)
            return
        }
        try {
            render(megaDriverService.readFullConfig(device) as JSON)
        } catch (UnavailableDeviceException e) {
            response.status = 502
            render([error: "Device unreachable: ${e.message}"] as JSON)
        } catch (Exception e) {
            log.error("readFullConfig failed for device id={}", id, e)
            response.status = 500
            render([error: "Read config failed: ${e.message}"] as JSON)
        }
    }

    /**
     * POST /api/devices/{id}/backup
     */
    def backupConfig() {
        Long id = params.id as Long
        Device device = Device.get(id)
        if (!device) {
            response.status = 404
            render([error: "Device not found: ${id}"] as JSON)
            return
        }

        try {
            DeviceBackup backup = megaDriverService.backupConfiguration(device)
            int lines = backup.configuration ? backup.configuration.split('\n').length : 0
            render([
                    id         : backup.id,
                    frmVersion : backup.frmVersion,
                    configLines: lines
            ] as JSON)
        } catch (UnavailableDeviceException e) {
            response.status = 502
            render([error: "Device unreachable: ${e.message}"] as JSON)
        } catch (Exception e) {
            log.error("backupConfig failed for device id={}", id, e)
            response.status = 500
            render([error: "Backup failed: ${e.message}"] as JSON)
        }
    }

    /**
     * POST /api/devices/{id}/push-to-controller/{backupId}
     */
    def restoreToController() {
        Long id = params.id as Long
        Long backupId = params.backupId as Long
        Device device = Device.get(id)
        DeviceBackup backup = backupId ? DeviceBackup.get(backupId) : null

        if (!device) {
            response.status = 404
            render([error: "Device not found: ${id}"] as JSON)
            return
        }
        if (!backup || backup.device?.id != device.id) {
            response.status = 404
            render([error: "Backup not found for device ${id}: ${backupId}"] as JSON)
            return
        }

        try {
            megaDriverService.restoreToController(device, backup, true)
            render([ok: true] as JSON)
        } catch (Exception e) {
            log.error("restoreToController failed device={} backup={}", id, backupId, e)
            response.status = 500
            render([error: "Push to controller failed: ${e.message}"] as JSON)
        }
    }

    /**
     * POST /api/devices/{id}/sync-from-backup/{backupId}
     */
    def syncFromBackup() {
        Long id = params.id as Long
        Long backupId = params.backupId as Long
        Device device = Device.get(id)
        DeviceBackup backup = backupId ? DeviceBackup.get(backupId) : null

        if (!device) {
            response.status = 404
            render([error: "Device not found: ${id}"] as JSON)
            return
        }
        if (!backup || backup.device?.id != device.id) {
            response.status = 404
            render([error: "Backup not found for device ${id}: ${backupId}"] as JSON)
            return
        }

        try {
            megaDriverService.syncFromBackup(device, backup)
            render([ok: true] as JSON)
        } catch (Exception e) {
            log.error("syncFromBackup failed device={} backup={}", id, backupId, e)
            response.status = 500
            render([error: "Sync from backup failed: ${e.message}"] as JSON)
        }
    }
}
