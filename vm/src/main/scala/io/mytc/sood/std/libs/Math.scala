package io.mytc.sood
package std.libs

import io.mytc.sood.vm.state.Memory
import vm.serialization._

object Math extends std.Lib {

  val sum: std.Func = new std.Func {
    override val name: String = "sum"
    override def apply(mem: Memory): Memory = {
      val sum = mem.stack.map(dataToInt32).sum
      mem.stack.clear()
      mem.push(int32ToData(sum))
      mem
    }
  }

  val prod: std.Func = new std.Func {
    override val name: String = "prod"
    override def apply(mem: Memory): Memory = {
      val product = mem.stack.map(dataToInt32).product
      mem.stack.clear()
      mem.push(int32ToData(product))
      mem
    }
  }


  override val address: String = "Math"
  override val functions: Seq[std.Func] = Array(
    sum,
    prod
  )

}
