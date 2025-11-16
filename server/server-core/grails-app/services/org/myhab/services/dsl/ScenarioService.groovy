package org.myhab.services.dsl

import org.myhab.domain.device.port.PortAction
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.services.dsl.action.DslCommand
import org.springframework.context.ApplicationContext
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * ScenarioService provides DSL methods for scenario execution in MyHAB automation system.
 * 
 * This service acts as the primary interface for scenario scripts, providing utility methods
 * for device control, timing, and conditional logic based on time of day.
 * 
 * <p>Available DSL methods:</p>
 * <ul>
 *   <li><b>isEvening()</b> - Check if current time is evening (after 18:00 UTC+2)</li>
 *   <li><b>switchOn(args)</b> - Turn device ports ON</li>
 *   <li><b>switchOff(args)</b> - Turn device ports OFF</li>
 *   <li><b>switchToggle(args)</b> - Toggle device port state</li>
 *   <li><b>pause(milliseconds)</b> - Pause scenario execution</li>
 * </ul>
 * 
 * <p>Example usage in scenario script:</p>
 * <pre>
 * if (isEvening()) {
 *     // Control ports directly by ID
 *     switchOn([portIds: [123, 456]])
 *     pause(1000)
 *     
 *     // Or control via peripherals
 *     switchOn([peripheralIds: [10, 11, 12]])
 *     
 *     log.info("Evening lights activated")
 * } else {
 *     switchOff([portIds: [123, 456]])
 * }
 * </pre>
 * 
 * @author MyHAB Team
 * @since 1.0
 */
@Slf4j
@Transactional
class ScenarioService {

    def powerService

    /**
     * Check if current time in UTC+2 timezone is evening (after 18:00).
     * 
     * <p>This method is useful for creating time-based automation rules,
     * such as turning on lights in the evening or adjusting heating schedules.</p>
     * 
     * <p>Evening is defined as any time at or after 18:00 (6:00 PM) UTC+2.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * if (isEvening()) {
     *     switchOn([portIds: [123, 456]])
     * }
     * </pre>
     * 
     * @return true if current time is at or after 18:00 UTC+2, false otherwise
     */
    def isEvening() {
        ZoneId utcPlus2 = ZoneId.of("UTC+2")
        ZonedDateTime now = ZonedDateTime.now(utcPlus2)
        LocalTime currentTime = now.toLocalTime()
        LocalTime eveningStart = LocalTime.of(18, 0) // 6:00 PM
        
        boolean result = currentTime.isAfter(eveningStart) || currentTime.equals(eveningStart)
        log.debug("isEvening check: current time in UTC+2 is ${currentTime}, evening starts at ${eveningStart}, result: ${result}")
        
        return result
    }

    /**
     * Turn device port(s) ON.
     * 
     * <p>This method supports two approaches for port selection:</p>
     * <ul>
     *   <li><b>Direct port selection</b> - Specify ports by their database IDs</li>
     *   <li><b>Peripheral-based selection</b> - Specify peripherals, uses their connectedTo ports</li>
     * </ul>
     * 
     * <p>Both approaches can be combined in a single call. The action is executed
     * via PowerService which publishes the command to the MQTT broker.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * // Turn on specific ports by ID
     * switchOn([portIds: [123, 456]])
     * 
     * // Turn on ports via peripheral IDs
     * switchOn([peripheralIds: [10, 11, 12]])
     * 
     * // Combine both approaches
     * switchOn([portIds: [123], peripheralIds: [10, 11]])
     * </pre>
     * 
     * @param args Map containing port selection criteria. Supported keys:
     *             <ul>
     *               <li><b>portIds</b> - Optional list of device port IDs to turn on</li>
     *               <li><b>peripheralIds</b> - Optional list of peripheral IDs (uses their connected ports)</li>
     *             </ul>
     *             At least one of portIds or peripheralIds must be provided.
     * @see org.myhab.services.dsl.action.PowerService#execute(java.util.Map)
     */
    def switchOn(args) {
        args.action = PortAction.ON
        powerService.execute(args)
    }

    /**
     * Turn device port(s) OFF.
     * 
     * <p>This method supports two approaches for port selection:</p>
     * <ul>
     *   <li><b>Direct port selection</b> - Specify ports by their database IDs</li>
     *   <li><b>Peripheral-based selection</b> - Specify peripherals, uses their connectedTo ports</li>
     * </ul>
     * 
     * <p>Both approaches can be combined in a single call. The action is executed
     * via PowerService which publishes the command to the MQTT broker.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * // Turn off specific ports by ID
     * switchOff([portIds: [123, 456]])
     * 
     * // Turn off ports via peripheral IDs
     * switchOff([peripheralIds: [10, 11, 12]])
     * 
     * // Combine both approaches
     * switchOff([portIds: [123], peripheralIds: [10, 11]])
     * </pre>
     * 
     * @param args Map containing port selection criteria. Supported keys:
     *             <ul>
     *               <li><b>portIds</b> - Optional list of device port IDs to turn off</li>
     *               <li><b>peripheralIds</b> - Optional list of peripheral IDs (uses their connected ports)</li>
     *             </ul>
     *             At least one of portIds or peripheralIds must be provided.
     * @see org.myhab.services.dsl.action.PowerService#execute(java.util.Map)
     */
    def switchOff(args) {
        args.action = PortAction.OFF
        powerService.execute(args)
    }

    /**
     * Toggle device port(s) state (ON becomes OFF, OFF becomes ON).
     * 
     * <p>This method supports two approaches for port selection:</p>
     * <ul>
     *   <li><b>Direct port selection</b> - Specify ports by their database IDs</li>
     *   <li><b>Peripheral-based selection</b> - Specify peripherals, uses their connectedTo ports</li>
     * </ul>
     * 
     * <p>Both approaches can be combined in a single call. The action is executed
     * via PowerService which publishes the command to the MQTT broker.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * // Toggle specific ports by ID
     * switchToggle([portIds: [123, 456]])
     * 
     * // Toggle ports via peripheral IDs
     * switchToggle([peripheralIds: [10, 11, 12]])
     * 
     * // Combine both approaches
     * switchToggle([portIds: [123], peripheralIds: [10, 11]])
     * </pre>
     * 
     * @param args Map containing port selection criteria. Supported keys:
     *             <ul>
     *               <li><b>portIds</b> - Optional list of device port IDs to toggle</li>
     *               <li><b>peripheralIds</b> - Optional list of peripheral IDs (uses their connected ports)</li>
     *             </ul>
     *             At least one of portIds or peripheralIds must be provided.
     * @see org.myhab.services.dsl.action.PowerService#execute(java.util.Map)
     */
    def switchToggle(args) {
        args.action = PortAction.TOGGLE
        powerService.execute(args)
    }

    /**
     * Pause scenario execution for specified duration.
     * 
     * <p>This method blocks the current thread for the specified number of milliseconds.
     * Useful for creating delays between device actions or waiting for devices to respond.</p>
     * 
     * <p><b>Warning:</b> Use with caution in production scenarios as it blocks the thread.
     * Consider using scheduled jobs for long-running delays instead.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * switchOn([portIds: [123]])
     * pause(1000) // Wait 1 second
     * switchOff([portIds: [123]])
     * </pre>
     * 
     * @param miliseconds Duration to pause in milliseconds (1000ms = 1 second)
     */
    def pause(miliseconds) {        
        Thread.sleep(miliseconds)
    }

    /**
     * Handle dynamic method invocation for DSL commands.
     * 
     * <p>This method is invoked when a method is called that doesn't exist on this service.
     * It attempts to find a Spring bean with the name "${methodName}Service" that implements
     * DslCommand interface and delegates the call to it.</p>
     * 
     * <p>This enables extensible DSL functionality by allowing additional command services
     * to be added as Spring beans without modifying this core service.</p>
     * 
     * <p>Example:</p>
     * <pre>
     * // If 'notificationService' bean exists and implements DslCommand:
     * notification([message: "Hello", recipient: "user@example.com"])
     * </pre>
     * 
     * @param methodName Name of the method being called
     * @param args Arguments passed to the method
     * @return The DslCommand bean that handled the execution
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no matching service bean is found
     */
    def methodMissing(String methodName, args) {
        ApplicationContext ctx = Holders.grailsApplication.mainContext
        DslCommand dslCommand = (DslCommand) ctx.getBean("${methodName}Service");
        dslCommand.execute(args);

        log.debug("missing method : ${methodName}")
        return dslCommand
    }

/*  
    /**
     * Handle dynamic property access for DSL.
     * 
     * <p>Currently commented out. This method would be invoked when accessing
     * a property that doesn't exist on this service.</p>
     * 
     * @param paramName Name of the property being accessed
     * @return Dynamic property value
     */
/*  def propertyMissing(String paramName) {
    log.debug("missing property: ${paramName}")
    return new PowerOut()
  }*/

}
