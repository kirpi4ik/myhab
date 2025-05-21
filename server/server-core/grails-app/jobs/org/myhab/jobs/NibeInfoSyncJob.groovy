package org.myhab.jobs

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@DisallowConcurrentExecution
@Transactional
class NibeInfoSyncJob implements Job {
    public static final String API_URL = "https://api.nibeuplink.com/api/v1"
    public static final String DEVICE_REF_ID = "78047"
    MqttTopicService mqttTopicService


    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def device = Device.findByModel(DeviceModel.NIBE_F1145_8_EM)
        try {
            def accConfig = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN)
            if (accConfig && accConfig.value) {
                def pumpResponse = Unirest.get("$API_URL/systems/$DEVICE_REF_ID/serviceinfo/categories").header("Authorization", "Bearer ${accConfig.value}").asString();
                if (pumpResponse.status == 200) {
                    new JsonSlurper().parseText(pumpResponse.getBody()).each { category ->
                        readParameters(device, accConfig.value, category['categoryId'])
                    }
                } else {
                    log.warn("Can't synca data - response status ${pumpResponse.status}")
                    mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                }
            } else {
                log.warn("Can't synca data - there are no access tokens configured for device ${device.id}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
            }
        } catch (Exception se) {
            log.warn("Can't connect : ${se.message}")
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    void readParameters(device, bearerToken, category) {
        def pumpResponse = Unirest.get("$API_URL/systems/$DEVICE_REF_ID/serviceinfo/categories/${category}").header("Authorization", "Bearer ${bearerToken}").asString();

        if (pumpResponse.status == 200) {
            def parameters = new JsonSlurper().parseText(pumpResponse.getBody())
            parameters.each { parameter ->
                if (parameter['name']) {
                    def port = device.ports.find { it.internalRef == parameter['name'] } as DevicePort
                    if (port) {
                        mqttTopicService.publish(port, "${parameter['rawValue']}")
                    } else {
                        // log.debug("Unknow port internal ref=[${parameter['name']}] for deviceId[${device.id}], mqtt publish event will be skipped")
                    }
                }
            }
            if (device.status == DeviceStatus.OFFLINE) {
                mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            }
        } else {
            log.warn("Can't synca data - response status ${pumpResponse.status}")
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }
}