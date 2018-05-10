package io.mytc.sood.vm

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.Memory

sealed trait Function
trait StdFunction extends (Memory => Memory) with Function
case class UserDefinedFunction(code: ByteBuffer) extends Function
