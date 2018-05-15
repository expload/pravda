package io.mytc.sood.vm.state

import java.nio.ByteBuffer

// TODO @fomkin: looks like it should be case class
trait Program {
  def storage: Storage
  def code: ByteBuffer
}
