package io.mytc.sood.vm
package std.libs

import serialization._

object Math extends std.Lib {

  private val sum = std.Func("sum", mem => {
    val sum = mem.stack.map(dataToInt32).sum
    mem.stack.clear()
    mem.push(int32ToData(sum))
    mem
  })

  private val prod = std.Func("prod", mem => {
    val product = mem.stack.map(dataToInt32).product
    mem.stack.clear()
    mem.push(int32ToData(product))
    mem
  })

  override val address: String = "Math"
  override val functions: Seq[io.mytc.sood.vm.std.Func] = Array(
    sum,
    prod
  )

}
