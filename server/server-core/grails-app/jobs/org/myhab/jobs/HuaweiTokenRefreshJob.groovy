package org.myhab.jobs

import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@DisallowConcurrentExecution
class HuaweiTokenRefreshJob implements Job {
    public static final String API_URL = "https://eu5.fusionsolar.huawei.com/thirdData/login"
    public static final String TOKEN_HEADER = "xsrf-token"
    MqttTopicService mqttTopicService

    static triggers = {
        //The validity period of XSRF-TOKEN is 30 minutes
        simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def device = Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2)
        try {
            def user = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_USER)
            def passwd = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_PASSWD)
            if (user && user.value && passwd && passwd.value) {
                HttpResponse<JsonNode> response = Unirest.post(API_URL)
                        .header("content-type", "application/json")
                        .field("userName", user.value)
                        .field("systemCode", passwd.value)
                        .asJson();
                if (response.status == 200) {
                    Configuration accKeyFromCfg = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN) ?: new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key())

                    def tokens = response.getHeaders().get(TOKEN_HEADER)
                    if (tokens != null && !tokens.isEmpty()) {
                        accKeyFromCfg.setValue(tokens.first())
                        accKeyFromCfg.save()
                    }
                }
            }
        } catch (Exception se) {
            log.warn("Can't connect : ${se.message}")
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }
}
