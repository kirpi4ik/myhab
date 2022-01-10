package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.async.mqtt.MqttTopicService
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.DeviceStatus
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse
import kong.unirest.Unirest
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
                } else {
                    log.warn("Can't synca data - response status ${pumpResponse.status}")
                    device.setStatus(DeviceStatus.OFFLINE)
                    device.save()
                }
            } else {
                log.warn("Can't synca data - there are no access tokens configured for device ${device.id}")
                device.setStatus(DeviceStatus.OFFLINE)
                device.save()
            }
        } catch (Exception se) {
            log.warn("Can't connect : ${se.message}")
            device.setStatus(DeviceStatus.OFFLINE)
            device.save()
        }
    }
}
