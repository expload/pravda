package io.mytc.sood.vm.state

import java.nio.ByteBuffer

trait AccountState {
  def storage: Storage
  def program: ByteBuffer
}
