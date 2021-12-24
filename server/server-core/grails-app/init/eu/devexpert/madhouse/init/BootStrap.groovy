package eu.devexpert.madhouse.init

import eu.devexpert.rules.facts.HeatControlInside
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules

class BootStrap {
    def telegramBotHandler
    def init = { servletContext ->
        telegramBotHandler.sendMessage("INFO", "\uD83D\uDE80 Salut! sistemul Madhouse tocmai a pornit")

    }
    def destroy = {
//        schedulerService.shutdown()
    }

}
