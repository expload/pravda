package pravda.vm

import java.nio.ByteBuffer

import pravda.vm.state.Memory

trait Function

trait StdFunction extends (Memory => Memory) with Function

trait UserDefinedFunction extends Function {
  val code: ByteBuffer
}
