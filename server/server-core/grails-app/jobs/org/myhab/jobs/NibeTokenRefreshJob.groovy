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

/**
 * @deprecated This job is deprecated and will be removed in a future version.
 * Token refresh functionality has been integrated into NibeInfoSyncJob.
 * 
 * To disable this job, set in application.yml:
 *   quartz.jobs.nibeTokenRefresh.enabled: false
 */
@Deprecated
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
            enabled = false  // Default to disabled (functionality moved to NibeInfoSyncJob)
        }
        
        if (enabled) {
            log.warn "NibeTokenRefreshJob: ENABLED - This job is DEPRECATED. Token refresh is now handled by NibeInfoSyncJob."
            log.warn "NibeTokenRefreshJob: Please set quartz.jobs.nibeTokenRefresh.enabled: false in application.yml"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "NibeTokenRefreshJob: DISABLED - Token refresh is handled by NibeInfoSyncJob"
        }
    }


    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeTokenRefresh.enabled', Boolean)
        
        if (enabled == null) {
            enabled = false
        }
        
        if (!enabled) {
            log.info("NibeTokenRefreshJob is DISABLED via configuration (token refresh handled by NibeInfoSyncJob)")
            return
        }
        
        log.warn("NibeTokenRefreshJob is DEPRECATED - token refresh is now integrated into NibeInfoSyncJob")
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
                // TODO: Add Telegram notification when sendMessage method is implemented
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
        HttpResponse<String> response = Unirest.post("https://api.myuplink.com/oauth/token")
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
        HttpResponse<String> response = Unirest.post("https://api.myuplink.com/oauth/token")
                .header("content-type", "application/x-www-form-urlencoded")
                .field("grant_type", "refresh_token")
                .field("client_id", device.getConfigurationByKey('cfg.key.device.oauth.client_id').value)
                .field("client_secret", device.getConfigurationByKey('cfg.key.device.oauth.client_secret').value)
                .field("refresh_token", "${refreshToken}")
                .asString();

        return new JsonSlurper().parseText(response.getBody())
    }
}