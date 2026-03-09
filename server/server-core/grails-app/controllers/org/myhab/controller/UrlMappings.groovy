package org.myhab.controller

import grails.util.Environment

class UrlMappings {
    static excludes = ["/images/**", "/css/**", "/js/**", "/img/**", "/font/**", "/fonts/**", "/*.html"]
    static mappings = {
        // Permanent redirect from /nx* to /*
        "/nx/$path**"(redirect: [uri: "/$path", permanent: true])
        
        get "/api/public/event"(controller: "event", action: "pubGetEvent")
        get "/pub-event"(controller: "event", action: "pubGetEvent")
        get "/e"(controller: "event", action: "shortUrlEvent")

        get "/api/public/share/$token"(controller: "sharedWidget", action: "show")
        post "/api/public/share/$token/action"(controller: "sharedWidget", action: "executeAction")
        
        // Label generation API
        get "/api/labels/cable/$id"(controller: "label", action: "generateCableLabel")
        get "/api/labels/templates"(controller: "label", action: "templates")
        get "/api/labels/fields"(controller: "label", action: "fields")
        get "/api/me"(controller: "me", action: "index")
        get "/api/users/$id/avatar"(controller: "userAvatar", action: "show")
        put "/api/users/$id/avatar"(controller: "userAvatar", action: "update")
        "/login/auth"(controller: "login", action: "auth")

        if (Environment.current == Environment.PRODUCTION) {
            "/"(uri: "/index.html")
            "/error"(uri: "/index.html")
            "/wui"(uri: "/index.html")
            "/ports/**"(uri: "/index.html")
            "/login"(uri: "/index.html")
            "/admin/**"(uri: "/index.html")
            "/zones/**"(uri: "/index.html")
            "/users/**"(uri: "/index.html")
            "/cables/**"(uri: "/index.html")
            "/devices/**"(uri: "/index.html")
            "/peripherals/**"(uri: "/index.html")
            "/shared/**"(uri: "/index.html")
            "/messages"(uri: "/index.html")
        } else {
            "/"(controller: "application", action: "index")
            "/error"(controller: "application", action: "index")
        }

        "500"(view: "/error")
        "404"(view: "/notFound")
    }
}
