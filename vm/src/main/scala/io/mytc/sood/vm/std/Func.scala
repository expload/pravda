package io.mytc.sood.vm
package std

import io.mytc.sood.vm.state.Memory

case class Func(name: String, f: Memory => Memory) extends StdFunction {
  override def apply(v: Memory): Memory = f(v)
}

