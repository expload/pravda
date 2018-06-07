package pravda.vm

package std

import pravda.vm.state.Memory
import pravda.vm.watt.WattCounter

trait Func extends StdFunction {
  val name: String

}

object Func {

  def apply(funcName: String, f: Memory => Unit, count: WattCounter => Unit = _ => ()): Func = new Func {
    val name: String = funcName

    def apply(mem: Memory, wc: WattCounter): Unit = {
      count(wc)
      f(mem)
    }
  }

  def apply(funcName: String, f: (Memory, WattCounter) => Unit): Func = new Func {
    val name: String = funcName
    def apply(mem: Memory, wc: WattCounter): Unit = f(mem, wc)
  }
}
