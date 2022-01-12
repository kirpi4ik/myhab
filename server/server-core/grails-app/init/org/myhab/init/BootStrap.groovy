package org.myhab.init

class BootStrap {
    def telegramBotHandler
    def init = { servletContext ->
        telegramBotHandler.sendMessage("INFO", "\uD83D\uDE80 Salut! sistemul myHAB tocmai a pornit")
    }
    def destroy = {
    }

}
