package io.mytc.sood.vm.state

import scodec.bits.ByteVector

trait Storage {
  def get(key: Address): Option[ByteVector]
  def put(key: Address, value: ByteVector)
  def delete(key: Address): Unit
}
