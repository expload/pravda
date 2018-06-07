package pravda.vm

import java.nio.ByteBuffer

import pravda.vm.state.Memory
import pravda.vm.watt.WattCounter

trait Function

trait StdFunction extends ((Memory, WattCounter) => Unit) with Function

trait UserDefinedFunction extends Function {
  val code: ByteBuffer
}
