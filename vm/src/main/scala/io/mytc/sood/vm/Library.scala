package io.mytc.sood.vm

import com.google.protobuf.ByteString

trait Library {

  def func(name: ByteString): Option[Function]

}
