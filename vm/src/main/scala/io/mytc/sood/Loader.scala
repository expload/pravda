package io.mytc.sood

trait Loader {

  def lib(address: Array[Byte]): Option[Library]

}
