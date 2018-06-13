package pravda.node.data.serialization

import com.google.protobuf.ByteString
import fastparse.utils.Base64
import pravda.node.clients.AbciClient._
import pravda.node.data.TimechainConfig
import pravda.node.data.common.{ApplicationStateInfo, InitialDistributionMember}
import pravda.common.bytes._
import tethys._
import tethys.derivation.builder._
import tethys.derivation.semiauto._
import jackson.jacksonTokenIteratorProducer
import jackson.pretty.prettyJacksonTokenWriterProducer
import org.json4s.JsonAST
import org.json4s.JsonAST.JValue
import pravda.node.data.blockchain.ExecutionInfo
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
    JsonReader.stringReader.map(hex2byteString)

  implicit val protobufByteStringWriter: JsonWriter[ByteString] =
    JsonWriter.stringWriter.contramap(byteString2hex)

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
      } else if (token.isArrayStart) {
        var ar = Vector.empty[JValue]
        while (!token.isArrayEnd) {
          ar = ar :+ json4sReader.read(it)
        }
        it.skipExpression()
        JsonAST.JArray(
          ar.toList
        )
      } else if (token.isObjectStart) objReader.read(it)
      else if (token.isStringValue) JsonAST.JString(JsonReader.stringReader.read(it))
      else if (token.isNumberValue) JsonAST.JDecimal(JsonReader.bigDecimalReader.read(it))
      else if (token.isBooleanValue) JsonAST.JBool(JsonReader.booleanReader.read(it))
      else throw new Exception(s"Unable to read JSON. Unexpected token $token")
    }
  }

  implicit val json4sWriter: JsonWriter[JValue] = new JsonWriter[JValue] {

    def write(value: JValue, tokenWriter: TokenWriter): Unit = value match {
      case JsonAST.JNothing      => tokenWriter.writeNull()
      case JsonAST.JNull         => tokenWriter.writeNull()
      case JsonAST.JBool(bool)   => tokenWriter.writeBoolean(bool)
      case JsonAST.JDecimal(num) => tokenWriter.writeNumber(num)
      case JsonAST.JDouble(num)  => tokenWriter.writeNumber(num)
      case JsonAST.JInt(num)     => tokenWriter.writeNumber(num)
      case JsonAST.JLong(num)    => tokenWriter.writeNumber(num)
      case JsonAST.JString(s)    => tokenWriter.writeString(s)
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
          .extract(_.time)
          .from("genesis_time".as[String])(identity)
          .extract(_.chainId)
          .from("chain_id".as[String])(identity)
          .extract(_.appHash)
          .from("app_hash".as[String])(identity)
      }
    }

  implicit val timechainConfigGenesisWriter: JsonWriter[TimechainConfig.Genesis] =
    jsonWriter[TimechainConfig.Genesis] {
      describe {
        WriterBuilder[TimechainConfig.Genesis]
          .remove(_.time)
          .add("genesis_time")(_.time)
          .remove(_.chainId)
          .add("chain_id")(_.chainId)
          .remove(_.appHash)
          .add("app_hash")(_.appHash)
      }
    }

  implicit val timechainConfigGenesisValidatorReader: JsonReader[TimechainConfig.GenesisValidator] =
    jsonReader[TimechainConfig.GenesisValidator] {
      describe {
        ReaderBuilder[TimechainConfig.GenesisValidator]
          .extract(_.publicKey)
          .from("pub_key".as[TimechainConfig.CryptoKey])(identity)
      }
    }

  implicit val timechainConfigGenesisValidatorWriter: JsonWriter[TimechainConfig.GenesisValidator] =
    jsonWriter[TimechainConfig.GenesisValidator] {
      describe {
        WriterBuilder[TimechainConfig.GenesisValidator]
          .remove(_.publicKey)
          .add("pub_key")(_.publicKey)
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
  // VM RWs for tethys
  //---------------------------------------------------------------------------
  implicit val execResultReader: JsonReader[ExecutionInfo] =
    jsonReader[ExecutionInfo]

  implicit val execResultWriter: JsonWriter[ExecutionInfo] =
    jsonWriter[ExecutionInfo]

  //---------------------------------------------------------------------------
  // Domain RWs for tethys
  //---------------------------------------------------------------------------

  //---------------------------------------------------------------------------
  // Misc RWs
  //---------------------------------------------------------------------------

  implicit val domainNodeSettingsReader: JsonReader[ApplicationStateInfo] =
    jsonReader[ApplicationStateInfo]

  implicit val domainNodeSettingsWriter: JsonWriter[ApplicationStateInfo] =
    jsonWriter[ApplicationStateInfo]

  implicit val initialDistributionReader: JsonReader[InitialDistributionMember] =
    jsonReader[InitialDistributionMember]

  implicit val initialDistributionWriter: JsonWriter[InitialDistributionMember] =
    jsonWriter[InitialDistributionMember]

  //---------------------------------------------------------------------------
  // ABCI
  //---------------------------------------------------------------------------

  implicit val txResultReader: JsonReader[TxResult] =
    jsonReader[TxResult]

  implicit val txSyncResultReader: JsonReader[TxSyncResult] =
    jsonReader[TxSyncResult]

  implicit val txCommitResultReader: JsonReader[TxCommitResult] =
    jsonReader[TxCommitResult]

  implicit val rpcSyncResponseReader: JsonReader[RpcSyncResponse] =
    jsonReader[RpcSyncResponse]

  implicit val rpcAsyncResponseReader: JsonReader[RpcAsyncResponse] =
    jsonReader[RpcAsyncResponse]

  implicit val rpcCommitResponseReader: JsonReader[RpcCommitResponse] =
    jsonReader[RpcCommitResponse]

  implicit val rpcErrorReader: JsonReader[RpcError] =
    jsonReader[RpcError]

  implicit val rpcErrorWriter: JsonWriter[RpcError] =
    jsonWriter[RpcError]

  implicit val rpcTxResponseReader: JsonReader[RpcTxResponse] =
    jsonReader[RpcTxResponse]

  implicit val rpcTxResponseResultReader: JsonReader[RpcTxResponse.Result] =
    jsonReader[RpcTxResponse.Result] {
      describe {
        ReaderBuilder[RpcTxResponse.Result]
          .extract(_.tx)
          .as[String](x => ByteString.copyFrom(Base64.Decoder(x).toByteArray))
      }
    }

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
