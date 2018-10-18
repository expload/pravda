package pravda.node

import com.google.protobuf.ByteString
import pravda.node.servers.Abci.StoredProgram
import utest._

object BsonTranscode extends TestSuite {

  val tests = Tests {
    "trancode from case class" - {
      import pravda.node.data.serialization._
      import pravda.node.data.serialization.bson._

      val storedProrgram = StoredProgram(ByteString.copyFrom(Array[Byte](0x01, 0x02)), `sealed` = false)
      val bson = transcode(storedProrgram).to[Bson]
      transcode(Bson @@ bson).to[StoredProgram] ==> storedProrgram
    }
  }
}
