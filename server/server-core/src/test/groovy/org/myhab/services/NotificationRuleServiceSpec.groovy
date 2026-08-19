package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.MessageLevel
import org.myhab.domain.MessageState
import org.myhab.domain.NotificationRule
import org.myhab.domain.RuleMatchType
import org.myhab.domain.User
import org.myhab.domain.UserMessage
import spock.lang.Specification

/**
 * Matching contract for per-user mute rules. The same matcher drives the live write path and
 * the retroactive sweep, so "muted from now on" and "muted retroactively" mean the same thing.
 */
class NotificationRuleServiceSpec extends Specification
        implements ServiceUnitTest<NotificationRuleService>, DataTest {

    // NotificationRuleService is @Transactional; DataTest supplies the transaction manager.
    void setupSpec() {
        mockDomains(User, UserMessage, NotificationRule)
    }

    private User user(String name) {
        new User(username: name, password: 'x').save(flush: true, failOnError: true)
    }

    private NotificationRule rule(User u, RuleMatchType type, String pattern,
                                  MessageState target = MessageState.ARCHIVE) {
        new NotificationRule(user: u, matchType: type, pattern: pattern, targetState: target)
                .save(flush: true, failOnError: true)
    }

    private UserMessage msg(User u, Map args) {
        new UserMessage([
                subject   : 'subject',
                fromSender: 'navimow',
                message   : 'body',
                level     : MessageLevel.INFO,
                state     : MessageState.NEW,
                user      : u
        ] + args).save(flush: true, failOnError: true)
    }

    void "SENDER matches an exact sender and nothing else"() {
        given:
            User u = user('alice')
            NotificationRule r = rule(u, RuleMatchType.SENDER, 'navimow')

        expect:
            service.matches(r, 'navimow', 'anything', null)
            !service.matches(r, 'navimow2', 'anything', null)
            !service.matches(r, 'gate_controller', 'anything', null)
    }

    void "KEY_PREFIX mutes a family without catching its siblings"() {
        given:
            User u = user('alice')
            NotificationRule r = rule(u, RuleMatchType.KEY_PREFIX, 'navimow.5.state')

        expect:
            service.matches(r, 'navimow', 's', 'navimow.5.state.mowing')
            service.matches(r, 'navimow', 's', 'navimow.5.state.docked')
            // errors and battery alerts must still come through
            !service.matches(r, 'navimow', 's', 'navimow.5.low_battery')
            !service.matches(r, 'navimow', 's', 'navimow.5.error.512')
            // a different mower is a different key
            !service.matches(r, 'navimow', 's', 'navimow.9.state.mowing')
    }

    void "KEY_PREFIX never matches a message the producer gave no key"() {
        given:
            User u = user('alice')
            NotificationRule r = rule(u, RuleMatchType.KEY_PREFIX, 'navimow.5.state')

        when:
            boolean matched = service.matches(r, 'navimow', 'subject', null)

        then:
            !matched
            noExceptionThrown()
    }

    void "SUBJECT_REGEX matches on the subject"() {
        given:
            User u = user('alice')
            NotificationRule r = rule(u, RuleMatchType.SUBJECT_REGEX, /low battery \(\d+%\)/)

        expect:
            service.matches(r, 'navimow', 'Navimow M1: low battery (17%)', null)
            !service.matches(r, 'navimow', 'Navimow M1: error 512', null)
    }

    void "an unparseable regex never matches and never throws"() {
        given:
            User u = user('alice')
            NotificationRule r = rule(u, RuleMatchType.SUBJECT_REGEX, '[unclosed')

        when:
            boolean matched = service.matches(r, 'navimow', 'anything at all', null)

        then:
            !matched
            noExceptionThrown()
    }

    void "firstMatching only ever consults the recipient's own rules"() {
        given:
            User alice = user('alice')
            User bob = user('bob')
            rule(bob, RuleMatchType.SENDER, 'navimow')

        expect:
            service.firstMatching(alice, 'navimow', 'subject', null) == null
            service.firstMatching(bob, 'navimow', 'subject', null) != null
    }

    void "firstMatching returns null when nothing matches"() {
        expect:
            service.firstMatching(user('alice'), 'navimow', 'subject', 'navimow.5.state.mowing') == null
    }

    void "applyToExisting files matching NEW messages and reports the count"() {
        given:
            User alice = user('alice')
            User bob = user('bob')
            msg(alice, [dedupKey: 'navimow.5.state.mowing'])
            msg(alice, [dedupKey: 'navimow.5.state.docked'])
            msg(alice, [dedupKey: 'navimow.5.low_battery'])                       // different family
            msg(alice, [dedupKey: 'navimow.5.state.paused', state: MessageState.READ])  // already filed
            msg(bob, [dedupKey: 'navimow.5.state.mowing'])                        // another user
            NotificationRule r = rule(alice, RuleMatchType.KEY_PREFIX, 'navimow.5.state')

        when:
            int applied = service.applyToExisting(alice, r)

        then:
            applied == 2
            UserMessage.countByUserAndState(alice, MessageState.ARCHIVE) == 2
            // untouched: the sibling family, the already-read one, and bob's copy
            UserMessage.countByUserAndState(alice, MessageState.NEW) == 1
            UserMessage.countByUserAndState(alice, MessageState.READ) == 1
            UserMessage.countByUserAndState(bob, MessageState.NEW) == 1
    }

    void "applyToExisting honours the rule's target state"() {
        given:
            User alice = user('alice')
            msg(alice, [fromSender: 'gate_controller'])
            NotificationRule r = rule(alice, RuleMatchType.SENDER, 'gate_controller', MessageState.READ)

        when:
            int applied = service.applyToExisting(alice, r)

        then:
            applied == 1
            UserMessage.countByUserAndState(alice, MessageState.READ) == 1
    }
}
