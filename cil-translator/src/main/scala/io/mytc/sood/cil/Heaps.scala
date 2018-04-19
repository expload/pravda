package io.mytc.sood.cil

import fastparse.byte.all._

object Heaps {
  def string(stringHeap: Bytes, idx: Long): Either[String, String] =
    utils.toEither(utils.nullTerminatedString.parse(stringHeap, idx.toInt))
}
