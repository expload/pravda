package pravda.vm

package std.libs

import serialization._

object Math extends std.NativeLibrary {

  import pravda.vm.watt.WattCounter._

  private val sum = std.Func("sum", mem => {
      val sum = mem.all.map(dataToInt32).sum
      mem.clear()
      mem.push(int32ToData(sum))
    },
    _.cpuUsage(CPUSimpleArithmetic)
  )

  private val prod = std.Func("prod", mem => {
      val product = mem.all.map(dataToInt32).product
      mem.clear()
      mem.push(int32ToData(product))
    },
    _.cpuUsage(CPUArithmetic)
  )

  override val address: String = "Math"
  override val functions: Seq[pravda.vm.StdFunction] = Array(
    sum,
    prod
  )

}
