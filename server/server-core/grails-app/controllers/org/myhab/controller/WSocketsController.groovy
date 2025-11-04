package org.myhab.controller

import grails.web.controllers.ControllerMethod
import groovy.util.logging.Slf4j
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.security.access.prepost.PreAuthorize

@Slf4j
class WSocketsController {
    static responseFormats = ['json', 'xml']

    @ControllerMethod
    @MessageMapping("/hello")
    @PreAuthorize("hasRole('ROLE_USER')")
    @SendTo("/topic/hello")
    @SendToUser("/queue/hello")
    String hello(String world) {
        log.debug("CTRL hello")
        return "hello from secured controller, ${world}!"
    }

    @ControllerMethod
    @MessageExceptionHandler
    @SendToUser(value = "/queue/errors", broadcast = false)
    String handleException(Exception e) {
        return "caught ${e.message}"
    }

}
