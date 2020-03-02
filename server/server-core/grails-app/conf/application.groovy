grails.resources.pattern = '/**'

grails.plugin.springsecurity.userLookup.userDomainClassName = 'eu.devexpert.madhouse.domain.User'
grails.plugin.springsecurity.userLookup.authoritiesPropertyName = 'authorities'
grails.plugin.springsecurity.userLookup.enabledPropertyName = 'enabled'
grails.plugin.springsecurity.userLookup.accountExpiredPropertyName = 'accountExpired'
grails.plugin.springsecurity.userLookup.accountLockedPropertyName = 'accountLocked'
grails.plugin.springsecurity.userLookup.passwordExpiredPropertyName = 'passwordExpired'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'eu.devexpert.madhouse.domain.UserRole'
grails.plugin.springsecurity.authority.className = 'eu.devexpert.madhouse.domain.Role'

grails.plugin.springsecurity.rest.login.active = true
grails.plugin.springsecurity.rest.login.endpointUrl = "/api/login"
grails.plugin.springsecurity.rest.login.failureStatusCode = 401
grails.plugin.springsecurity.rest.logout.endpointUrl = "/api/logout"
//grails.plugin.springsecurity.rest.token.validation.headerName = "X-Auth-Token"

grails.plugin.springsecurity.rest.token.storage.jwt.useSignedJwt = true
grails.plugin.springsecurity.rest.token.storage.jwt.expiration = 36000
grails.plugin.springsecurity.rest.token.storage.jwt.secret = '***REMOVED***'
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
        [pattern: '/admin/**', access: ['ROLE_ADMIN', 'isFullyAuthenticated()']]
]
grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/api/public/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/e**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/actuator/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/graphql', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter', access: ['ROLE_ADMIN']],
        [pattern: '/api/**', filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter'],
        [pattern: '/#/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor'],
        [pattern: '/**', filters: 'anonymousAuthenticationFilter,restTokenValidationFilter,restExceptionTranslationFilter,filterInvocationInterceptor']
]
