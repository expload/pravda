package io.mytc.sood

import java.nio.ByteBuffer

import io.mytc.sood.vm.state.Memory

sealed trait Function
trait StdFunction extends ((Memory) => Memory) with Function
case class LibFunction(code: ByteBuffer) extends Function
