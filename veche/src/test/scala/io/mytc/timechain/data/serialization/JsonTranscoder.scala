package io.mytc.timechain.data.serialization

import io.mytc.timechain.data.Action
import io.mytc.timechain.data.common._
import .PurchaseIntention._
import com.google.protobuf.ByteString
import io.mytc.timechain.data.Action.{ActionsFile, ActionsFileRecord}
import io.mytc.timechain.servers.ApiRoute.Track
import utest._

object JsonTranscoder extends TestSuite {

  import json._

  override def tests = Tests {

    "Transcode to json" - {
      val action: Action = Action.SmokeVape
      assert(transcode(action).to[Json] == "\"SmokeVape\"")
      assert(
        transcode(Json @@ "\"SayMonadIsAMonoidInTheCategoryOfEndofunctors\"").to[Action] ==
          Action.SayMonadIsAMonoidInTheCategoryOfEndofunctors
      )
      assert(transcode(Track(action)).to[Json] == "\"SmokeVape\"")
      assert(transcode(Json @@ "\"SmokeVape\"").to[Track] == Track(action))

      val intention  = SignedPurchaseIntention(
        PurchaseIntentionData(
          32,
          Address.fromHex("aaff"),
          DataRef.fromHex("ffaa")
        ),
        ByteString.copyFromUtf8("hello")
      )
      val expectedIntentionJson =
        """{
          |  "data" : {
          |    "nonce" : 32,
          |    "address" : "aaff",
          |    "dataRef" : "ffaa"
          |  },
          |  "signature" : "68656c6c6f"
          |}""".stripMargin
      assert(transcode(intention).to[Json] == expectedIntentionJson)

      val record = ActionsFileRecord(100L, "UTC", Action.DrinkSmoothie)
      val file = ActionsFile(Address.fromHex("aabbcc"), List(record))
      val expectedRecordJson = """{
                                 |  "address" : "aabbcc",
                                 |  "actions" : [ {
                                 |    "timestamp" : 100,
                                 |    "timezone" : "UTC",
                                 |    "action" : "DrinkSmoothie"
                                 |  } ]
                                 |}""".stripMargin
      assert(transcode(file).to[Json] == expectedRecordJson)
    }


  }
}
