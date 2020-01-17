package eu.devexpert.madhouse.services.dsl

import grails.gorm.transactions.Transactional
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer

@Transactional
class DslService {
  def execute(scenario) {

    def cc = new CompilerConfiguration()
    cc.addCompilationCustomizers(new SandboxTransformer())
    def binding = new Binding();

    def shell = new GroovyShell(this.class.classLoader, binding, cc)
    shell.evaluate("""
    import eu.devexpert.madhouse.services.dsl.ScenarioService
    import grails.util.Holders
    import org.springframework.context.ApplicationContext
    
    ApplicationContext ctx = Holders.grailsApplication.mainContext
    ScenarioService scenarioService = (ScenarioService) ctx.getBean("scenarioService");
    def dsl = """ + scenario + """
    dsl.delegate = scenarioService
    dsl()""")
  }
}
