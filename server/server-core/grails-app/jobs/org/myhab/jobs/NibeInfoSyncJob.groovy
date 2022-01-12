package org.myhab.jobs


import org.myhab.domain.device.Device
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
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
            def accConfig = device.getConfigurationByKey('cfg.key.device.oauth.access_token')
            if (accConfig && accConfig.value) {
                def bearerToken = accConfig.value

                HttpResponse<String> pumpResponse = Unirest.get("$API_URL/systems/$DEVICE_REF_ID/serviceinfo/categories/STATUS")
                        .header("Authorization", "Bearer ${bearerToken}")
                        .asString();

                if (pumpResponse.status == 200) {
                    def parameters = new JsonSlurper().parseText(pumpResponse.getBody())
                    def waterTemp = (parameters.find { it['parameterId'] == 40014 }['rawValue'] / 10)


                    mqttTopicService.publish(device.ports.find { it.internalRef == 't1' }, "${waterTemp}")

                    if (device.status.equals(DeviceStatus.OFFLINE)) {
                        mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
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
}
