package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.quartz.*

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Transactional
class NibeTokenRefreshJob implements Job {

    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(500)
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
        Configuration refreshKeyFromCfg = device.getConfigurationByKey('cfg.key.device.oauth.refresh_token') ?: new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: 'cfg.key.device.oauth.refresh_token')
        Configuration accKeyFromCfg = device.getConfigurationByKey('cfg.key.device.oauth.access_token') ?: new Configuration(entityId: device.id, entityType: EntityType.DEVICE, key: 'cfg.key.device.oauth.access_token')
        if (refreshKeyFromCfg.value) {
            def tk = regenerateTokens(device, refreshKeyFromCfg.value)
            refreshKeyFromCfg.value = tk['refresh_token']
            refreshKeyFromCfg.save()
            accKeyFromCfg.value = tk['access_token']
            accKeyFromCfg.save()
            log.debug("Acc token : ${tk['access_token']}")
        } else {
            log.warn("There are no tokens configured for device ${device.id}")
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
