grails.resources.pattern = '/**'

grails.plugin.springsecurity.oauthProvider.clientLookup.className = 'org.myhab.domain.auth.Client'
grails.plugin.springsecurity.oauthProvider.accessTokenLookup.className = 'org.myhab.domain.auth.AccessToken'
grails.plugin.springsecurity.oauthProvider.refreshTokenLookup.className = 'org.myhab.domain.auth.RefreshToken'
grails.plugin.springsecurity.oauthProvider.authorizationCodeLookup.className = 'org.myhab.domain.auth.AuthorizationCode'
grails.plugin.springsecurity.oauthProvider.authorizationEndpointUrl = '/oauth/authorize'
grails.plugin.springsecurity.oauthProvider.tokenEndpointUrl = '/oauth/token'
grails.plugin.springsecurity.oauthProvider.errorEndpointUrl = '/oauth/error'
grails.plugin.springsecurity.oauthProvider.userApprovalEndpointUrl = '/oauth/confirm_access'
grails.plugin.springsecurity.oauthProvider.userApprovalParameter = 'user_oauth_approval'
grails.plugin.springsecurity.oauthProvider.active = true

grails.plugin.springsecurity.userLookup.userDomainClassName = 'org.myhab.domain.User'
grails.plugin.springsecurity.userLookup.authoritiesPropertyName = 'authorities'
grails.plugin.springsecurity.userLookup.enabledPropertyName = 'enabled'
grails.plugin.springsecurity.userLookup.accountExpiredPropertyName = 'accountExpired'
grails.plugin.springsecurity.userLookup.accountLockedPropertyName = 'accountLocked'
grails.plugin.springsecurity.userLookup.passwordExpiredPropertyName = 'passwordExpired'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'org.myhab.domain.UserRole'
grails.plugin.springsecurity.authority.className = 'org.myhab.domain.Role'

grails.plugin.springsecurity.rest.login.active = true
grails.plugin.springsecurity.rest.login.endpointUrl = "/api/login"
grails.plugin.springsecurity.rest.login.failureStatusCode = 401
grails.plugin.springsecurity.rest.logout.endpointUrl = "/api/logout"
//grails.plugin.springsecurity.rest.token.validation.headerName = "X-Auth-Token"

grails.plugin.springsecurity.rest.token.storage.jwt.useSignedJwt = true
grails.plugin.springsecurity.rest.token.storage.jwt.expiration = 36000
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

dataSource {
    driverClassName = "org.postgresql.Driver"
    dialect = "org.hibernate.dialect.PostgreSQLDialect"
    properties {
        jmxEnabled = true
        initialSize = 5
        maxActive = 50
        minIdle = 5
        maxIdle = 25
        maxWait = 10000
        maxAge = 60000
        timeBetweenEvictionRunsMillis = 5000
        minEvictableIdleTimeMillis = 60000
        validationQuery = "SELECT 1"
        validationQueryTimeout = 3
        validationInterval = 15000
        testOnBorrow = true
        testWhileIdle = true
        testOnReturn = false
        jdbcInterceptors = "ConnectionState"
        defaultTransactionIsolation = 2 //# TRANSACTION_READ_COMMITTED
    }
}
environments {
    development {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5432/myhab"
            username = "myhab"
            password = "myhab"
            logSql = false
            formatSql = false
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = System.getenv("DB_URL")
            username = System.getenv("DB_USERNAME")
            password = System.getenv("DB_PASSWORD")
            logSql = false
            formatSql = false
        }
    }
}
