package org.myhab.init

import groovy.util.logging.Slf4j
import kong.unirest.Unirest

import java.security.KeyStore

@Slf4j
class BootStrap {
    def telegramBotHandler
    def schedulerService
    
    def init = { servletContext ->
//        telegramBotHandler.sendMessage("INFO", "\uD83D\uDE80 Salut! sistemul myHAB tocmai a pornit")

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "changeit";
        keystore.load(BootStrap.class.getResourceAsStream("jssecacerts"), password.toCharArray());
        Unirest.config()
                .reset()
                .socketTimeout(2000)
                .connectTimeout(2000)
                .setDefaultHeader("Accept", "application/json")
                .followRedirects(true)
                .enableCookieManagement(false)
                .verifySsl(false)
                .clientCertificateStore(keystore, password)
        
        // Auto-start all ACTIVE jobs in Quartz scheduler
        try {
            log.info("Initializing Quartz scheduler: Starting all ACTIVE jobs...")
            schedulerService.startAll()
            log.info("Quartz scheduler initialized successfully")
        } catch (Exception e) {
            log.error("Failed to initialize Quartz scheduler: ${e.message}", e)
        }
    }
    
    def destroy = {
    }

}