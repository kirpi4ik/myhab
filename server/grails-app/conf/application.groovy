grails.resources.pattern = '/**'
grails.plugin.springsecurity.rest.token.storage.jwt.secret = '***REMOVED***'
grails.plugin.springsecurity.filterChain.chainMap = [
        //Stateless chain
        [
                pattern: '/api/**',
                filters: 'JOINED_FILTERS,-anonymousAuthenticationFilter,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-rememberMeAuthenticationFilter'
        ],

        //Traditional, stateful chain
        [
                pattern: '/stateful/**',
                filters: 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter'
        ]
]
