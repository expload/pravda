package pravda.vm

package std

import com.google.protobuf.ByteString

trait NativeLibrary extends Library {

  val address: String
  val functions: Seq[Func]

  private lazy val functionTable: Map[String, Func] = functions.map(f => f.name -> f).toMap

  def func(name: String): Option[Func] = functionTable.get(name)

  def func(name: ByteString): Option[Func] =
    functionTable.get(name.toStringUtf8)

}
