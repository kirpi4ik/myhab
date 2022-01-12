package org.myhab

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Value

@CompileStatic
class CorsInterceptor implements GrailsConfigurationAware {

    int order = HIGHEST_PRECEDENCE + 200

    Config config
    @Value('${allowedOrigin}')
    String allowedOrigin

    CorsInterceptor() {
        matchAll().excludes(controller: 'auth')
    }

    boolean before() { true }

    boolean after() {
//        String origin = request.getHeader("Origin");
//        boolean options = "OPTIONS".equals(request.getMethod());
//        def allowedOrigins = allowedOrigin.split(",")
//        if (options) {
//            if (origin == null) return;
//            response.addHeader("Access-Control-Allow-Headers", "origin, authorization, accept, content-type, x-requested-with");
//            response.addHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS");
//            response.addHeader("Access-Control-Max-Age", "3600");
//        }
//        response.addHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        response.addHeader("Access-Control-Allow-Credentials", "true");

        return true
    }

    void afterView() {
        // no-op
    }

    @Override
    void setConfiguration(Config co) {
        this.config = co
    }
}
