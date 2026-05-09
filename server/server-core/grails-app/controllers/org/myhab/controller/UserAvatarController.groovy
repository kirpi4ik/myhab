package org.myhab.controller

import grails.converters.JSON
import grails.gorm.transactions.Transactional

import java.io.ByteArrayInputStream

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.domain.User

/**
 * REST controller for user avatar: GET (serve image) and PUT (upload).
 * Avatars are stored in User.avatar (bytea, max 3KB). Non-admin users can only access their own avatar.
 */
@Secured(['ROLE_ADMIN', 'ROLE_USER'])
class UserAvatarController {

    SpringSecurityService springSecurityService

    static allowedMethods = [
        show: 'GET',
        update: 'PUT'
    ]

    private static final int MAX_AVATAR_SIZE = 3 * 1024
    private static final List<String> ALLOWED_CONTENT_TYPES = ['image/png', 'image/jpeg', 'image/jpg']

    /**
     * GET /api/users/:id/avatar
     * Returns avatar image or 204 No Content if user has no avatar.
     */
    def show() {
        Long userId = params.long('id')
        if (!userId) {
            response.status = 400
            render([error: 'User ID is required'] as JSON)
            return
        }

        if (!canAccessUser(userId)) {
            response.status = 403
            render([error: 'Forbidden'] as JSON)
            return
        }

        User user = User.get(userId)
        if (!user) {
            response.status = 404
            render([error: 'User not found'] as JSON)
            return
        }

        if (!user.avatar || user.avatar.length == 0) {
            response.status = 204
            render(status: 204)
            return
        }

        String contentType = contentTypeFromBytes(user.avatar)
        render(file: new ByteArrayInputStream(user.avatar), contentType: contentType, fileName: 'avatar')
    }

    /**
     * PUT /api/users/:id/avatar
     * Accepts multipart/form-data with part named "avatar" or "file".
     */
    @Transactional
    def update() {
        Long userId = params.long('id')
        if (!userId) {
            response.status = 400
            render([error: 'User ID is required'] as JSON)
            return
        }

        if (!canAccessUser(userId)) {
            response.status = 403
            render([error: 'Forbidden'] as JSON)
            return
        }

        User user = User.get(userId)
        if (!user) {
            response.status = 404
            render([error: 'User not found'] as JSON)
            return
        }

        def file = request.getFile('avatar') ?: request.getFile('file')
        if (!file || file.empty) {
            response.status = 400
            render([error: 'No file uploaded. Use multipart form field "avatar" or "file".'] as JSON)
            return
        }

        if (file.size > MAX_AVATAR_SIZE) {
            response.status = 400
            render([error: "Avatar must not exceed ${MAX_AVATAR_SIZE / 1024}KB"] as JSON)
            return
        }

        String contentType = file.contentType?.toLowerCase()
        boolean allowed = contentType && (contentType == 'image/png' || contentType == 'image/jpeg' || contentType == 'image/jpg')
        if (!allowed) {
            response.status = 400
            render([error: 'Allowed types: image/png, image/jpeg'] as JSON)
            return
        }

        user.avatar = file.bytes
        user.save(flush: true, failOnError: true)

        response.status = 200
        render([success: true] as JSON)
    }

    private boolean canAccessUser(Long targetUserId) {
        def principal = springSecurityService?.principal
        if (!principal) return false

        String username = principal instanceof String ? principal : (principal?.username ?: principal?.toString())
        User currentUser = User.findByUsername(username)
        if (!currentUser) return false

        if (currentUser.id == targetUserId) return true
        // Spring Security Core 6.x removed springSecurityService.getAuthorities().
        // Read authorities off the loaded User instead — works regardless of whether
        // the JWT principal is a UserDetails or a bare username String.
        return currentUser.authorities?.any { it?.authority == 'ROLE_ADMIN' }
    }

    private static String contentTypeFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return 'image/png'
        // Compare via int (& 0xFF) to avoid signed-byte vs Integer-literal mismatches.
        // PNG signature: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) return 'image/png'
        // JPEG: FF D8
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return 'image/jpeg'
        return 'image/png'
    }
}
