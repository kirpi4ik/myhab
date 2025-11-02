import ch.qos.logback.classic.Level
import ch.qos.logback.core.util.FileSize
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider
import net.logstash.logback.composite.loggingevent.*
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder
import net.logstash.logback.stacktrace.ShortenedThrowableConverter
import org.springframework.boot.system.ApplicationPid

import static groovy.json.JsonOutput.toJson

//conversionRule 'clr', ColorConverterrter
//conversionRule 'wex', WhitespaceThrowableProxyConverteronverter

def jsonPattern = """{"log":{"ts_date":"%d{ISO8601}", "thread":"[%t]", "level" : "%level", "reqMethod":"%X{reqMethod}","reqUrl":"%X{reqUrl}","reqPayload":%X{reqPayload},"reqHeaders":%X{reqHeaders},"authUser":"%X{authUser}","remoteIp":"%X{remoteIp}", "requestId":"%X{requestId}","respStatus":"%X{respStatus}", "logger":"%logger{36}", "message": "%msg", "stacktrace":"%xThrowable" }}%n"""
// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(LoggingEventCompositeJsonEncoder) {
        providers(LoggingEventJsonProviders) {
            timestamp(LoggingEventFormattedTimestampJsonProvider) {
                fieldName = 'ts_date'
                timeZone = 'UTC'
                pattern = 'yyyy-MM-dd HH:mm:ss.SSS'
            }
            logLevel(LogLevelJsonProvider) {
                fieldName = "level"
            }
            loggerName(LoggerNameJsonProvider) {
                fieldName = 'logger'
                shortenedLoggerNameLength = 35
            }
            message(MessageJsonProvider) {
                fieldName = 'message'
            }
            globalCustomFields(GlobalCustomFieldsJsonProvider) {
                customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: Metadata.current.'info.app.version', jvm: System.getProperty('java.version'))}"
            }
            threadName(ThreadNameJsonProvider) {
                fieldName = 'thread'
            }
            mdc(MdcJsonProvider) {
//        includeMdcKeyNames = ["reqMethod", "reqUrl", "request", "authUser", "remoteIp", "requestId", "respStatus"]

            }
            arguments(ArgumentsJsonProvider)
            stackTrace(StackTraceJsonProvider) {
                fieldName = "stacktrace"
                throwableConverter(ShortenedThrowableConverter) {
                    maxDepthPerThrowable = 20
                    maxLength = 8192
                    shortenedClassNameLength = 35
                    exclude = /sun\..*/
                    exclude = /java\..*/
                    exclude = /groovy\..*/
                    exclude = /com\.sun\..*/
                    rootCauseFirst = true
                }
            }
        }
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", RollingFileAppender) {
        file = "/var/log/myhab.log"
        encoder(LoggingEventCompositeJsonEncoder) {
            providers(LoggingEventJsonProviders) {
                timestamp(LoggingEventFormattedTimestampJsonProvider) {
                    fieldName = 'ts_date'
                    timeZone = 'UTC'
                    pattern = 'yyyy-MM-dd HH:mm:ss.SSS'
                }
                logLevel(LogLevelJsonProvider) {
                    fieldName = "level"
                }
                loggerName(LoggerNameJsonProvider) {
                    fieldName = 'logger'
                    shortenedLoggerNameLength = 35
                }
                message(MessageJsonProvider) {
                    fieldName = 'message'
                }
                globalCustomFields(GlobalCustomFieldsJsonProvider) {
                    customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: Metadata.current.'info.app.version', jvm: System.getProperty('java.version'))}"
                }
                threadName(ThreadNameJsonProvider) {
                    fieldName = 'thread'
                }
                mdc(MdcJsonProvider) {
//          includeMdcKeyNames = ["reqMethod", "reqUrl", "request", "authUser", "remoteIp", "requestId", "respStatus"]
                }
                arguments(ArgumentsJsonProvider)
                stackTrace(StackTraceJsonProvider) {
                    fieldName = "stacktrace"
                    throwableConverter(ShortenedThrowableConverter) {
                        maxDepthPerThrowable = 20
                        maxLength = 8192
                        shortenedClassNameLength = 35
                        exclude = /sun\..*/
                        exclude = /java\..*/
                        exclude = /groovy\..*/
                        exclude = /com\.sun\..*/
                        rootCauseFirst = true
                    }
                }
            }
        }
        rollingPolicy(FixedWindowRollingPolicy) {
            fileNamePattern = "/var/log/myhab-%i.log.zip"
            minIndex = 1
            maxIndex = 10
        }
        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = FileSize.valueOf("10 mb")
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    root(ERROR, ['STDOUT', 'FULL_STACKTRACE'])
} else {
    appender("FULL_STACKTRACE", RollingFileAppender) {
        file = "/var/log/myhab.log"
        encoder(LoggingEventCompositeJsonEncoder) {
            providers(LoggingEventJsonProviders) {
                timestamp(LoggingEventFormattedTimestampJsonProvider) {
                    fieldName = 'ts_date'
                    timeZone = 'UTC'
                    pattern = 'yyyy-MM-dd HH:mm:ss.SSS'
                }
                logLevel(LogLevelJsonProvider) {
                    fieldName = "level"
                }
                loggerName(LoggerNameJsonProvider) {
                    fieldName = 'logger'
                    shortenedLoggerNameLength = 35
                }
                message(MessageJsonProvider) {
                    fieldName = 'message'
                }
                globalCustomFields(GlobalCustomFieldsJsonProvider) {
                    customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: Metadata.current.'info.app.version', jvm: System.getProperty('java.version'))}"
                }
                threadName(ThreadNameJsonProvider) {
                    fieldName = 'thread'
                }
                mdc(MdcJsonProvider) {
//          includeMdcKeyNames = ["reqMethod", "reqUrl", "request", "authUser", "remoteIp", "requestId", "respStatus"]
                }
                arguments(ArgumentsJsonProvider)
                stackTrace(StackTraceJsonProvider) {
                    fieldName = "stacktrace"
                    throwableConverter(ShortenedThrowableConverter) {
                        maxDepthPerThrowable = 20
                        maxLength = 8192
                        shortenedClassNameLength = 35
                        exclude = /sun\..*/
                        exclude = /java\..*/
                        exclude = /groovy\..*/
                        exclude = /com\.sun\..*/
                        rootCauseFirst = true
                    }
                }
            }
        }
        rollingPolicy(FixedWindowRollingPolicy) {
            fileNamePattern = "/var/log/myhab-%i.log.zip"
            minIndex = 1
            maxIndex = 10
        }
        triggeringPolicy(SizeBasedTriggeringPolicy) {
            maxFileSize = FileSize.valueOf("10 mb")
        }
    }
    root(ERROR, ['STDOUT', 'FULL_STACKTRACE'])
}

logger 'org.myhab', DEBUG, ['STDOUT', 'FULL_STACKTRACE'], additivity = false
logger 'graphql', WARN, ['STDOUT', 'FULL_STACKTRACE'], additivity = false
//logger 'org.jsoup', DEBUG, ['STDOUT']
//logger 'org.springframework.web.socket', DEBUG, ['STDOUT']
//logger 'org.springframework.messaging', DEBUG, ['STDOUT']
//logger 'org.springframework.security', DEBUG, ['STDOUT']
//Uncomment these lines for Spring Security debugging if needed:
//logger 'grails.plugin.springsecurity', DEBUG, ['STDOUT']
//logger 'org.springframework.security', DEBUG, ['STDOUT']
//logger 'grails.plugin.springsecurity.web.filter.DebugFilter', DEBUG, ['STDOUT']
logger 'org.springframework', ERROR, ['STDOUT'], additivity = false
logger 'org.hibernate.', ERROR, ['STDOUT'], additivity = false
//logger 'org.hibernate.type.descriptor.sql.BasicBinder', DEBUG, ['STDOUT']
//logger 'org.hibernate.SQL', DEBUG, ['STDOUT']