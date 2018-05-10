package io.mytc.sood.vm

import scodec.bits.ByteVector

trait Library {

  def func(name: ByteVector): Option[Function]

}
