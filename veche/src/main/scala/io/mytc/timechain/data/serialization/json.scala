package io.mytc.timechain.data.serialization


import com.google.protobuf.ByteString
import io.mytc.timechain.clients.AbciClient._
import io.mytc.timechain.data.Action.{ActionsFile, ActionsFileRecord}
import io.mytc.timechain.data.{Action, TimechainConfig}
import io.mytc.timechain.data.common.{NodeSettings, OrganizationInfo}
import io.mytc.timechain.data.domain.{ActionPackage, Offer}
import io.mytc.timechain.data.offchain.PurchaseIntention.{AuthorizedPurchaseIntention, SignedPurchaseIntention}
import io.mytc.timechain.data.offchain.PurchaseIntentionData
import io.mytc.timechain.servers.ApiRoute.{AddDeposit, Commit, Track}
import io.mytc.timechain.utils
import tethys._
import tethys.derivation.builder._
import tethys.derivation.semiauto._
import jackson.jacksonTokenIteratorProducer
import jackson.pretty.prettyJacksonTokenWriterProducer
import org.json4s.JsonAST
import org.json4s.JsonAST.JValue
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter
import supertagged.{Tagged, lifterF}


object json {

  //----------------------------------------------------------------------
  // Supertagged support for tethys
  //----------------------------------------------------------------------

  implicit def tethysWriterLifter[T: JsonWriter, U]: JsonWriter[Tagged[T, U]] =
    lifterF[JsonWriter].lift[T, U]

  implicit def tethysReaderLifter[T: JsonReader, U]: JsonReader[Tagged[T, U]] =
    lifterF[JsonReader].lift[T, U]

  //----------------------------------------------------------------------
  // Protobufs' ByteString support for tethys
  //----------------------------------------------------------------------

  implicit val protobufByteStringReader: JsonReader[ByteString] =
    JsonReader.stringReader.map(utils.hex2byteString)

  implicit val protobufByteStringWriter: JsonWriter[ByteString] =
    JsonWriter.stringWriter.contramap(utils.bytes2hex)

  //---------------------------------------------------------------------------
  // Json4s AST support
  //---------------------------------------------------------------------------

  implicit val fieldReader: JsonReader[JsonAST.JField] = jsonReader[JsonAST.JField]

  lazy val objReader: JsonReader[JsonAST.JObject] = jsonReader[JsonAST.JObject]

  implicit val json4sReader: JsonReader[JValue] = new JsonReader[JValue] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): JValue = {
      val token = it.currentToken()
      if (token.isNullValue) {
        it.skipExpression()
        JsonAST.JNull
      }
      else if (token.isArrayStart) {
        var ar = Vector.empty[JValue]
        while(!token.isArrayEnd) {
          ar = ar :+ json4sReader.read(it)
        }
        it.skipExpression()
        JsonAST.JArray(
          ar.toList
        )
      }
      else if (token.isObjectStart) objReader.read(it)
      else if (token.isStringValue) JsonAST.JString(JsonReader.stringReader.read(it))
      else if (token.isNumberValue) JsonAST.JDecimal(JsonReader.bigDecimalReader.read(it))
      else if (token.isBooleanValue) JsonAST.JBool(JsonReader.booleanReader.read(it))
      else throw new Exception(s"Unable to read JSON. Unexpected token $token")
    }
  }

  implicit val json4sWriter: JsonWriter[JValue] = new JsonWriter[JValue] {

    def write(value: JValue, tokenWriter: TokenWriter): Unit = value match {
      case JsonAST.JNothing => tokenWriter.writeNull()
      case JsonAST.JNull => tokenWriter.writeNull()
      case JsonAST.JBool(bool) => tokenWriter.writeBoolean(bool)
      case JsonAST.JDecimal(num) => tokenWriter.writeNumber(num)
      case JsonAST.JDouble(num) => tokenWriter.writeNumber(num)
      case JsonAST.JInt(num) => tokenWriter.writeNumber(num)
      case JsonAST.JLong(num) => tokenWriter.writeNumber(num)
      case JsonAST.JString(s) => tokenWriter.writeString(s)
      case JsonAST.JObject(xs) =>
        tokenWriter.writeObjectStart()
        for ((k, v) <- xs) {
          tokenWriter.writeFieldName(k)
          write(v, tokenWriter)
        }
        tokenWriter.writeObjectEnd()
      case JsonAST.JArray(xs) =>
        tokenWriter.writeArrayStart()
        for (x <- xs) write(x, tokenWriter)
        tokenWriter.writeArrayEnd()
      case JsonAST.JSet(xs) =>
        tokenWriter.writeArrayStart()
        for (x <- xs) write(x, tokenWriter)
        tokenWriter.writeArrayEnd()
    }
  }

  //----------------------------------------------------------------------
  // Config RWs for tethys
  //----------------------------------------------------------------------

  implicit val timechainConfigGenesisReader: JsonReader[TimechainConfig.Genesis] =
    jsonReader[TimechainConfig.Genesis] {
      describe {
        ReaderBuilder[TimechainConfig.Genesis]
          .extract(_.time).from("genesis_time".as[String])(identity)
          .extract(_.chainId).from("chain_id".as[String])(identity)
          .extract(_.appHash).from("app_hash".as[String])(identity)
      }
    }

  implicit val timechainConfigGenesisWriter: JsonWriter[TimechainConfig.Genesis] =
    jsonWriter[TimechainConfig.Genesis] {
      describe {
        WriterBuilder[TimechainConfig.Genesis]
          .remove(_.time).add("genesis_time")(_.time)
          .remove(_.chainId).add("chain_id")(_.chainId)
          .remove(_.appHash).add("app_hash")(_.appHash)
      }
    }

  implicit val timechainConfigGenesisValidatorReader: JsonReader[TimechainConfig.GenesisValidator] =
    jsonReader[TimechainConfig.GenesisValidator] {
      describe {
        ReaderBuilder[TimechainConfig.GenesisValidator]
          .extract(_.publicKey).from("pub_key".as[TimechainConfig.CryptoKey])(identity)
      }
    }

  implicit val timechainConfigGenesisValidatorWriter: JsonWriter[TimechainConfig.GenesisValidator] =
    jsonWriter[TimechainConfig.GenesisValidator] {
      describe {
        WriterBuilder[TimechainConfig.GenesisValidator]
          .remove(_.publicKey).add("pub_key")(_.publicKey)
      }
    }

  implicit val timechainConfigCryptoKeyReader: JsonReader[TimechainConfig.CryptoKey] =
    jsonReader[TimechainConfig.CryptoKey]

  implicit val timechainConfigPaymentWalletReader: JsonReader[TimechainConfig.PaymentWallet] =
    jsonReader[TimechainConfig.PaymentWallet]

  implicit val timechainConfigApiConfigReader: JsonReader[TimechainConfig.ApiConfig] =
    jsonReader[TimechainConfig.ApiConfig]

  implicit val timechainConfigTendermintConfigReader: JsonReader[TimechainConfig.TendermintConfig] =
    jsonReader[TimechainConfig.TendermintConfig]

  implicit val timechainConfigCryptoKeyWriter: JsonWriter[TimechainConfig.CryptoKey] =
    jsonWriter[TimechainConfig.CryptoKey]

  implicit val timechainConfigPaymentWalletWriter: JsonWriter[TimechainConfig.PaymentWallet] =
    jsonWriter[TimechainConfig.PaymentWallet]

  implicit val timechainConfigApiConfigWriter: JsonWriter[TimechainConfig.ApiConfig] =
    jsonWriter[TimechainConfig.ApiConfig]

  implicit val timechainConfigTendermintConfigWriter: JsonWriter[TimechainConfig.TendermintConfig] =
    jsonWriter[TimechainConfig.TendermintConfig]

  //---------------------------------------------------------------------------
  // Domain RWs for tethys
  //---------------------------------------------------------------------------

  implicit val domainOfferReader: JsonReader[Offer] =
    jsonReader[Offer]

  implicit val domainActionPackageReader: JsonReader[ActionPackage] =
    jsonReader[ActionPackage]

  implicit val domainActionsFileReader: JsonReader[ActionsFile] =
    jsonReader[ActionsFile]

  implicit val domainActionsFileRecordReader: JsonReader[ActionsFileRecord] =
    jsonReader[ActionsFileRecord]

  implicit val domainOfferWriter: JsonWriter[Offer] =
    jsonWriter[Offer]

  implicit val domainActionPackageWriter: JsonWriter[ActionPackage] =
    jsonWriter[ActionPackage]

  implicit val domainActionsFileWriter: JsonWriter[ActionsFile] =
    jsonWriter[ActionsFile]

  implicit val domainActionsFileRecordWriter: JsonWriter[ActionsFileRecord] =
    jsonWriter[ActionsFileRecord]

  implicit val intentionDataReader: JsonReader[PurchaseIntentionData] =
    jsonReader[PurchaseIntentionData]

  implicit val intentionDataWriter: JsonObjectWriter[PurchaseIntentionData] =
    jsonWriter[PurchaseIntentionData]

  implicit val signedIntentionReader: JsonReader[SignedPurchaseIntention] =
    jsonReader[SignedPurchaseIntention]

  implicit val signedIntentionWriter: JsonObjectWriter[SignedPurchaseIntention] =
    jsonWriter[SignedPurchaseIntention]

  implicit val authIntentionReader: JsonReader[AuthorizedPurchaseIntention] =
    jsonReader[AuthorizedPurchaseIntention]

  implicit val authIntentionWriter: JsonObjectWriter[AuthorizedPurchaseIntention] =
    jsonWriter[AuthorizedPurchaseIntention]

  implicit val commitReader: JsonReader[Commit] =
    jsonReader[Commit]

  implicit val commitWriter: JsonObjectWriter[Commit] =
    jsonWriter[Commit]

  implicit val addDepositReader: JsonReader[AddDeposit] =
    jsonReader[AddDeposit]

  implicit val addDepositWriter: JsonObjectWriter[AddDeposit] =
    jsonWriter[AddDeposit]


  //---------------------------------------------------------------------------
  // Actions
  //---------------------------------------------------------------------------
  private def strToAction: String => Action = {
    case "SmokeVape" => Action.SmokeVape
    case "DrinkSmoothie" => Action.DrinkSmoothie
    case "DriveGyroscooter" => Action.DriveGyroscooter
    case "WorkInCoworking" => Action.WorkInCoworking
    case "LiveInColiving" => Action.LiveInColiving
    case "SayMonadIsAMonoidInTheCategoryOfEndofunctors" => Action.SayMonadIsAMonoidInTheCategoryOfEndofunctors
  }

  implicit val trackWriter: JsonWriter[Track] = JsonWriter.stringWriter.contramap[Track]{
    _.action.toString
  }

  implicit val trackReader: JsonReader[Track] = JsonReader.stringReader.map(s => Track(strToAction(s)))

  implicit val actionWriter: JsonWriter[Action] = JsonWriter.stringWriter.contramap[Action]{
    _.toString
  }

  implicit val actionReader: JsonReader[Action] = JsonReader.stringReader.map(strToAction)

  //---------------------------------------------------------------------------
  // Common RWs
  //---------------------------------------------------------------------------

  implicit val domainOrganizationInfoReader: JsonReader[OrganizationInfo] =
    jsonReader[OrganizationInfo]
  
  implicit val domainOrganizationInfoWriter: JsonWriter[OrganizationInfo] =
    jsonWriter[OrganizationInfo]

  //---------------------------------------------------------------------------
  // Misc RWs
  //---------------------------------------------------------------------------

  implicit val domainNodeSettingsReader: JsonReader[NodeSettings] =
    jsonReader[NodeSettings]

  implicit val domainNodeSettingsWriter: JsonWriter[NodeSettings] =
    jsonWriter[NodeSettings]


  //---------------------------------------------------------------------------
  // ABCI
  //---------------------------------------------------------------------------
  implicit val txResultReader: JsonReader[TxResult] =
    jsonReader[TxResult]

  implicit val txResultWriter: JsonWriter[TxResult] =
    jsonWriter[TxResult]

  implicit val txSyncResultReader: JsonReader[TxSyncResult] =
    jsonReader[TxSyncResult]

  implicit val txSyncResultWriter: JsonWriter[TxSyncResult] =
    jsonWriter[TxSyncResult]

  implicit val txCommitResultReader: JsonReader[TxCommitResult] =
    jsonReader[TxCommitResult]

  implicit val txCommitResultWriter: JsonWriter[TxCommitResult] =
    jsonWriter[TxCommitResult]

  implicit val rpcSyncResponseReader: JsonReader[RpcSyncResponse] =
    jsonReader[RpcSyncResponse]

  implicit val rpcSyncResponseWriter: JsonWriter[RpcSyncResponse] =
    jsonWriter[RpcSyncResponse]

  implicit val rpcAsyncResponseReader: JsonReader[RpcAsyncResponse] =
    jsonReader[RpcAsyncResponse]

  implicit val rpcAsyncResponseWriter: JsonWriter[RpcAsyncResponse] =
    jsonWriter[RpcAsyncResponse]

  implicit val rpcCommitResponseReader: JsonReader[RpcCommitResponse] =
    jsonReader[RpcCommitResponse]

  implicit val rpcCommitResponseWriter: JsonWriter[RpcCommitResponse] =
    jsonWriter[RpcCommitResponse]

  //---------------------------------------------------------------------------
  // Tethys transcoding
  //---------------------------------------------------------------------------

  implicit def tethysJsonEncoder[T: JsonWriter]: Transcoder[T, Json] =
    Json @@ _.asJson

  implicit def tethysJsonDecoder[T: JsonReader]: Transcoder[Json, T] =
    _.jsonAs[T].fold(throw _, identity)


  //  implicit val zdtPushkaWriter = new Writer[ZonedDateTime] {
//    def write(value: ZonedDateTime): Ast =
//      Ast.Str(value.format(PushkaDateTimeFormatter))
//  }
//
//  implicit val zdtPushkaReader = new Reader[ZonedDateTime] {
//    def read(value: Ast): ZonedDateTime = value match {
//      case Ast.Str(s) => ZonedDateTime.parse(s, PushkaDateTimeFormatter)
//    }
//  }
//
//  private val PushkaDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss Z")
}
