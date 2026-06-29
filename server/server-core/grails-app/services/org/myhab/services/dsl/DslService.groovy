package org.myhab.services.dsl

import grails.gorm.transactions.Transactional
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext

/**
 * DslService provides secure dynamic execution of scenario DSL scripts.
 *
 * <p>The script body is evaluated as a Groovy closure with a
 * {@link CompositeDelegate} as its delegate (DELEGATE_FIRST). The composite
 * resolves method calls in order across:</p>
 * <ol>
 *   <li>{@code knowledgeService} — read-only predicates (isRaining, isDay, isNight,
 *       currentExternTemperature, …).</li>
 *   <li>{@code scenarioService} — actions (switchOn / switchOff / switchToggle /
 *       pause) and the legacy {@code isEvening()} predicate. Its
 *       {@code methodMissing} is the final fallback and resolves
 *       {@code ${name}Service} Spring beans implementing
 *       {@link org.myhab.services.dsl.action.DslCommand}, so the existing
 *       action-plugin mechanism keeps working unchanged.</li>
 * </ol>
 *
 * <p>The Groovy sandbox compiler customizer is installed; no
 * {@code GroovyInterceptor} is registered, so no method-call whitelist is
 * enforced (consistent with the prior behaviour).</p>
 *
 * @see org.myhab.services.dsl.ScenarioService
 * @see org.myhab.services.dsl.knowledge.KnowledgeService
 * @see org.myhab.services.dsl.CompositeDelegate
 */
@Slf4j
@Transactional
class DslService {

  def scenarioService
  def knowledgeService

  /**
   * Execute a scenario DSL script in a sandboxed environment.
   *
   * <p>The script body is wrapped in a closure whose delegate is a
   * {@link CompositeDelegate} fanning method calls across {@code knowledgeService}
   * (predicates) and {@code scenarioService} (actions + plugin fallback).</p>
   *
   * <p>Example scenario script:</p>
   * <pre>
   * if (isEvening() &amp;&amp; isNight()) {
   *     switchOn([portIds: [123, 456]])
   * }
   * if (isRaining()) {
   *     switchOff([peripheralIds: [42]])
   * }
   * </pre>
   *
   * @param scenario The scenario script as a String containing Groovy DSL code
   * @param actor    Trigger origin recorded as the audit actor for any state
   *                 changes the script performs: {@code CRON} (scheduled),
   *                 {@code EVENT} (device/event-driven), or the real username
   *                 for a manual run. Defaults to {@code SYSTEM}.
   * @return The result of the script evaluation
   * @throws IllegalArgumentException if scenario is null or empty
   * @throws Exception if script evaluation fails
   */
  def execute(String scenario, String actor = 'SYSTEM') {
    // Validate input
    if (!scenario?.trim()) {
      log.error("Cannot execute empty or null scenario")
      throw new IllegalArgumentException("Scenario script cannot be null or empty")
    }

    log.debug("Executing scenario DSL script (${scenario.length()} characters), actor: ${actor}")

    // Bind the audit actor for the duration of this synchronous execution so
    // switchOn/Off/Toggle attribute their state changes correctly.
    scenarioService.setExecutionActor(actor)
    try {
      // Configure compiler with sandbox security
      def cc = new CompilerConfiguration()
      cc.addCompilationCustomizers(new SandboxTransformer())

      // Build the composite delegate: knowledges first, scenario actions last.
      // Order matters: ScenarioService must be last so its methodMissing fires
      // for unknown method names (resolves ${name}Service beans / DslCommand).
      def composite = new CompositeDelegate(delegates: [knowledgeService, scenarioService])

      // Create binding with services pre-injected (script may also call them
      // explicitly, e.g. for tests or namespaced usage)
      def binding = new Binding()
      binding.setVariable('scenarioService', scenarioService)
      binding.setVariable('knowledgeService', knowledgeService)
      binding.setVariable('composite', composite)

      // Create sandboxed shell
      def shell = new GroovyShell(this.class.classLoader, binding, cc)

      // Evaluate scenario as a closure and set its delegate to the composite.
      // DELEGATE_FIRST means bare names (switchOn, isRaining, ...) resolve
      // through the composite before the script's own scope.
      def result = shell.evaluate("""
        def dslClosure = {
          ${scenario}
        }
        dslClosure.delegate = composite
        dslClosure.resolveStrategy = Closure.DELEGATE_FIRST
        return dslClosure()
      """)

      log.debug("Scenario executed successfully")
      return result

    } catch (Exception ex) {
      log.error("Failed to execute scenario DSL script: ${ex.message}", ex)
      throw new RuntimeException("Scenario execution failed: ${ex.message}", ex)
    } finally {
      scenarioService.clearExecutionActor()
    }
  }
}
