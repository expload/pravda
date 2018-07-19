package pravda.vm.standard

import pravda.vm.{Data, Memory, WattCounter}

trait FunctionDefinition {

  def id: Long

  def name: String = this.getClass.getSimpleName.stripSuffix("$")

  def description: String

  def args: Seq[(String, Seq[Data.Type])]

  def returns: Seq[Data.Type]

  def apply(memory: Memory, wattCounter: WattCounter): Unit
}
