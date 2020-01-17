package madhouse.v2

import eu.devexpert.madhouse.alert.Alert
import eu.devexpert.madhouse.alert.AlertPriority

class BootStrap {
    def alertService

    def schedulerService
    def init = { servletContext ->
//        schedulerService.startAll()
//        def var = new Alert(AlertPriority.P1, "helo", "description")
//        alertService.sendAlert(var)
    }
    def destroy = {
//        schedulerService.shutdown()
    }

}
