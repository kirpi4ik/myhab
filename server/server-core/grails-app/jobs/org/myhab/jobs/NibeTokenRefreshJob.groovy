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
 * Nibe myUplink OAuth2 Token Refresh Job
 * 
 * Refreshes OAuth2 tokens every 5 minutes to prevent expiration.
 * The myUplink API requires frequent token refresh as tokens expire quickly.
 * 
 * This job runs independently of NibeInfoSyncJob to ensure tokens are always fresh.
 * NibeInfoSyncJob reads the refreshed tokens from device configuration.
 * 
 * Token Flow:
 * 1. Uses refresh_token to get new access_token and refresh_token
 * 2. Saves both tokens to device configuration
 * 3. NibeInfoSyncJob uses these tokens for API calls
 * 
 * Configuration:
 *   quartz.jobs.nibeTokenRefresh.enabled: true
 *   quartz.jobs.nibeTokenRefresh.interval: 300  # 5 minutes
 */
@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Transactional
class NibeTokenRefreshJob implements Job {

    MqttTopicService mqttTopicService
    TelegramBotHandler telegramBotHandler
    
    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeTokenRefresh.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.nibeTokenRefresh.interval', Integer) ?: 300  // Default 5 minutes
        
        if (enabled == null) {
            log.debug "NibeTokenRefreshJob: Configuration not found, defaulting to ENABLED"
            enabled = true  // Default to enabled for token refresh
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
        
        log.info("NibeTokenRefreshJob execute() called - enabled: ${enabled}")
        
        if (!enabled) {
            log.info("NibeTokenRefreshJob is DISABLED via configuration, skipping execution")
            return
        }
        
        log.info("NibeTokenRefreshJob is ENABLED, proceeding with token refresh")
        
        def device = Device.findByModel(DeviceModel.NIBE_F1145_8_EM)
        if (!device) {
            log.error("Nibe device not found in database (model: NIBE_F1145_8_EM)")
            return
        }
        
        try {
            // Fetch configurations directly from database
            def clientIdConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                'cfg.key.device.oauth.client_id'
            )
            def clientSecretConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                'cfg.key.device.oauth.client_secret'
            )
            def refreshTokenConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                CfgKey.DEVICE.DEVICE_OAUTH_REFRESH_TOKEN.key()
            )
            def accessTokenConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key()
            )
            
            log.debug("Config lookup results: clientId=${clientIdConfig != null}, clientSecret=${clientSecretConfig != null}, refreshToken=${refreshTokenConfig != null}")
            
            def clientId = clientIdConfig?.value
            def clientSecret = clientSecretConfig?.value
            def refreshToken = refreshTokenConfig?.value
            
            log.debug("Config values: clientId=${clientId}, clientSecret=${clientSecret ? '***' : 'null'}, refreshToken=${refreshToken ? '***' : 'null'}")
            
            if (!clientId || !clientSecret || !refreshToken) {
                log.error("OAuth credentials not configured for device ${device.id}")
                log.error("Missing: clientId=${!clientId}, clientSecret=${!clientSecret}, refreshToken=${!refreshToken}")
                
                // Notify via Telegram if handler is available
                if (telegramBotHandler) {
                    telegramBotHandler.sendMessage("⚠️ Nibe Token Refresh Failed: OAuth credentials not configured for device ${device.id}")
                }
                return
            }
            
            log.debug("Refreshing OAuth tokens for device ${device.id}")
            
            // Call the token refresh method
            def tokens = regenerateTokens(clientId, clientSecret, refreshToken)
            
            if (!tokens) {
                log.error("Failed to regenerate tokens - null response")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            // Save new refresh token
            if (!refreshTokenConfig) {
                refreshTokenConfig = new Configuration(
                    entityId: device.id,
                    entityType: EntityType.DEVICE,
                    key: CfgKey.DEVICE.DEVICE_OAUTH_REFRESH_TOKEN.key()
                )
            }
            refreshTokenConfig.value = tokens.refresh_token
            refreshTokenConfig.save(flush: true)
            
            // Save new access token
            if (!accessTokenConfig) {
                accessTokenConfig = new Configuration(
                    entityId: device.id,
                    entityType: EntityType.DEVICE,
                    key: CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key()
                )
            }
            accessTokenConfig.value = tokens.access_token
            accessTokenConfig.save(flush: true)
            
            log.info("OAuth tokens refreshed successfully (expires in ${tokens.expires_in}s)")
            log.debug("New refresh token saved: ${tokens.refresh_token?.take(20)}...")
            log.debug("New access token saved: ${tokens.access_token?.take(20)}...")
            
            // Mark device as online if it was offline
            if (device.status == DeviceStatus.OFFLINE) {
                mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            }
            
        } catch (Exception e) {
            log.error("Exception during token refresh: ${e.message}", e)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
            
            // Notify via Telegram if handler is available
            if (telegramBotHandler) {
                telegramBotHandler.sendMessage("❌ Nibe Token Refresh Failed: ${e.message}")
            }
        }
    }

    /**
     * Get initial OAuth tokens using authorization code
     * This is used during initial setup only
     * 
     * @param clientId OAuth client ID
     * @param clientSecret OAuth client secret
     * @param authzCode Authorization code from OAuth flow
     * @param redirectUri Redirect URI used in OAuth flow
     * @return Map with access_token, refresh_token, expires_in, token_type, scope
     */
    Map getAuthTokens(String clientId, String clientSecret, String authzCode, String redirectUri) {
        try {
            log.debug("Getting initial tokens with authorization code")
            
            HttpResponse<String> response = Unirest.post("https://api.myuplink.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "authorization_code")
                .field("client_id", clientId)
                .field("client_secret", clientSecret)
                .field("code", authzCode)
                .field("redirect_uri", redirectUri)
                .asString()
            
            if (response.status != 200) {
                log.error("Failed to get initial tokens: HTTP ${response.status} - ${response.body}")
                return null
            }
            
            def tokens = new JsonSlurper().parseText(response.body)
            log.info("Initial tokens obtained successfully (expires in ${tokens.expires_in}s)")
            
            return tokens
            
        } catch (Exception e) {
            log.error("Exception getting initial tokens: ${e.message}", e)
            return null
        }
    }

    /**
     * Regenerate tokens using refresh token
     * This is the main method called by the job every 5 minutes
     * 
     * myUplink API returns:
     * - access_token: JWT token for API calls (expires in 1 hour)
     * - refresh_token: Token to get new access tokens (rotates on each refresh)
     * - expires_in: Seconds until access_token expires (typically 3600)
     * - token_type: "Bearer"
     * - scope: "READSYSTEM WRITESYSTEM offline_access"
     * 
     * IMPORTANT: The refresh_token is rotated on each call, so we must save the new one!
     * 
     * @param clientId OAuth client ID
     * @param clientSecret OAuth client secret
     * @param refreshToken Current refresh token
     * @return Map with new access_token, refresh_token, expires_in, token_type, scope
     */
    Map regenerateTokens(String clientId, String clientSecret, String refreshToken) {
        try {
            log.debug("Regenerating tokens with refresh token")
            
            HttpResponse<String> response = Unirest.post("https://api.myuplink.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "refresh_token")
                .field("client_id", clientId)
                .field("client_secret", clientSecret)
                .field("refresh_token", refreshToken)
                .asString()
            
            if (response.status != 200) {
                log.error("Failed to regenerate tokens: HTTP ${response.status} - ${response.body}")
                return null
            }
            
            def tokens = new JsonSlurper().parseText(response.body)
            
            // Validate response has required fields
            if (!tokens.access_token || !tokens.refresh_token) {
                log.error("Invalid token response - missing access_token or refresh_token")
                return null
            }
            
            log.info("Tokens regenerated successfully")
            log.debug("Token details: expires_in=${tokens.expires_in}s, token_type=${tokens.token_type}, scope=${tokens.scope}")
            
            return tokens
            
        } catch (Exception e) {
            log.error("Exception regenerating tokens: ${e.message}", e)
            return null
        }
    }
}