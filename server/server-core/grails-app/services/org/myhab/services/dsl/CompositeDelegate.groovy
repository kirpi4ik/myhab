package org.myhab.services.dsl

import groovy.util.logging.Slf4j

/**
 * Fans method calls across an ordered list of delegate services.
 * Used by {@link DslService} as the closure delegate so the scenario DSL stays
 * flat (no namespace prefix) while methods can come from multiple specialized
 * services (knowledges, actions, future categories).
 *
 * <p>Resolution rule: walk delegates in order, invoke on the first that
 * {@code respondsTo(name, args)}. If no delegate has a named method, fall
 * through to the last non-null delegate's {@code invokeMethod} so its
 * {@code methodMissing} is consulted (this preserves
 * {@link org.myhab.services.dsl.ScenarioService}'s action-plugin lookup that
 * resolves bean {@code ${name}Service} implementing
 * {@link org.myhab.services.dsl.action.DslCommand}).</p>
 *
 * <p>Wiring convention: services that contribute named methods come first;
 * the service that owns the {@code methodMissing} fallback comes last.</p>
 */
@Slf4j
class CompositeDelegate {
    List delegates = []

    def methodMissing(String name, args) {
        for (d in delegates) {
            if (d == null) continue
            if (d.metaClass.respondsTo(d, name, args).size() > 0) {
                return d.metaClass.invokeMethod(d, name, args)
            }
        }
        def last = delegates?.findAll { it != null }?.last()
        if (last == null) {
            throw new MissingMethodException(name, this.class, args as Object[])
        }
        return last.invokeMethod(name, args)
    }
}
