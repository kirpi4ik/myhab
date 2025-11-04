package org.myhab.jobs

import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.telegram.TelegramBotHandler
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
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeTokenRefresh.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.nibeTokenRefresh.interval', Integer) ?: 500
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "NibeTokenRefreshJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "NibeTokenRefreshJob: DISABLED - Not registering trigger"
        }
    }


    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeTokenRefresh.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("NibeTokenRefreshJob is DISABLED via configuration, skipping execution")
            return
        }
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
                telegramBotHandler.sendMessage(TelegramBotHandler.MSG_LEVEL.WARNING, "There are no tokens configured for device ${device.id}")
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