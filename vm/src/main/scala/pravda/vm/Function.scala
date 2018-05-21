package pravda.vm

import java.nio.ByteBuffer

import pravda.vm.state.Memory

trait Function // FIXME sealed
trait StdFunction                                      extends (Memory => Memory) with Function
final case class UserDefinedFunction(code: ByteBuffer) extends Function
