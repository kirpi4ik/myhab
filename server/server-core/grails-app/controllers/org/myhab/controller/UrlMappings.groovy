package org.myhab.controller

import grails.util.Environment

class UrlMappings {
    static excludes = ["/images/**", "/css/**", "/js/**", "/img/**", "/font/**", "/fonts/**", "/*.html"]
    static mappings = {
        get "/api/public/event"(controller: "event", action: "pubGetEvent")
        get "/pub-event"(controller: "event", action: "pubGetEvent")
        get "/e"(controller: "event", action: "shortUrlEvent")
        "/login/auth"(controller: "login", action: "auth")

        if (Environment.current == Environment.PRODUCTION) {
            "/"(uri: "/index.html")
            "/error"(uri: "/index.html")
            "/nx"(uri: "/nx/index.html")
            "/nx/wui"(uri: "/nx/index.html")
            "/nx/ports/**"(uri: "/nx/index.html")
            "/nx/login"(uri: "/nx/index.html")
            "/nx/zones/**"(uri: "/nx/index.html")
            "/nx/users/**"(uri: "/nx/index.html")
            "/nx/cables/**"(uri: "/nx/index.html")
            "/nx/devices/**"(uri: "/nx/index.html")
            "/nx/peripherals/**"(uri: "/nx/index.html")
        } else {
            "/"(controller: "application", action: "index")
            "/error"(controller: "application", action: "index")
        }

        "500"(view: "/error")
        "404"(view: "/notFound")
    }
}
