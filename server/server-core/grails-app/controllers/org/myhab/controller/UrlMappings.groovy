package org.myhab.controller

import grails.util.Environment

class UrlMappings {
    static excludes = ["/images/**", "/css/**", "/js/**", "/img/**", "/font/**", "/fonts/**", "/*.html"]
    static mappings = {
        get '/api/public/event'(controller: "event", action: "pubGetEvent")
        get '/pub-event'(controller: "event", action: "pubGetEvent")
        get '/e'(controller: "event", action: "shortUrlEvent")
        "/login/auth"(controller:'login', action:'auth')


//        delete "/$controller/$id(.$format)?"(action: "delete")
//        get "/$controller(.$format)?"(action: "index")
//        get "/$controller/$id(.$format)?"(action: "show")
//        post "/$controller(.$format)?"(action: "save")
//        put "/$controller/$id(.$format)?"(action: "update")
//        patch "/$controller/$id(.$format)?"(action: "patch")

        //tag::defaultPage[]
        if (Environment.current == Environment.PRODUCTION) {
            '/'(uri: '/index.html')
            '/error'(uri: '/index.html')
            '/nx'(uri: '/nx/index.html')
            '/nx/**'(uri: '/nx/index.html')
            '/nx/error'(uri: '/nx/index.html')
        } else {
            '/'(controller: 'application', action: 'index')
            '/error'(controller: 'application', action: 'index')
        }
        //end::defaultPage[]

        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
