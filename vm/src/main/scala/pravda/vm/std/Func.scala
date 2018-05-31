package pravda.vm

package std

import pravda.vm.state.Memory
import pravda.vm.watt.WattCounter

object Func {
  def apply(name: String, f: Memory => Unit, count: WattCounter => Unit = _ => ()): StdFunction =
    (mem: Memory, wc: WattCounter) => {
      count(wc)
      f(mem)
    }

  def apply(name: String, f: (Memory, WattCounter) => Unit): StdFunction =
    (mem: Memory, wc: WattCounter) => f(mem, wc)

}
