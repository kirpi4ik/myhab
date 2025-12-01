package org.myhab.config

import groovy.util.logging.Slf4j
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
import org.quartz.spi.ClassLoadHelper
import org.quartz.spi.OperableTrigger
import org.quartz.TriggerKey
import org.quartz.CronTrigger
import org.quartz.SimpleTrigger
import org.quartz.impl.triggers.CronTriggerImpl
import org.quartz.impl.triggers.SimpleTriggerImpl
import org.quartz.JobKey
import java.util.TimeZone

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

/**
 * Custom PostgreSQL delegate that properly handles BYTEA columns
 * 
 * The standard PostgreSQLDelegate uses getBlob() which expects OID references,
 * but our schema uses BYTEA columns for direct binary storage.
 * This delegate overrides the blob handling to use getBytes() instead.
 */
@Slf4j
class CustomPostgreSQLDelegate extends PostgreSQLDelegate {

    CustomPostgreSQLDelegate() {
        super()
        log.info("CustomPostgreSQLDelegate initialized for PostgreSQL BYTEA handling")
    }

    /**
     * Override to use getBytes() instead of getBlob() for BYTEA columns
     */
    @Override
    protected byte[] getJobDataFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
        // Use getBytes() for BYTEA columns instead of getBlob()
        byte[] data = rs.getBytes(colName)
        
        // Handle NULL case
        if (rs.wasNull()) {
            return null
        }
        
        return data
    }

    /**
     * Override to properly handle object deserialization from BYTEA
     */
    @Override
    protected Object getObjectFromBlob(ResultSet rs, String colName) throws ClassNotFoundException, IOException, SQLException {
        byte[] data = getJobDataFromBlob(rs, colName)
        
        if (data == null || data.length == 0) {
            return null
        }
        
        try {
            // Deserialize the object
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))
            Object obj = ois.readObject()
            ois.close()
            return obj
        } catch (Exception e) {
            log.error("Error deserializing object from BYTEA column ${colName}: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Complete override of selectTrigger to work around Quartz's ResultSet handling issue
     * with PostgreSQL. We execute the query and manually build the trigger from a fresh ResultSet.
     * Supports both CRON and SIMPLE triggers.
     */
    @Override
    OperableTrigger selectTrigger(Connection conn, TriggerKey triggerKey) throws SQLException {
        // Execute the JOIN query with BOTH cron and simple triggers
        String sql = """
            SELECT t.*, 
                   ct.CRON_EXPRESSION, ct.TIME_ZONE_ID,
                   st.REPEAT_COUNT, st.REPEAT_INTERVAL, st.TIMES_TRIGGERED
            FROM ${this.tablePrefix}TRIGGERS t
            LEFT OUTER JOIN ${this.tablePrefix}CRON_TRIGGERS ct 
                ON t.SCHED_NAME = ct.SCHED_NAME 
                AND t.TRIGGER_NAME = ct.TRIGGER_NAME 
                AND t.TRIGGER_GROUP = ct.TRIGGER_GROUP
            LEFT OUTER JOIN ${this.tablePrefix}SIMPLE_TRIGGERS st
                ON t.SCHED_NAME = st.SCHED_NAME
                AND t.TRIGGER_NAME = st.TRIGGER_NAME
                AND t.TRIGGER_GROUP = st.TRIGGER_GROUP
            WHERE t.SCHED_NAME = ? 
                AND t.TRIGGER_NAME = ? 
                AND t.TRIGGER_GROUP = ?
        """
        
        def ps = conn.prepareStatement(sql)
        ps.setString(1, this.instanceId)
        ps.setString(2, triggerKey.name)
        ps.setString(3, triggerKey.group)
        
        def rs = ps.executeQuery()
        
        try {
            if (!rs.next()) {
                return null
            }
            
            // Read trigger type
            def triggerType = rs.getString("TRIGGER_TYPE")
            
            if ("CRON".equals(triggerType)) {
                return buildCronTrigger(rs, triggerKey)
            } else if ("SIMPLE".equals(triggerType)) {
                return buildSimpleTrigger(rs, triggerKey)
            } else {
                log.warn("Unsupported trigger type: ${triggerType} for ${triggerKey}")
                return null
            }
            
        } catch (Exception e) {
            log.error("Failed to build trigger ${triggerKey}: ${e.message}", e)
            throw e
        } finally {
            rs.close()
            ps.close()
        }
    }
    
    /**
     * Build a CronTrigger from ResultSet data
     */
    private CronTriggerImpl buildCronTrigger(ResultSet rs, TriggerKey triggerKey) throws SQLException {
        def cronTrigger = new CronTriggerImpl()
        cronTrigger.setName(rs.getString("TRIGGER_NAME"))
        cronTrigger.setGroup(rs.getString("TRIGGER_GROUP"))
        
        def jobName = rs.getString("JOB_NAME")
        def jobGroup = rs.getString("JOB_GROUP")
        cronTrigger.setJobKey(new JobKey(jobName, jobGroup))
        
        def description = rs.getString("DESCRIPTION")
        if (description != null) {
            cronTrigger.setDescription(description)
        }
        
        def nextFireTime = rs.getLong("NEXT_FIRE_TIME")
        if (nextFireTime > 0) {
            cronTrigger.setNextFireTime(new Date(nextFireTime))
        }
        
        def prevFireTime = rs.getLong("PREV_FIRE_TIME")
        if (prevFireTime > 0) {
            cronTrigger.setPreviousFireTime(new Date(prevFireTime))
        }
        
        cronTrigger.setPriority(rs.getInt("PRIORITY"))
        
        def startTime = rs.getLong("START_TIME")
        cronTrigger.setStartTime(new Date(startTime))
        
        def endTime = rs.getLong("END_TIME")
        if (endTime > 0) {
            cronTrigger.setEndTime(new Date(endTime))
        }
        
        def misfireInstr = rs.getInt("MISFIRE_INSTR")
        cronTrigger.setMisfireInstruction(misfireInstr)
        
        // Read CRON-specific data
        def cronExpression = rs.getString("CRON_EXPRESSION")
        def timeZoneId = rs.getString("TIME_ZONE_ID")
        
        cronTrigger.setCronExpression(cronExpression)
        if (timeZoneId != null) {
            cronTrigger.setTimeZone(TimeZone.getTimeZone(timeZoneId))
        }
        
        // Read job data from BLOB
        try {
            def jobDataMap = getObjectFromBlob(rs, "JOB_DATA")
            if (jobDataMap != null) {
                cronTrigger.setJobDataMap(jobDataMap)
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize job data for trigger ${triggerKey}, using empty JobDataMap: ${e.message}")
        }
        
        return cronTrigger
    }
    
    /**
     * Build a SimpleTrigger from ResultSet data
     */
    private SimpleTriggerImpl buildSimpleTrigger(ResultSet rs, TriggerKey triggerKey) throws SQLException {
        def simpleTrigger = new SimpleTriggerImpl()
        simpleTrigger.setName(rs.getString("TRIGGER_NAME"))
        simpleTrigger.setGroup(rs.getString("TRIGGER_GROUP"))
        
        def jobName = rs.getString("JOB_NAME")
        def jobGroup = rs.getString("JOB_GROUP")
        simpleTrigger.setJobKey(new JobKey(jobName, jobGroup))
        
        def description = rs.getString("DESCRIPTION")
        if (description != null) {
            simpleTrigger.setDescription(description)
        }
        
        def nextFireTime = rs.getLong("NEXT_FIRE_TIME")
        if (nextFireTime > 0) {
            simpleTrigger.setNextFireTime(new Date(nextFireTime))
        }
        
        def prevFireTime = rs.getLong("PREV_FIRE_TIME")
        if (prevFireTime > 0) {
            simpleTrigger.setPreviousFireTime(new Date(prevFireTime))
        }
        
        simpleTrigger.setPriority(rs.getInt("PRIORITY"))
        
        def startTime = rs.getLong("START_TIME")
        simpleTrigger.setStartTime(new Date(startTime))
        
        def endTime = rs.getLong("END_TIME")
        if (endTime > 0) {
            simpleTrigger.setEndTime(new Date(endTime))
        }
        
        def misfireInstr = rs.getInt("MISFIRE_INSTR")
        simpleTrigger.setMisfireInstruction(misfireInstr)
        
        // Read SIMPLE-specific data
        def repeatCount = rs.getInt("REPEAT_COUNT")
        def repeatInterval = rs.getLong("REPEAT_INTERVAL")
        def timesTriggered = rs.getInt("TIMES_TRIGGERED")
        
        simpleTrigger.setRepeatCount(repeatCount)
        simpleTrigger.setRepeatInterval(repeatInterval)
        simpleTrigger.setTimesTriggered(timesTriggered)
        
        // Read job data from BLOB
        try {
            def jobDataMap = getObjectFromBlob(rs, "JOB_DATA")
            if (jobDataMap != null) {
                simpleTrigger.setJobDataMap(jobDataMap)
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize job data for trigger ${triggerKey}, using empty JobDataMap: ${e.message}")
        }
        
        return simpleTrigger
    }
}

