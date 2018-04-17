package io.mytc.sood.vm.state

trait Storage {
  def get(key: Array[Byte]): Option[Array[Byte]]
  def put(key: Array[Byte], value: Array[Byte])
  def delete(key: Array[Byte]): Unit
}
