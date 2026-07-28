grails.resources.pattern = '/**'

// There is no OAuth2 provider plugin on the classpath. The `/oauth/**` and
// `/securedOAuth2Resources/**` rules below, and the `-oauth2*Filter` subtractions,
// are therefore inert — subtracting an unregistered filter is a no-op.
// org.myhab.domain.auth.* survives only because UserService queries AccessToken /
// RefreshToken for revocation.

grails.plugin.springsecurity.userLookup.userDomainClassName = 'org.myhab.domain.User'
grails.plugin.springsecurity.userLookup.authoritiesPropertyName = 'authorities'
grails.plugin.springsecurity.userLookup.enabledPropertyName = 'enabled'
grails.plugin.springsecurity.userLookup.accountExpiredPropertyName = 'accountExpired'
grails.plugin.springsecurity.userLookup.accountLockedPropertyName = 'accountLocked'
grails.plugin.springsecurity.userLookup.passwordExpiredPropertyName = 'passwordExpired'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'org.myhab.domain.UserRole'
grails.plugin.springsecurity.authority.className = 'org.myhab.domain.Role'

// Password encoding configuration for Spring Security 5.3.0
grails.plugin.springsecurity.password.algorithm = 'bcrypt'
grails.plugin.springsecurity.password.bcrypt.logrounds = 10


grails.plugin.springsecurity.rest.login.active = true
grails.plugin.springsecurity.rest.login.endpointUrl = "/api/login"
grails.plugin.springsecurity.rest.login.failureStatusCode = 401
grails.plugin.springsecurity.rest.logout.endpointUrl = "/api/logout"
//grails.plugin.springsecurity.rest.token.validation.headerName = "X-Auth-Token"

grails.plugin.springsecurity.rest.token.storage.jwt.useSignedJwt = true
// Access-token TTL in seconds: 18_000_000 s = 300_000 min ≈ 208 days.
// Long-lived on purpose (home devices, wall tablets).
grails.plugin.springsecurity.rest.token.storage.jwt.expiration = 18000000
grails.plugin.springsecurity.rest.token.storage.jwt.secret = System.getenv("JWT_SECRET")
grails.plugin.springsecurity.rest.token.generation.jwt.algorithm = "HS256"
grails.plugin.springsecurity.rest.token.generation.jwt.jweAlgorithm = "RSA-OAEP"

//grails.plugin.springsecurity.rest.token.storage.jwt.useEncryptedJwt = true
//grails.plugin.springsecurity.rest.token.storage.jwt.privateKeyPath = '/path/to/private_key.der'
//grails.plugin.springsecurity.rest.token.storage.jwt.publicKeyPath = '/path/to/public_key.der'

grails.plugin.springsecurity.rest.token.rendering.usernamePropertyName = 'login'
grails.plugin.springsecurity.rest.token.rendering.authoritiesPropertyName = 'permissions'
grails.plugin.springsecurity.rest.token.validation.enableAnonymousAccess = true

grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        [pattern: '/**', access: ['permitAll']],
        [pattern: '/error', access: ['permitAll']],
        [pattern: '/index', access: ['permitAll']],
        [pattern: '/index.gsp', access: ['permitAll']],
        [pattern: '/shutdown', access: ['permitAll']],
        [pattern: '/assets/**', access: ['permitAll']],
        [pattern: '/**/js/**', access: ['permitAll']],
        [pattern: '/**/css/**', access: ['permitAll']],
        [pattern: '/**/images/**', access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/graphql/**', access: 'isAuthenticated()'],
        [pattern: '/actuator/**', access: ['permitAll']],
        [pattern: '/stomp/**', access: ['isFullyAuthenticated()']],//websokets must be changed to some role
        [pattern: '/admin/**', access: ['ROLE_ADMIN', 'isFullyAuthenticated()']],
        [pattern: '/oauth/authorize', access: "isFullyAuthenticated() and (request.getMethod().equals('GET') or request.getMethod().equals('POST'))"],
        [pattern: '/oauth/token', access: "isFullyAuthenticated() and request.getMethod().equals('POST')"]
]
grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/actuator/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/api/public/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        // OAuth2 callbacks from third-party providers (Navimow today). The
        // browser arriving here is NOT authenticated to myHAB — CSRF is enforced
        // by the random `state` param validated inside the controller.
        [pattern: '/auth/external/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/pub-event', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/e**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/#/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/graphql', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter, -oauth2ProviderFilter,-logoutFilter,-rememberMeAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter', access: ['ROLE_ADMIN']],
        [pattern: '/api/**', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
        [pattern: '/stomp**', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
        /*[pattern: '/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],*/
        [pattern: '/oauth/token', filters: 'JOINED_FILTERS,-oauth2ProviderFilter,-securityContextPersistenceFilter,-logoutFilter,-authenticationProcessingFilter,-rememberMeAuthenticationFilter,-exceptionTranslationFilter'],
        [pattern: '/securedOAuth2Resources/**', filters: 'JOINED_FILTERS,-securityContextPersistenceFilter,-logoutFilter,-authenticationProcessingFilter,-rememberMeAuthenticationFilter,-oauth2BasicAuthenticationFilter,-exceptionTranslationFilter'],
        [pattern: '/**', filters: 'JOINED_FILTERS,-statelessSecurityContextPersistenceFilter,-oauth2ProviderFilter,-clientCredentialsTokenEndpointFilter,-oauth2BasicAuthenticationFilter,-oauth2ExceptionTranslationFilter']
]

// Common DataSource configuration
dataSource {
    driverClassName = "org.postgresql.Driver"
    dialect = "org.hibernate.dialect.PostgreSQL95Dialect"
    properties {
        jmxEnabled = true
        initialSize = 5
        maxActive = 50
        minIdle = 5
        maxIdle = 25
        maxWait = 10000
        maxAge = 10 * 60 * 1000 // 10 minutes
        timeBetweenEvictionRunsMillis = 5000
        minEvictableIdleTimeMillis = 60000
        validationQuery = "SELECT 1"
        validationQueryTimeout = 3
        validationInterval = 15000
        testOnBorrow = true
        testWhileIdle = true
        testOnReturn = false
        jdbcInterceptors = "ConnectionState"
        defaultTransactionIsolation = 2 // TRANSACTION_READ_COMMITTED
        removeAbandoned = true
        removeAbandonedTimeout = 120
        logAbandoned = false
    }
}

// Quartz Scheduler Configuration
// Using native Quartz library (not the Grails plugin)
// Configuration is done in org.myhab.config.QuartzConfiguration

environments {
    development {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5432/madhouse?TimeZone=UTC"
            username = "myhab"
            password = "myhab"
            logSql = false
            formatSql = false
        }
        
    }
    production {
        dataSource {
            dbCreate = "update"
            // Append TimeZone=UTC to the connection URL if not already present
            url = System.getenv("DB_URL")?.contains("TimeZone=") ? System.getenv("DB_URL") : "${System.getenv('DB_URL')}?TimeZone=UTC"
            username = System.getenv("DB_USERNAME")
            password = System.getenv("DB_PASSWORD")
            logSql = false
            formatSql = false
        }
    }
}
