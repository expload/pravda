package pravda.vm.udf

import java.nio.ByteBuffer

import pravda.vm.UserDefinedFunction

final case class Func(code: ByteBuffer) extends UserDefinedFunction
