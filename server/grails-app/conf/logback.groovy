import grails.util.BuildSettings
import grails.util.Environment
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
                customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: "1.3.2")}"
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
    appender("FULL_STACKTRACE", FileAppender) {
        file = "/var/log/madhouse.log"
        append = true
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
                    customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: "1.3.2")}"
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
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    root(ERROR, ['STDOUT', 'FULL_STACKTRACE'])
} else {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "/var/log/madhouse.log"
        append = true
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
                    customFields = "${toJson(pid: "${new ApplicationPid()}", appVersion: "1.3.2")}"
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
    }
    root(ERROR, ['STDOUT', 'FULL_STACKTRACE'])
}

logger 'eu.devexpert', DEBUG, ['STDOUT', 'FULL_STACKTRACE']
logger 'org.springframework.security', ERROR, ['STDOUT']
logger 'grails.plugin.springsecurity', ERROR, ['STDOUT']
logger 'grails.plugin.springsecurity.web.filter.DebugFilter', ERROR, ['STDOUT']
logger 'org.springframework', ERROR, ['STDOUT']
logger 'org.hibernate.', ERROR, ['STDOUT']
logger 'org.hibernate.type.descriptor.sql.BasicBinder', ERROR, ['STDOUT']
logger 'org.hibernate.SQL', ERROR, ['STDOUT']