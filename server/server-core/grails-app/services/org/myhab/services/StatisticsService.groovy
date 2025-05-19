package org.myhab.services

import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.myhab.domain.EntityType
import org.myhab.domain.TimeSeriesStatistic
import org.myhab.domain.common.Event
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortValue
import org.myhab.domain.events.TopicName

@Transactional
class StatisticsService implements EventPublisher {

    def serviceMethod() {

    }

    void saveActivePowerStatistics(String key, Date intervalStart, Date intervalEnd) {
        def activePower = DevicePort.findByInternalRef('total_active_power')

        def portValue = PortValue.createCriteria().list {
            eq('portId', activePower.getId())
            and {
                between('tsCreated', intervalStart, intervalEnd)
            }
            maxResults(1)
            order('tsCreated', 'asc')
        }.find()?.value
        if (activePower && portValue) {
            TimeSeriesStatistic previousStat = TimeSeriesStatistic.createCriteria().list {
                eq('key', "emeters.${activePower.getDevice().getCode()}.${key}")
                order('tsCreated', 'asc')
                maxResults(1)
            }?.find()
            TimeSeriesStatistic tss = new TimeSeriesStatistic()
            def aggregatedValue = (Float.parseFloat(activePower.value) - Float.parseFloat(portValue)).round(3)
            if (previousStat && aggregatedValue) {
                tss.setDeltaDiff((aggregatedValue - previousStat.value).round(2))
            }
            tss.setKey("emeters.${activePower.getDevice().getCode()}.${key}")
            tss.setValue(aggregatedValue)
            tss.save(flush: true, failOnError: true)

            publish(TopicName.EVT_STAT_VALUE_CHANGED.id(), new Event().with {
                p0 = TopicName.EVT_STAT_VALUE_CHANGED.id()
                p1 = EntityType.TS_STATISTIC.name()
                p2 = "emeters.${activePower.getDevice().getCode()}.${key}"
                p3 = tss.id
                p4 = tss.value
                p5 = "agg-job"
                it
            })
        }
    }
}
