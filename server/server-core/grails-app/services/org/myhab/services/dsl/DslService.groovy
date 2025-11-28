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
 * <p>This service uses Groovy's sandbox mechanism to safely evaluate DSL scripts
 * by delegating method calls to ScenarioService. The sandbox provides protection
 * against potentially harmful operations.</p>
 * 
 * @see org.myhab.services.dsl.ScenarioService
 */
@Slf4j
@Transactional
class DslService {
  
  def scenarioService
  
  /**
   * Execute a scenario DSL script in a sandboxed environment.
   * 
   * <p>The script is evaluated with ScenarioService as its delegate, allowing
   * direct calls to DSL methods like switchOn, switchOff, isEvening, etc.</p>
   * 
   * <p>Example scenario script:</p>
   * <pre>
   * if (isEvening()) {
   *     switchOn([portIds: [123, 456]])
   * }
   * </pre>
   * 
   * @param scenario The scenario script as a String containing Groovy DSL code
   * @return The result of the script evaluation
   * @throws IllegalArgumentException if scenario is null or empty
   * @throws Exception if script evaluation fails
   */
  def execute(String scenario) {
    // Validate input
    if (!scenario?.trim()) {
      log.error("Cannot execute empty or null scenario")
      throw new IllegalArgumentException("Scenario script cannot be null or empty")
    }
    
    log.debug("Executing scenario DSL script (${scenario.length()} characters)")
    
    try {
      // Configure compiler with sandbox security
      def cc = new CompilerConfiguration()
      cc.addCompilationCustomizers(new SandboxTransformer())
      
      // Create binding with scenarioService pre-injected
      def binding = new Binding()
      binding.setVariable('scenarioService', scenarioService)
      
      // Create sandboxed shell
      def shell = new GroovyShell(this.class.classLoader, binding, cc)
      
      // Evaluate scenario as a closure and set its delegate to scenarioService
      // This allows direct method calls like switchOn() without prefixing scenarioService
      def result = shell.evaluate("""
        def dslClosure = {
          ${scenario}
        }
        dslClosure.delegate = scenarioService
        dslClosure.resolveStrategy = Closure.DELEGATE_FIRST
        return dslClosure()
      """)
      
      log.debug("Scenario executed successfully")
      return result
      
    } catch (Exception ex) {
      log.error("Failed to execute scenario DSL script: ${ex.message}", ex)
      throw new RuntimeException("Scenario execution failed: ${ex.message}", ex)
    }
  }
}
