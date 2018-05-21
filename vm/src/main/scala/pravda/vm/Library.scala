package pravda.vm

import com.google.protobuf.ByteString

trait Library {

  def func(name: ByteString): Option[Function]

}
