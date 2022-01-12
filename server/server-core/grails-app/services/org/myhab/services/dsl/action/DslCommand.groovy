package org.myhab.services.dsl.action

/**
 * All DSL methods/commands must implements this interface
 */
interface DslCommand {
  def execute(args)

}