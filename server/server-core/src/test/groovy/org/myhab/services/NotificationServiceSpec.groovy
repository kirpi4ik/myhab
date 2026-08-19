package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.MessageLevel
import org.myhab.domain.MessageState
import org.myhab.domain.NotificationRule
import org.myhab.domain.Role
import org.myhab.domain.RuleMatchType
import org.myhab.domain.User
import org.myhab.domain.UserRole
import org.myhab.domain.UserMessage
import spock.lang.Specification

/**
 * Write-path contract: every row carries the producer's dedupKey, a matching mute rule files the
 * row directly and keeps it off the push channel, and the admin fan-out still reaches every admin.
 */
class NotificationServiceSpec extends Specification
        implements ServiceUnitTest<NotificationService>, DataTest {

    void setupSpec() {
        mockDomains(User, UserMessage, NotificationRule, Role, UserRole)
    }

    def setup() {
        service.webPushService = Mock(WebPushService)
        service.notificationRuleService = Mock(NotificationRuleService)
        // No Hazelcast in unit tests: cooldown lookups fail open, marks are no-ops.
        service.hazelcastInstance = null
    }

    private User user(String name) {
        new User(username: name, password: 'x').save(flush: true, failOnError: true)
    }

    void "an unmuted message is stored NEW and pushed"() {
        given:
            User u = user('alice')

        when:
            UserMessage um = service.notify(u, MessageLevel.INFO, 'Gate opened', 'Card 1', 'gate_controller')

        then:
            1 * service.notificationRuleService.firstMatching(u, 'gate_controller', 'Gate opened', null) >> null
            1 * service.webPushService.sendToUser(u, _)
            um != null
            um.state == MessageState.NEW
    }

    void "a muted message is stored in the rule's target state and never pushed"() {
        given:
            User u = user('alice')
            NotificationRule r = new NotificationRule(user: u, matchType: RuleMatchType.SENDER,
                    pattern: 'navimow', targetState: MessageState.ARCHIVE).save(flush: true, failOnError: true)

        when:
            UserMessage um = service.notify(u, MessageLevel.INFO, 'Navimow M1: docked → mowing',
                    'body', 'navimow', 'navimow.5.state.mowing', 5)

        then:
            1 * service.notificationRuleService.firstMatching(_, _, _, _) >> r
            0 * service.webPushService.sendToUser(_, _)
            // Muted is not suppressed — the row exists, it just never reaches the inbox.
            um != null
            um.state == MessageState.ARCHIVE
    }

    void "the producer's dedupKey is persisted on the row"() {
        given:
            User u = user('alice')

        when:
            service.notify(u, MessageLevel.INFO, 's', 'b', 'navimow', 'navimow.5.low_battery', 5)

        then:
            1 * service.notificationRuleService.firstMatching(_, _, _, _) >> null
            UserMessage.findByUser(u).dedupKey == 'navimow.5.low_battery'
    }

    void "a keyless message stores SQL NULL, not an empty string"() {
        given:
            User u = user('alice')

        when:
            service.notify(u, MessageLevel.INFO, 's', 'b', 'system')

        then:
            1 * service.notificationRuleService.firstMatching(_, _, _, _) >> null
            UserMessage.findByUser(u).dedupKey == null
    }

    void "notifyAdmins reaches every admin and stamps the key on each row"() {
        given:
            Role admin = new Role(authority: 'ROLE_ADMIN').save(flush: true, failOnError: true)
            User alice = user('alice')
            User bob = user('bob')
            new UserRole(user: alice, role: admin).save(flush: true, failOnError: true)
            new UserRole(user: bob, role: admin).save(flush: true, failOnError: true)

        when:
            int delivered = service.notifyAdmins(MessageLevel.WARN, 'Navimow M1: low battery (17%)',
                    'body', 'navimow', 'navimow.5.low_battery', 360)

        then:
            // Regression: the fan-out must not go through notify(), whose markFired would
            // silence every admin after the first — while the key must still reach every row.
            2 * service.notificationRuleService.firstMatching(_, _, _, _) >> null
            delivered == 2
            UserMessage.count() == 2
            UserMessage.list().every { it.dedupKey == 'navimow.5.low_battery' }
            UserMessage.list().every { it.state == MessageState.NEW }
    }

    void "notifyAdmins applies each admin's own rules independently"() {
        given:
            Role admin = new Role(authority: 'ROLE_ADMIN').save(flush: true, failOnError: true)
            User alice = user('alice')
            User bob = user('bob')
            new UserRole(user: alice, role: admin).save(flush: true, failOnError: true)
            new UserRole(user: bob, role: admin).save(flush: true, failOnError: true)
            NotificationRule aliceRule = new NotificationRule(user: alice, matchType: RuleMatchType.SENDER,
                    pattern: 'navimow', targetState: MessageState.READ).save(flush: true, failOnError: true)

        when:
            service.notifyAdmins(MessageLevel.INFO, 'subject', 'body', 'navimow')

        then:
            1 * service.notificationRuleService.firstMatching(alice, _, _, _) >> aliceRule
            1 * service.notificationRuleService.firstMatching(bob, _, _, _) >> null
            UserMessage.findByUser(alice).state == MessageState.READ
            UserMessage.findByUser(bob).state == MessageState.NEW
    }

    void "a null user writes nothing"() {
        expect:
            service.notify(null, MessageLevel.INFO, 's', 'b') == null
            UserMessage.count() == 0
    }
}
