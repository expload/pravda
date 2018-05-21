package pravda.common

import com.google.protobuf.ByteString
import pravda.common.contrib.ed25519

object crypto {

  /**
    * Generates ed25519 key pair.
    * @param randomBytes64 base random 64 bytes
    * @return (pub[32], sec[64])
    */
  def ed25519KeyPair(randomBytes64: ByteString): (ByteString, ByteString) = {
    // FIXME use Address and PrivateKey tagged types
    val sec = randomBytes64.toByteArray
    val pub = new Array[Byte](32)
    ed25519.generateKey(pub, sec)
    (ByteString.copyFrom(pub), ByteString.copyFrom(sec))
  }
}
