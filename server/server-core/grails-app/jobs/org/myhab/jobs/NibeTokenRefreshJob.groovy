package org.myhab.jobs

import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.quartz.*

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Transactional
class NibeTokenRefreshJob implements Job {

    MqttTopicService mqttTopicService

    def telegramBotHandler
    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(500)
    }


    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def device = Device.findByModel(DeviceModel.NIBE_F1145_8_EM)
        try {
            Configuration refreshKeyFromCfg = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_REFRESH_TOKEN) ?: new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: 'cfg.key.device.oauth.refresh_token')
            Configuration accKeyFromCfg = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN) ?: new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: 'cfg.key.device.oauth.access_token')
            if (refreshKeyFromCfg.value) {
                def tk = regenerateTokens(device, refreshKeyFromCfg.value)
                refreshKeyFromCfg.value = tk['refresh_token']
                refreshKeyFromCfg.save()
                accKeyFromCfg.value = tk['access_token']
                accKeyFromCfg.save()
//                log.debug("Acc token : ${tk['access_token']}")
            } else {
                log.warn("There are no tokens configured for device ${device.id}")
                telegramBotHandler.sendMessage('WARN', "There are no tokens configured for device ${device.id}")
            }
            if (device.status == DeviceStatus.OFFLINE) {
                mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            }
        } catch (Exception se) {
            log.warn("Can't connect : ${se.message}")
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    def getAuthTokens(device, authzCode) {
        HttpResponse<String> response = Unirest.post("https://api.nibeuplink.com/oauth/token")
                .header("content-type", "application/x-www-form-urlencoded")
                .field("grant_type", "authorization_code")
                .field("client_id", device.getConfigurationByKey('cfg.key.device.oauth.client_id').value)
                .field("client_secret", device.getConfigurationByKey('cfg.key.device.oauth.client_secret').value)
                .field("code", device.getConfigurationByKey('cfg.key.device.oauth.authz_code').value)
                .field("redirect_uri", device.getConfigurationByKey('cfg.key.device.oauth.redirect_uri').value)
                .asString();
        return new JsonSlurper().parseText(response.getBody())
    }

    def regenerateTokens(device, refreshToken) {
        HttpResponse<String> response = Unirest.post("https://api.nibeuplink.com/oauth/token")
                .header("content-type", "application/x-www-form-urlencoded")
                .field("grant_type", "refresh_token")
                .field("client_id", device.getConfigurationByKey('cfg.key.device.oauth.client_id').value)
                .field("client_secret", device.getConfigurationByKey('cfg.key.device.oauth.client_secret').value)
                .field("refresh_token", "${refreshToken}")
                .asString();

        return new JsonSlurper().parseText(response.getBody())
    }
}
