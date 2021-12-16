package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.async.mqtt.MqttTopicService
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@DisallowConcurrentExecution
class NibeInfoSyncJob implements Job {
    public static final String API_URL = "https://api.nibeuplink.com/api/v1"
    public static final String DEVICE_REF_ID = "78047"
    MqttTopicService mqttTopicService


    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
        Unirest.config()
                .socketTimeout(2000)
                .connectTimeout(2000)
                .setDefaultHeader("Accept", "application/json")
                .followRedirects(true)
                .enableCookieManagement(false)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def device = Device.findByModel(DeviceModel.NIBE_F1145_8_EM)
        def accConfig = device.getConfigurationByKey('cfg.key.device.oauth.access_token')
        if (accConfig && accConfig.value) {
            def bearerToken = accConfig.value
            HttpResponse<String> response = Unirest.get("$API_URL/systems/$DEVICE_REF_ID/status/system")
                    .header("Authorization", "Bearer ${bearerToken}")
                    .asString();

            def statSystem = new JsonSlurper().parseText(response.getBody())
            def heaterReverseTemp = (statSystem[3]['parameters'][1]['rawValue'] / 10)

            HttpResponse<String> resp2 = Unirest.get("$API_URL/systems/$DEVICE_REF_ID/serviceinfo/categories/STATUS")
                    .header("Authorization", "Bearer ${bearerToken}")
                    .asString();
            def parameters = new JsonSlurper().parseText(resp2.getBody())
            def waterTemp = (parameters.find { it['parameterId'] == 40014 }['rawValue'] / 10)


            mqttTopicService.publish(device.ports.find { it.internalRef == 't1' }, "${waterTemp}")
        } else {
            log.warn("Can't synca data - there are no access tokens configured for device ${device.id}")
        }
    }
}
