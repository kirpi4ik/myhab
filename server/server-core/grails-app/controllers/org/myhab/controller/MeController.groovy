package org.myhab.controller

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.domain.User

/**
 * Returns current authenticated user info (e.g. id, username) for the frontend.
 * Used so the client can display current user avatar and profile.
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class MeController {

    SpringSecurityService springSecurityService

    /**
     * GET /api/me
     * Returns { id, username } of the current user.
     */
    def index() {
        def principal = springSecurityService?.principal
        if (!principal) {
            response.status = 401
            render([error: 'Not authenticated'] as JSON)
            return
        }
        String username = principal instanceof String ? principal : (principal?.username ?: principal?.toString())
        User user = User.findByUsername(username)
        if (!user) {
            response.status = 404
            render([error: 'User not found'] as JSON)
            return
        }
        render([id: user.id, username: user.username] as JSON)
    }
}
