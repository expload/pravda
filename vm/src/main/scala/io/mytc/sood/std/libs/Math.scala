package io.mytc.sood
package std.libs

import io.mytc.sood.vm.state.Memory
import vm.serialization._

object Math extends std.Lib {

  val sum: std.Func = new std.Func {
    override val name: String = "sum"
    override def apply(mem: Memory): Memory = {
      val n = dataToInt32(mem.pop())
      val sum = mem.stack.takeRight(n).map(dataToInt32).sum
      mem.stack.remove(mem.stack.length - n, n)
      mem.push(int32ToData(sum))
      mem
    }
  }


  override val address: String = "Math"
  override val functions: Seq[std.Func] = Array(
    sum
  )

}
