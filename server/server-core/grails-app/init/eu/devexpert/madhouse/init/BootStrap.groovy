package eu.devexpert.madhouse.init

class BootStrap {
    def telegramBotHandler
    def init = { servletContext ->
        telegramBotHandler.sendMessage("INFO", "\uD83D\uDE80 Salut! sistemul Madhouse tocmai a pornit")
    }
    def destroy = {
    }

}
