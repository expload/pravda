package pravda.vm

import com.google.protobuf.ByteString

package object state {
  type Data = ByteString
  type Address = ByteString
}
