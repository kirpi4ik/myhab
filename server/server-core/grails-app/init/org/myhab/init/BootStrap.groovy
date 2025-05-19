package org.myhab.init

import kong.unirest.Unirest

import java.security.KeyStore

class BootStrap {
    def telegramBotHandler
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
    }
    def destroy = {
    }

}