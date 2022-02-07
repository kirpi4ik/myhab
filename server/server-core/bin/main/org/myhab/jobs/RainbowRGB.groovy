package org.myhab.jobs

import grails.events.EventPublisher
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

class RainbowRGB implements Job, EventPublisher {
    static triggers = {
        simple name: 'rainbowRGBColors', repeatInterval: TimeUnit.SECONDS.toMillis(80)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {

    }
/*
    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        Random rnd = new Random()

        def cnt = 0
        def cmd_list = ["500000", "005000", "000050", "505050", "000000"]
        def color = 0

        while (true) {
            def cmd = ""
            (0..12).each { it ->
                if (it == cnt) {
                    cmd += cmd_list[color];
                }
                cmd += "000000";

            }
            cnt++;
            if (cnt == 12) {
                cnt = 0;
                color = rnd.nextInt(4)
            }
            try {
                Jsoup.connect("http://192.168.1.52/sec/sec/?pt=35&ws=$cmd").get()
            } catch (SocketTimeoutException e) {
            }
            sleep(20)
        }
    }*/
}
