package pravda.node

import com.google.protobuf.ByteString
import pravda.node.servers.Abci.StoredProgram
import utest._

object BJsonTranscode extends TestSuite {

  val tests = Tests {
    "trancode from case class" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.bjson._

      val storedProrgram = StoredProgram(ByteString.copyFrom(Array[Byte](0x01, 0x02)), `sealed` = false)
      val bjson = transcode(storedProrgram).to[BJson]
      transcode(BJson @@ bjson).to[StoredProgram] ==> storedProrgram
    }
  }
}
