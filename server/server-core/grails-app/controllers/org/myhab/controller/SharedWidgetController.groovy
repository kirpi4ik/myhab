package org.myhab.controller

import grails.converters.JSON
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.ConfigKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetAudit
import org.myhab.domain.SharedWidgetAuditResult
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.device.DevicePeripheral

@Slf4j
@Secured(['permitAll'])
class SharedWidgetController implements EventPublisher {

    static responseFormats = ['json']
    static allowedMethods = [show: 'GET', executeAction: 'POST', verifyPin: 'POST']

    /** Actions a switch-type share link accepts. */
    private static final List<String> SWITCH_ACTIONS = ['on', 'off']

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
        Boolean currentState = null
        String autoOffTimeout = null
        try {
            def peripheral = DevicePeripheral.get(widget.peripheralId)
            peripheralName = peripheral?.name ?: ''
            if (peripheral && widget.widgetType.isSwitch()) {
                currentState = peripheral.connectedTo?.find()?.value == 'ON'
                autoOffTimeout = Configuration.findByEntityIdAndEntityTypeAndKey(
                        peripheral.id, EntityType.PERIPHERAL, ConfigKey.STATE_ON_TIMEOUT)?.value
            }
        } catch (ignored) {}

        render([
            widgetType     : widget.widgetType.name(),
            requiresPin    : widget.pin != null && !widget.pin.isEmpty(),
            state          : effectiveState,
            peripheralName : peripheralName,
            actionsAllowed : widget.actionsAllowed,
            actionsUsed    : widget.actionsUsed,
            shareExpireDate: widget.shareExpireDate?.time,
            currentState   : currentState,
            autoOffTimeout : autoOffTimeout,
            offAllowed     : isOffAllowed(widget),
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
            // A spent switch link must still let the guest in to press Stop.
            if (!isOffAllowed(widget)) {
                recordAudit(widget, 'verify-pin', SharedWidgetAuditResult.DENIED_STATE, effectiveState)
                response.status = 403
                render([success: false, error: "Share link is ${effectiveState.toLowerCase()}"] as JSON)
                return
            }
        }

        def body = request.JSON
        String providedPin = body?.pin ?: ''

        if (widget.pin == null || widget.pin.isEmpty()) {
            render([success: true] as JSON)
            return
        }

        if (providedPin != widget.pin) {
            recordAudit(widget, 'verify-pin', SharedWidgetAuditResult.DENIED_PIN)
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

        // Read once up front (the action lives here too now), tolerating a bodyless POST —
        // a non-PIN gate link never needed a body before.
        def body = null
        try {
            body = request.JSON
        } catch (ignored) {
        }

        String action = 'open'
        if (widget.widgetType.isSwitch()) {
            action = body?.action?.toString()?.toLowerCase()
            if (!SWITCH_ACTIONS.contains(action)) {
                response.status = 400
                render([success: false, error: "Action must be one of ${SWITCH_ACTIONS.join(', ')}"] as JSON)
                return
            }
        }

        String effectiveState = resolveEffectiveState(widget)
        // Turning a switch off is always permitted while the link has not been explicitly
        // killed: only "on" consumes an allowance, so a guest whose allowance is spent (or
        // whose link has run past its end date) must still be able to stop the device.
        boolean stateOk = effectiveState == SharedWidgetState.VALID.name() ||
                (action == 'off' && isOffAllowed(widget))
        if (!stateOk) {
            recordAudit(widget, action, SharedWidgetAuditResult.DENIED_STATE, effectiveState)
            response.status = 403
            render([success: false, error: "Share link is ${effectiveState.toLowerCase()}"] as JSON)
            return
        }

        if (widget.pin != null && !widget.pin.isEmpty()) {
            String providedPin = body?.pin ?: ''
            if (providedPin != widget.pin) {
                recordAudit(widget, action, SharedWidgetAuditResult.DENIED_PIN)
                response.status = 403
                render([success: false, error: 'Invalid PIN'] as JSON)
                return
            }
        }

        if (widget.widgetType.isSwitch()) {
            executeSwitch(widget, action)
        } else {
            executeGateAccess(widget)
        }

        if (action != 'off') {
            widget.actionsUsed = widget.actionsUsed + 1
            if (widget.actionsUsed >= widget.actionsAllowed) {
                widget.state = SharedWidgetState.EXPIRED
                widget.stateDescription = 'Action limit reached'
            }
            widget.save(flush: true, failOnError: true)
        }

        recordAudit(widget, action, SharedWidgetAuditResult.SUCCESS)

        render([
            success         : true,
            action          : action,
            actionsRemaining: Math.max(0, widget.actionsAllowed - widget.actionsUsed),
            offAllowed      : isOffAllowed(widget),
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

    private void executeSwitch(SharedWidget widget, String action) {
        def eventData = [
            p0: widget.widgetType.topic,
            p1: 'PERIPHERAL',
            p2: widget.peripheralId,
            p3: 'shared-link',
            p4: action,
            p6: "shared:${widget.token}"
        ]
        publish(eventData.p0, eventData)
    }

    /**
     * Whether a switch link may still be turned off. Only an explicit admin kill
     * (DISABLED/ARCHIVED) takes the stop control away.
     */
    private boolean isOffAllowed(SharedWidget widget) {
        return widget.widgetType.isSwitch() &&
                widget.state != SharedWidgetState.DISABLED &&
                widget.state != SharedWidgetState.ARCHIVED
    }

    /**
     * Records the attempt. Own transaction so the row commits independently of the caller
     * (verifyPin is not transactional), mirroring AuditService. A failed audit write must
     * never break the action itself.
     */
    private void recordAudit(SharedWidget widget, String action, SharedWidgetAuditResult result,
                             String reason = null) {
        try {
            SharedWidgetAudit.withNewTransaction {
                new SharedWidgetAudit(
                        sharedWidget: widget,
                        action: action,
                        result: result,
                        resultDescription: reason,
                        remoteAddress: clientIp()?.take(64),
                        userAgent: request.getHeader('User-Agent')?.take(512)
                ).save(flush: true, failOnError: true)
            }
        } catch (Exception e) {
            log.error("Failed to write shared-widget audit row for ${widget?.id}: ${e.message}", e)
        }
    }

    /** nginx fronts :8181 in production, so the socket peer is the proxy. */
    private String clientIp() {
        String forwarded = request.getHeader('X-Forwarded-For')
        return forwarded ? forwarded.split(',')[0].trim() : request.remoteAddr
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
