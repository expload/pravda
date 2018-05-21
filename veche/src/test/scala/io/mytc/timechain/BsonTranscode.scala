package io.mytc.timechain

import com.google.protobuf.ByteString
import io.mytc.timechain.data.common.Address
import io.mytc.timechain.data.serialization.Bson
import io.mytc.timechain.servers.Abci.StoredProgram
import utest._

object BsonTranscode extends TestSuite {

  val tests = Tests {
    "trancode from case class" - {
      import io.mytc.timechain.data.serialization._
      import io.mytc.timechain.data.serialization.bson._

      val storedProrgram = StoredProgram(ByteString.copyFrom(Array[Byte](0x01, 0x02)),
                                         Address @@ ByteString.copyFrom(Array[Byte](0x03, 0x04)))
      val bson = transcode(storedProrgram).to[Bson]
      transcode(Bson @@ bson).to[StoredProgram] ==> storedProrgram
    }
  }
}
