package pravda.vm

package std

package libs

object Math extends std.NativeLibrary {

//  import pravda.vm.watt.WattCounter._
//
//  private val sum = std.Func("sum", mem => {
//    val sum = mem.all.map(dataToInt32).sum
//    mem.clear()
//    mem.push(int32ToData(sum))
//  }, _.cpuUsage(CpuSimpleArithmetic))
//
//  private val prod = std.Func("prod", mem => {
//    val product = mem.all.map(dataToInt32).product
//    mem.clear()
//    mem.push(int32ToData(product))
//  }, _.cpuUsage(CpuArithmetic))
//
//  override val address: String = "Math"
//  override val functions: Seq[Func] = Array(
//    sum,
//    prod
//  )
  val address: String = "Math"
  val functions: Seq[Func] = Seq()
}
