package org.myhab.controller

import grails.converters.JSON
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.SharedWidgetType
import org.myhab.domain.device.DevicePeripheral

@Secured(['permitAll'])
class SharedWidgetController implements EventPublisher {

    static responseFormats = ['json']
    static allowedMethods = [show: 'GET', executeAction: 'POST', verifyPin: 'POST']

    /**
     * GET /api/public/share/:token
     * Returns widget metadata without revealing sensitive data (pin).
     */
    def show() {
        String token = params.token
        if (!token) {
            response.status = 400
            render([error: 'Token is required'] as JSON)
            return
        }

        SharedWidget widget = SharedWidget.findByToken(token)
        if (!widget) {
            response.status = 404
            render([error: 'Share link not found'] as JSON)
            return
        }

        String effectiveState = resolveEffectiveState(widget)

        String peripheralName = ''
        try {
            def peripheral = DevicePeripheral.get(widget.peripheralId)
            peripheralName = peripheral?.name ?: ''
        } catch (ignored) {}

        render([
            widgetType    : widget.widgetType.name(),
            requiresPin   : widget.pin != null && !widget.pin.isEmpty(),
            state         : effectiveState,
            peripheralName: peripheralName,
            actionsAllowed: widget.actionsAllowed,
            actionsUsed   : widget.actionsUsed,
        ] as JSON)
    }

    /**
     * POST /api/public/share/:token/verify-pin
     * Verifies the PIN for a shared widget without executing any action.
     */
    def verifyPin() {
        String token = params.token
        if (!token) {
            response.status = 400
            render([success: false, error: 'Token is required'] as JSON)
            return
        }

        SharedWidget widget = SharedWidget.findByToken(token)
        if (!widget) {
            response.status = 404
            render([success: false, error: 'Share link not found'] as JSON)
            return
        }

        String effectiveState = resolveEffectiveState(widget)
        if (effectiveState != SharedWidgetState.VALID.name() && effectiveState != 'NOT_YET_ACTIVE') {
            response.status = 403
            render([success: false, error: "Share link is ${effectiveState.toLowerCase()}"] as JSON)
            return
        }

        def body = request.JSON
        String providedPin = body?.pin ?: ''
        
        if (widget.pin == null || widget.pin.isEmpty()) {
            render([success: true] as JSON)
            return
        }

        if (providedPin != widget.pin) {
            response.status = 403
            render([success: false, error: 'Invalid PIN'] as JSON)
            return
        }

        render([success: true] as JSON)
    }

    /**
     * POST /api/public/share/:token/action
     * Executes the widget action after validating token, state, dates, pin, and action count.
     */
    @Transactional
    def executeAction() {
        String token = params.token
        if (!token) {
            response.status = 400
            render([success: false, error: 'Token is required'] as JSON)
            return
        }

        SharedWidget widget = SharedWidget.findByToken(token)
        if (!widget) {
            response.status = 404
            render([success: false, error: 'Share link not found'] as JSON)
            return
        }

        String effectiveState = resolveEffectiveState(widget)
        if (effectiveState != SharedWidgetState.VALID.name()) {
            response.status = 403
            render([success: false, error: "Share link is ${effectiveState.toLowerCase()}"] as JSON)
            return
        }

        if (widget.pin != null && !widget.pin.isEmpty()) {
            def body = request.JSON
            String providedPin = body?.pin ?: ''
            if (providedPin != widget.pin) {
                response.status = 403
                render([success: false, error: 'Invalid PIN'] as JSON)
                return
            }
        }

        if (widget.widgetType == SharedWidgetType.GATE_ACCESS) {
            executeGateAccess(widget)
        } else {
            response.status = 400
            render([success: false, error: 'Unknown widget type'] as JSON)
            return
        }

        widget.actionsUsed = widget.actionsUsed + 1
        if (widget.actionsUsed >= widget.actionsAllowed) {
            widget.state = SharedWidgetState.EXPIRED
            widget.stateDescription = 'Action limit reached'
        }
        widget.save(flush: true, failOnError: true)

        render([
            success         : true,
            actionsRemaining: Math.max(0, widget.actionsAllowed - widget.actionsUsed),
        ] as JSON)
    }

    private void executeGateAccess(SharedWidget widget) {
        def eventData = [
            p0: 'evt_intercom_door_lock',
            p1: 'PERIPHERAL',
            p2: widget.peripheralId,
            p3: 'shared-link',
            p4: 'open',
            p5: "{'unlocked'}",
            p6: "shared:${widget.token}"
        ]
        publish(eventData.p0, eventData)
    }

    /**
     * Checks date range and action limits, auto-transitions state if needed.
     */
    private String resolveEffectiveState(SharedWidget widget) {
        if (widget.state == SharedWidgetState.DISABLED || widget.state == SharedWidgetState.ARCHIVED) {
            return widget.state.name()
        }

        Date now = new Date()
        if (widget.shareExpireDate != null && now.after(widget.shareExpireDate)) {
            if (widget.state == SharedWidgetState.VALID) {
                widget.state = SharedWidgetState.EXPIRED
                widget.stateDescription = 'Share link expired'
                widget.save(flush: true)
            }
            return SharedWidgetState.EXPIRED.name()
        }
        if (widget.shareStartDate != null && now.before(widget.shareStartDate)) {
            return 'NOT_YET_ACTIVE'
        }
        if (widget.actionsUsed >= widget.actionsAllowed) {
            if (widget.state == SharedWidgetState.VALID) {
                widget.state = SharedWidgetState.EXPIRED
                widget.stateDescription = 'Action limit reached'
                widget.save(flush: true)
            }
            return SharedWidgetState.EXPIRED.name()
        }

        return widget.state.name()
    }
}
