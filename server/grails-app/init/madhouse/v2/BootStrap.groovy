package madhouse.v2

class BootStrap {

    def schedulerService
    def init = { servletContext ->
        schedulerService.startAll()
    }
    def destroy = {
        schedulerService.shutdown()
    }

}
