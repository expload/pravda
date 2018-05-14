package io.mytc.sood.vm

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.Memory

trait Function // FIXME sealed
trait StdFunction                                      extends (Memory => Memory) with Function
final case class UserDefinedFunction(code: ByteBuffer) extends Function
