package pravda.vm

package std

import pravda.vm.state.Memory

final case class Func(name: String, f: Memory => Memory) extends StdFunction {
  override def apply(v: Memory): Memory = f(v)
}
