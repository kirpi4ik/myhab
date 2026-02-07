import grails.util.Environment
import grails.util.Holders
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import org.myhab.async.mqtt.handlers.MQTTMessageHandler
import org.myhab.async.socket.WebSocketConfig
import org.myhab.auth.ClaimProvider
import org.myhab.config.ConfigProvider
import org.myhab.config.CustomPostgreSQLDelegate
import org.myhab.config.AutowiringSpringBeanJobFactory
import org.myhab.graphql.GQLSchemaFactory
import org.myhab.graphql.GraphQLGenerator
import org.myhab.graphql.fetchers.Mutation
import org.myhab.listener.domain.UserPasswordEncoderListener
import org.myhab.services.TelegramService
import org.myhab.telegram.TelegramBotHandler
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.JobDetailFactoryBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import org.quartz.SimpleTrigger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.myhab.jobs.NibeTokenRefreshJob
import org.myhab.jobs.NibeInfoSyncJob
import org.myhab.jobs.ConfigSyncJob
import org.myhab.jobs.DeviceControllerStateSyncJob
import org.myhab.jobs.HeatingControlJob
import org.myhab.jobs.EventLogReaderJob
import org.myhab.jobs.MeteoStationSyncJob
import org.myhab.jobs.HuaweiInfoSyncJob
import org.myhab.jobs.PortValueSyncTriggerJob
import org.myhab.jobs.SwitchOFFOnTimeoutJob
import org.myhab.jobs.RandomColorsJob
import org.myhab.jobs.RainbowRGB
import org.myhab.listener.quartz.JobHistoryListener
import org.quartz.SimpleTrigger

// Read configuration for static jobs
def config = Holders.grailsApplication?.config

// Get database connection details for Quartz (environment-aware)
def environment = grails.util.Environment.current.name
def dbUrl, dbUsername, dbPassword
if (environment == 'production') {
    dbUrl = System.getenv("DB_URL")?.contains("TimeZone=") ? System.getenv("DB_URL") : "${System.getenv('DB_URL')}?TimeZone=UTC"
    dbUsername = System.getenv("DB_USERNAME")
    dbPassword = System.getenv("DB_PASSWORD")
} else {
    // Development/Test environment
    dbUrl = 'jdbc:postgresql://localhost:5432/madhouse?TimeZone=UTC'
    dbUsername = 'myhab'
    dbPassword = 'myhab'
}

// Job intervals
def nibeTokenRefreshInterval = config?.getProperty('quartz.jobs.nibeTokenRefresh.interval', Integer, 300)
def nibeInfoSyncInterval = config?.getProperty('quartz.jobs.nibeInfoSync.interval', Integer, 60)
def configSyncInterval = config?.getProperty('quartz.jobs.configSync.interval', Integer, 60)
def deviceControllerStateSyncInterval = config?.getProperty('quartz.jobs.deviceControllerStateSync.interval', Integer, 60)
def heatingControlInterval = config?.getProperty('quartz.jobs.heatingControl.interval', Integer, 120)
def eventLogReaderInterval = config?.getProperty('quartz.jobs.eventLogReader.interval', Integer, 60)
def meteoStationSyncInterval = config?.getProperty('quartz.jobs.meteoStationSync.interval', Integer, 1800)
def huaweiInfoSyncInterval = config?.getProperty('quartz.jobs.huaweiInfoSync.interval', Integer, 300)
def portValueSyncTriggerInterval = config?.getProperty('quartz.jobs.portValueSyncTrigger.interval', Integer, 60)
def switchOffOnTimeoutInterval = config?.getProperty('quartz.jobs.switchOffOnTimeout.interval', Integer, 30)
def randomColorsInterval = config?.getProperty('quartz.jobs.randomColors.interval', Integer, 5)
def rainbowRGBInterval = config?.getProperty('quartz.jobs.rainbowRGB.interval', Integer, 80)

// Job enabled flags
def nibeTokenRefreshEnabled = config?.getProperty('quartz.jobs.nibeTokenRefresh.enabled', Boolean, true)
def nibeInfoSyncEnabled = config?.getProperty('quartz.jobs.nibeInfoSync.enabled', Boolean, true)
def configSyncEnabled = config?.getProperty('quartz.jobs.configSync.enabled', Boolean, true)
def deviceControllerStateSyncEnabled = config?.getProperty('quartz.jobs.deviceControllerStateSync.enabled', Boolean, true)
def heatingControlEnabled = config?.getProperty('quartz.jobs.heatingControl.enabled', Boolean, false)
def eventLogReaderEnabled = config?.getProperty('quartz.jobs.eventLogReader.enabled', Boolean, false)
def meteoStationSyncEnabled = config?.getProperty('quartz.jobs.meteoStationSync.enabled', Boolean, true)
def huaweiInfoSyncEnabled = config?.getProperty('quartz.jobs.huaweiInfoSync.enabled', Boolean, true)
def portValueSyncTriggerEnabled = config?.getProperty('quartz.jobs.portValueSyncTrigger.enabled', Boolean, true)
def switchOffOnTimeoutEnabled = config?.getProperty('quartz.jobs.switchOffOnTimeout.enabled', Boolean, true)
def randomColorsEnabled = config?.getProperty('quartz.jobs.randomColors.enabled', Boolean, false)
def rainbowRGBEnabled = config?.getProperty('quartz.jobs.rainbowRGB.enabled', Boolean, false)

// Build list of enabled triggers
def enabledTriggers = []
if (nibeTokenRefreshEnabled) enabledTriggers << 'nibeTokenRefreshTrigger'
if (nibeInfoSyncEnabled) enabledTriggers << 'nibeInfoSyncTrigger'
if (configSyncEnabled) enabledTriggers << 'configSyncTrigger'
if (deviceControllerStateSyncEnabled) enabledTriggers << 'deviceControllerStateSyncTrigger'
if (heatingControlEnabled) enabledTriggers << 'heatingControlTrigger'
if (eventLogReaderEnabled) enabledTriggers << 'eventLogReaderTrigger'
if (meteoStationSyncEnabled) enabledTriggers << 'meteoStationSyncTrigger'
if (huaweiInfoSyncEnabled) enabledTriggers << 'huaweiInfoSyncTrigger'
if (portValueSyncTriggerEnabled) enabledTriggers << 'portValueSyncTriggerTrigger'
if (switchOffOnTimeoutEnabled) enabledTriggers << 'switchOffOnTimeoutTrigger'
if (randomColorsEnabled) enabledTriggers << 'randomColorsTrigger'
if (rainbowRGBEnabled) enabledTriggers << 'rainbowRGBTrigger'

beans = {
    configProvider(ConfigProvider) {
        repoURI = System.getenv("CFG_REPO_URI")
        username = System.getenv("CFG_USERNAME")
        password = System.getenv("CFG_PASSWORD")
        branch = (Environment.current == Environment.PRODUCTION ? "prod" : (Environment.DEVELOPMENT ? "dev" : "beta"))
    }

    passwordEncoder(BCryptPasswordEncoder)
    userPasswordEncoderListener(UserPasswordEncoderListener)
    graphQLSchemaGenerator(GQLSchemaFactory) {
        deleteResponseHandler = ref("graphQLDeleteResponseHandler")
        namingConvention = ref("graphQLEntityNamingConvention")
        typeManager = ref("graphQLTypeManager")
        dataBinderManager = ref("graphQLDataBinderManager")
        dataFetcherManager = ref("graphQLDataFetcherManager")
        interceptorManager = ref("graphQLInterceptorManager")
        paginationResponseHandler = ref("graphQLPaginationResponseHandler")
        serviceManager = ref("graphQLServiceManager")

        dateFormats = '#{grailsGraphQLConfiguration.getDateFormats()}'
        dateFormatLenient = '#{grailsGraphQLConfiguration.getDateFormatLenient()}'
        listArguments = '#{grailsGraphQLConfiguration.getListArguments()}'
        gqlSchema = "classpath:schema.graphqls"
    }
    mutation(Mutation)
    graphQLGenrator(GraphQLGenerator) {
        schema = ref("graphQLSchema")
    }
    graphQL(graphQLGenrator: "generate")
    customClaimProvider(ClaimProvider)

    threadMetrics(JvmThreadMetrics)

    webSocketConfig(WebSocketConfig) {
        configProvider = ref("configProvider")
    }
    mQTTMessageHandler(MQTTMessageHandler)
    mQTTMessageHandler(MQTTMessageHandler)

    telegramBotHandler(TelegramBotHandler) {
        configProvider = ref("configProvider")
        userService = ref("userService")
        telegramService = ref("telegramService")
    }
    
    // ===========================
    // Static Quartz Jobs Configuration
    // ===========================
    // These are application-level jobs (former Grails auto-registered jobs)
    // They are Spring-managed beans with automatic dependency injection
    
    // NibeTokenRefreshJob - OAuth2 token refresh
    // Configurable via: quartz.jobs.nibeTokenRefresh.enabled and interval
    nibeTokenRefreshJobDetail(JobDetailFactoryBean) {
        jobClass = NibeTokenRefreshJob
        durability = true // Must be durable since triggers are registered separately
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Nibe myUplink OAuth2 Token Refresh - Spring Managed'
    }
    
    nibeTokenRefreshTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('nibeTokenRefreshJobDetail')
        startDelay = 10000L // 10 seconds delay after startup
        repeatInterval = nibeTokenRefreshInterval * 1000L // Convert seconds to milliseconds
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // NibeInfoSyncJob - Data synchronization
    // Configurable via: quartz.jobs.nibeInfoSync.enabled and interval
    nibeInfoSyncJobDetail(JobDetailFactoryBean) {
        jobClass = NibeInfoSyncJob
        durability = true // Must be durable since triggers are registered separately
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Nibe myUplink Data Sync - Spring Managed'
    }
    
    nibeInfoSyncTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('nibeInfoSyncJobDetail')
        startDelay = 30000L // 30 seconds delay after startup
        repeatInterval = nibeInfoSyncInterval * 1000L // Convert seconds to milliseconds
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // ConfigSyncJob - Configuration synchronization
    configSyncJobDetail(JobDetailFactoryBean) {
        jobClass = ConfigSyncJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Configuration Synchronization - Spring Managed'
    }
    
    configSyncTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('configSyncJobDetail')
        startDelay = 15000L
        repeatInterval = configSyncInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // DeviceControllerStateSyncJob - Device controller state sync via HTTP
    deviceControllerStateSyncJobDetail(JobDetailFactoryBean) {
        jobClass = DeviceControllerStateSyncJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Device Controller State Sync - Spring Managed'
    }
    
    deviceControllerStateSyncTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('deviceControllerStateSyncJobDetail')
        startDelay = 20000L
        repeatInterval = deviceControllerStateSyncInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // HeatingControlJob - Thermostat heating control
    heatingControlJobDetail(JobDetailFactoryBean) {
        jobClass = HeatingControlJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Heating Control - Spring Managed'
    }
    
    heatingControlTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('heatingControlJobDetail')
        startDelay = 60000L
        repeatInterval = heatingControlInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // EventLogReaderJob - Event log reader
    eventLogReaderJobDetail(JobDetailFactoryBean) {
        jobClass = EventLogReaderJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Event Log Reader - Spring Managed'
    }
    
    eventLogReaderTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('eventLogReaderJobDetail')
        startDelay = 25000L
        repeatInterval = eventLogReaderInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // MeteoStationSyncJob - Weather station data sync
    meteoStationSyncJobDetail(JobDetailFactoryBean) {
        jobClass = MeteoStationSyncJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Meteo Station Sync - Spring Managed'
    }
    
    meteoStationSyncTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('meteoStationSyncJobDetail')
        startDelay = 45000L
        repeatInterval = meteoStationSyncInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // HuaweiInfoSyncJob - Huawei FusionSolar data sync
    huaweiInfoSyncJobDetail(JobDetailFactoryBean) {
        jobClass = HuaweiInfoSyncJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Huawei FusionSolar Sync - Spring Managed'
    }
    
    huaweiInfoSyncTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('huaweiInfoSyncJobDetail')
        startDelay = 40000L
        repeatInterval = huaweiInfoSyncInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // PortValueSyncTriggerJob - MQTT port value sync trigger
    portValueSyncTriggerJobDetail(JobDetailFactoryBean) {
        jobClass = PortValueSyncTriggerJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Port Value Sync Trigger - Spring Managed'
    }
    
    portValueSyncTriggerTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('portValueSyncTriggerJobDetail')
        startDelay = 35000L
        repeatInterval = portValueSyncTriggerInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // SwitchOFFOnTimeoutJob - Auto-switch-off on timeout
    switchOffOnTimeoutJobDetail(JobDetailFactoryBean) {
        jobClass = SwitchOFFOnTimeoutJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Switch OFF On Timeout - Spring Managed'
    }
    
    switchOffOnTimeoutTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('switchOffOnTimeoutJobDetail')
        startDelay = 50000L
        repeatInterval = switchOffOnTimeoutInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // RandomColorsJob - Random colors (demo/testing)
    randomColorsJobDetail(JobDetailFactoryBean) {
        jobClass = RandomColorsJob
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Random Colors - Spring Managed'
    }
    
    randomColorsTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('randomColorsJobDetail')
        startDelay = 55000L
        repeatInterval = randomColorsInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // RainbowRGB - Rainbow RGB (demo/testing)
    rainbowRGBJobDetail(JobDetailFactoryBean) {
        jobClass = RainbowRGB
        durability = true
        requestsRecovery = false
        group = 'STATIC_JOBS'
        description = 'Rainbow RGB - Spring Managed'
    }
    
    rainbowRGBTrigger(SimpleTriggerFactoryBean) {
        jobDetail = ref('rainbowRGBJobDetail')
        startDelay = 60000L
        repeatInterval = rainbowRGBInterval * 1000L
        repeatCount = SimpleTrigger.REPEAT_INDEFINITELY
        group = 'STATIC_JOBS'
    }
    
    // ===========================
    // Spring-aware JobFactory for Quartz
    // ===========================
    // This enables Spring dependency injection for Quartz jobs
    quartzJobFactory(AutowiringSpringBeanJobFactory) { bean ->
        bean.autowire = 'byName'
    }
    
    // ===========================
    // Quartz Job History Listener
    // ===========================
    // Records job execution history to the database
    jobHistoryListener(JobHistoryListener)

    // ===========================
    // Quartz Scheduler Configuration
    // ===========================
    quartzScheduler(SchedulerFactoryBean) { bean ->
        bean.autowire = 'byName'
        jobFactory = ref('quartzJobFactory')  // Enable Spring autowiring for jobs
        overwriteExistingJobs = true
        waitForJobsToCompleteOnShutdown = true
        autoStartup = false  // Don't auto-start; BootStrap will start it after scheduling dynamic jobs
        
        // Register job listeners for execution history tracking
        globalJobListeners = [ref('jobHistoryListener')]
        
        // Register only enabled static job triggers
        triggers = enabledTriggers.collect { triggerName -> ref(triggerName) }
        
        quartzProperties = [
            'org.quartz.scheduler.instanceName': 'quartzScheduler',
            'org.quartz.scheduler.instanceId': 'quartzScheduler',
            'org.quartz.threadPool.class': 'org.quartz.simpl.SimpleThreadPool',
            'org.quartz.threadPool.threadCount': '25',
            'org.quartz.threadPool.threadPriority': '5',
            // Use Quartz's native JobStoreTX (manages its own transactions)
            'org.quartz.jobStore.class': 'org.quartz.impl.jdbcjobstore.JobStoreTX',
            'org.quartz.jobStore.driverDelegateClass': 'org.myhab.config.CustomPostgreSQLDelegate',
            'org.quartz.jobStore.tablePrefix': 'qrtz_',
            'org.quartz.jobStore.isClustered': 'false',
            'org.quartz.jobStore.useProperties': 'false',
            'org.quartz.jobStore.dataSource': 'quartzDS',
            // Quartz's own dataSource (environment-aware)
            'org.quartz.dataSource.quartzDS.driver': 'org.postgresql.Driver',
            'org.quartz.dataSource.quartzDS.URL': dbUrl,
            'org.quartz.dataSource.quartzDS.user': dbUsername,
            'org.quartz.dataSource.quartzDS.password': dbPassword,
            'org.quartz.dataSource.quartzDS.maxConnections': '5',
            'org.quartz.dataSource.quartzDS.validationQuery': 'SELECT 1'
        ]
    }
}