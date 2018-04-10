package io.mytc.sood.forth

import org.scalatest._


class ForthTest extends FlatSpec with Matchers {

  import io.mytc.sood.vm.Vm
  import io.mytc.sood.forth.Compiler
  import java.nio.ByteBuffer
  import scala.collection.mutable.ArrayBuffer

  "A forth compiler" must "correctly compile code" in {
    val compiler = Compiler()
    val res = compiler.compile( """
      : seq5 1 2 3 4 5 ;
    """ )

    res match {
      case Left(err)   ⇒ throw new RuntimeException(err)
      case Right(code) ⇒ Vm.run(ByteBuffer.wrap(code), Option.empty[ArrayBuffer[Array[Byte]]])
    }

  }

}
