package org.myhab.jobs

import grails.events.EventPublisher
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.parser.ValueParser
import org.myhab.services.DeviceService
import org.myhab.services.PortValueService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit
import grails.gorm.transactions.Transactional

@Slf4j
@DisallowConcurrentExecution
@Transactional
class DeviceControllerStateSyncJob implements Job, EventPublisher {
    // Typed on purpose: the Quartz job factory autowires BY TYPE, so a
    // def/Object-typed property is silently skipped and stays null.
    DeviceService deviceService
    PortValueService portValueService
    // DISABLED: Grails auto-scheduling conflicts with SchedulerService
    // Jobs are now managed via SchedulerService and database-backed triggers
    /*
    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.deviceControllerStateSync.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.deviceControllerStateSync.interval', Integer) ?: 60
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "DeviceControllerStateSyncJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "DeviceControllerStateSyncJob: DISABLED - Not registering trigger"
        }
    }
    */

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.deviceControllerStateSync.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("DeviceControllerStateSyncJob is DISABLED via configuration, skipping execution")
            return
        }
        Device.withCriteria {
            not {
                eq('status', DeviceStatus.DISABLED)
            }
        }.each { deviceController ->
            def httpSyncSupported = deviceController.getConfigurationByKey(CfgKey.DEVICE.DEVICE_HTTP_SYNC_SUPPORTED)
            // MegaD controllers default to HTTP sync (their ?cmd=all is the
            // ground truth for port state); an explicit 'false' row opts out.
            // Other models keep the opt-in behavior.
            boolean syncEnabled = httpSyncSupported != null ?
                    Boolean.valueOf(httpSyncSupported.value) :
                    deviceController.model == DeviceModel.MEGAD_2561_RTC
            if (syncEnabled) {
                try {
                    def portValues = deviceService.readPortValuesFromDevice(deviceController)
                    portValues.each { portVal ->
                        // Per-port isolation: ?cmd=all reports a value for EVERY port
                        // index, most of which have no DevicePort row — one bad entry
                        // must not abort the rest of the device's sync.
                        try {
                            def port = DevicePort.where {
                                internalRef == portVal.key
                                device.id == deviceController.id
                            }.find()

                            if (port != null) {
                                def rtPortVal = ValueParser.parser(port).apply(portVal.value)
                                if (port.value != rtPortVal) {
                                    def eventData = new EventData().with {
                                        p0 = TopicName.EVT_ASYNC_PORT_VALUE_CHANGED.id()
                                        p1 = "${deviceController.model}"
                                        p2 = "${deviceController.code}" // device internal code
                                        p3 = "${port.type}" // port type (optional)
                                        p4 = "${port.internalRef}" // port internal code
                                        p5 = "${rtPortVal}" // port value
                                        p6 = "device http sync job"
                                        it
                                    }
                                    publish(TopicName.EVT_ASYNC_PORT_VALUE_CHANGED.id(), eventData)
                                }
                            }
                        } catch (Exception portEx) {
                            log.error("Port state sync failed: device=${deviceController.code}, portRef=${portVal.key}: ${portEx.message}", portEx)
                        }
                    }
                } catch (UnavailableDeviceException unavailable) {
                    // Device unreachable is a recurring, expected condition; the
                    // OFFLINE status event was already published downstream.
                    log.warn("HTTP state sync skipped, device=${deviceController.code} unreachable: ${unavailable.message}")
                } catch (Exception failedToSync) {
                    log.error("HTTP state sync failed for device=${deviceController.code}: ${failedToSync.message}", failedToSync)
                }
            }
        }

    }
}
