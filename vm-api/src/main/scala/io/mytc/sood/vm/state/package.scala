package io.mytc.sood.vm

import scodec.bits.ByteVector

package object state {
  type Data = ByteVector // TODO: replace it with ByteString
  type Address = ByteVector // TODO: replace it with ByteString
}
