package org.myhab.domain.common

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/**
 * Utility class for timezone conversions.
 * 
 * Database Storage Strategy:
 * - All timestamps are stored in UTC in the database
 * - All Date fields in domain objects represent UTC time
 * - Use this utility to convert to/from local timezone for display purposes only
 * 
 * Usage Examples:
 * 
 * // Converting UTC Date to local timezone for display
 * Date utcDate = device.tsCreated
 * String localTime = TimezoneUtil.formatInLocalTimezone(utcDate, "Europe/Bucharest")
 * 
 * // Converting local time string to UTC Date for storage
 * Date utcDate = TimezoneUtil.parseFromLocalTimezone("2025-11-16 14:30:00", "Europe/Bucharest")
 * 
 * // Getting current UTC time (for manual timestamp creation)
 * Date now = TimezoneUtil.nowUtc()
 */
class TimezoneUtil {
    
    /**
     * Get current UTC time as Date
     * @return Current time in UTC
     */
    static Date nowUtc() {
        return DateTime.now(DateTimeZone.UTC).toDate()
    }
    
    /**
     * Format a UTC Date in a specific timezone for display
     * @param utcDate Date in UTC (from database)
     * @param timezoneId Timezone ID (e.g., "Europe/Bucharest", "America/New_York")
     * @param format Date format pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return Formatted string in the specified timezone
     */
    static String formatInLocalTimezone(Date utcDate, String timezoneId, String format = "yyyy-MM-dd HH:mm:ss") {
        if (!utcDate) return null
        
        DateTimeZone timezone = DateTimeZone.forID(timezoneId)
        DateTime utcDateTime = new DateTime(utcDate, DateTimeZone.UTC)
        DateTime localDateTime = utcDateTime.withZone(timezone)
        
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format)
        return localDateTime.toString(formatter)
    }
    
    /**
     * Format a UTC Date in system default timezone for display
     * @param utcDate Date in UTC (from database)
     * @param format Date format pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return Formatted string in the default timezone
     */
    static String formatInDefaultTimezone(Date utcDate, String format = "yyyy-MM-dd HH:mm:ss") {
        if (!utcDate) return null
        
        DateTime utcDateTime = new DateTime(utcDate, DateTimeZone.UTC)
        DateTime localDateTime = utcDateTime.withZone(DateTimeZone.getDefault())
        
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format)
        return localDateTime.toString(formatter)
    }
    
    /**
     * Parse a local time string and convert to UTC Date for storage
     * @param localTimeString Time string in local timezone
     * @param timezoneId Timezone ID of the input string
     * @param format Date format pattern (default: "yyyy-MM-dd HH:mm:ss")
     * @return Date in UTC ready for database storage
     */
    static Date parseFromLocalTimezone(String localTimeString, String timezoneId, String format = "yyyy-MM-dd HH:mm:ss") {
        if (!localTimeString) return null
        
        DateTimeZone timezone = DateTimeZone.forID(timezoneId)
        DateTimeFormatter formatter = DateTimeFormat.forPattern(format).withZone(timezone)
        DateTime localDateTime = formatter.parseDateTime(localTimeString)
        
        // Convert to UTC
        return localDateTime.withZone(DateTimeZone.UTC).toDate()
    }
    
    /**
     * Convert a Date from one timezone to another
     * @param date Input date
     * @param fromTimezoneId Source timezone
     * @param toTimezoneId Target timezone
     * @return Converted date
     */
    static Date convertTimezone(Date date, String fromTimezoneId, String toTimezoneId) {
        if (!date) return null
        
        DateTimeZone fromZone = DateTimeZone.forID(fromTimezoneId)
        DateTimeZone toZone = DateTimeZone.forID(toTimezoneId)
        
        DateTime fromDateTime = new DateTime(date, fromZone)
        return fromDateTime.withZone(toZone).toDate()
    }
    
    /**
     * Get the offset in hours between UTC and a specific timezone at a given time
     * @param timezoneId Timezone ID
     * @param date Date to check offset for (accounts for DST)
     * @return Offset in hours (e.g., 2.0 for UTC+2, -5.0 for UTC-5)
     */
    static double getTimezoneOffset(String timezoneId, Date date = new Date()) {
        DateTimeZone timezone = DateTimeZone.forID(timezoneId)
        DateTime dateTime = new DateTime(date, timezone)
        return timezone.getOffset(dateTime) / 3600000.0
    }
}

