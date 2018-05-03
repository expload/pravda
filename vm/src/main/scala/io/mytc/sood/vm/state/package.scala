package io.mytc.sood.vm

import scodec.bits.ByteVector

package object state {
  type Data = ByteVector
  type Address = ByteVector
}
