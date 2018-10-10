/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.node.data.serialization

import com.google.protobuf.ByteString
import fastparse.utils.Base64
import org.json4s.JsonAST
import org.json4s.JsonAST.JValue
import pravda.common.bytes._
import pravda.node.clients.AbciClient._
import pravda.node.data.PravdaConfig
import pravda.node.data.common.{ApplicationStateInfo, CoinDistributionMember}
import pravda.node.servers.{Abci, ApiRoute}
import pravda.vm
import supertagged.{Tagged, lifterF}
import tethys._
import tethys.commons.Token
import tethys.derivation.builder._
import tethys.derivation.semiauto._
import tethys.jackson.jacksonTokenIteratorProducer
import tethys.jackson.pretty.prettyJacksonTokenWriterProducer
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter

object json {

  private def throwUtrj(token: Token) =
    throw new Exception(s"Unable to read JSON. Unexpected token $token")

  implicit def eitherReader[L: JsonReader, R: JsonReader]: JsonReader[Either[L, R]] = new JsonReader[Either[L, R]] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Either[L, R] = {
      if (it.currentToken().isObjectStart) {
        it.nextToken()
        if (it.currentToken().isFieldName) {
          it.fieldName() match {
            case "error" =>
              it.nextToken()
              val res = Left(JsonReader[L].read(it))
              it.nextToken()
              res
            case "result" =>
              it.nextToken()
              val res = Right(JsonReader[R].read(it))
              it.nextToken()
              res
          }
        } else throwUtrj(it.currentToken())
      } else throwUtrj(it.currentToken())
    }
  }

  implicit def eitherWriter[L: JsonWriter, R: JsonWriter]: JsonWriter[Either[L, R]] = new JsonWriter[Either[L, R]] {

    def write(value: Either[L, R], tw: TokenWriter): Unit = {
      tw.writeObjectStart()
      value match {
        case Left(l) =>
          tw.writeFieldName("error")
          JsonWriter[L].write(l, tw)
        case Right(r) =>
          tw.writeFieldName("result")
          JsonWriter[R].write(r, tw)
      }
      tw.writeObjectEnd()
    }
  }

  implicit def tuple2Reader[T1: JsonReader, T2: JsonReader]: JsonReader[(T1, T2)] =
    jsonReader[Tuple2[T1, T2]]

  implicit def tuple2Writer[T1: JsonWriter, T2: JsonWriter]: JsonWriter[(T1, T2)] =
    jsonWriter[Tuple2[T1, T2]]

  //----------------------------------------------------------------------
  // Supertagged support for tethys
  //----------------------------------------------------------------------

  implicit def tethysWriterLifter[T: JsonWriter, U]: JsonWriter[Tagged[T, U]] =
    lifterF[JsonWriter].lift[T, U]

  implicit def tethysReaderLifter[T: JsonReader, U]: JsonReader[Tagged[T, U]] =
    lifterF[JsonReader].lift[T, U]

  //----------------------------------------------------------------------
  // vm.Data support for tethys
  //----------------------------------------------------------------------

  implicit val dataReader: JsonReader[vm.Data] =
    JsonReader.stringReader.map(s => vm.Data.parser.all.parse(s).get.value)

  implicit val dataWriter: JsonWriter[vm.Data] =
    JsonWriter.stringWriter.contramap(_.mkString())

  implicit val dataBytesReader: JsonReader[vm.Data.Primitive.Bytes] =
    JsonReader.stringReader.map(s => vm.Data.parser.bytes.parse(s).get.value)

  implicit val dataBytesWriter: JsonWriter[vm.Data.Primitive.Bytes] =
    JsonWriter.stringWriter.contramap(_.mkString())

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
      else throwUtrj(token)
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

  implicit val pravdaConfigGenesisReader: JsonReader[PravdaConfig.Genesis] =
    jsonReader[PravdaConfig.Genesis] {
      describe {
        ReaderBuilder[PravdaConfig.Genesis]
          .extract(_.time)
          .from("genesis_time".as[String])(identity)
          .extract(_.chainId)
          .from("chain_id".as[String])(identity)
          .extract(_.appHash)
          .from("app_hash".as[String])(identity)
      }
    }

  implicit val pravdaConfigGenesisWriter: JsonWriter[PravdaConfig.Genesis] =
    jsonWriter[PravdaConfig.Genesis] {
      describe {
        WriterBuilder[PravdaConfig.Genesis]
          .remove(_.time)
          .add("genesis_time")(_.time)
          .remove(_.chainId)
          .add("chain_id")(_.chainId)
          .remove(_.appHash)
          .add("app_hash")(_.appHash)
      }
    }

  implicit val pravdaConfigGenesisValidatorReader: JsonReader[PravdaConfig.GenesisValidator] =
    jsonReader[PravdaConfig.GenesisValidator] {
      describe {
        ReaderBuilder[PravdaConfig.GenesisValidator]
          .extract(_.publicKey)
          .from("pub_key".as[PravdaConfig.CryptoKey])(identity)
      }
    }

  implicit val pravdaConfigGenesisValidatorWriter: JsonWriter[PravdaConfig.GenesisValidator] =
    jsonWriter[PravdaConfig.GenesisValidator] {
      describe {
        WriterBuilder[PravdaConfig.GenesisValidator]
          .remove(_.publicKey)
          .add("pub_key")(_.publicKey)
      }
    }

  implicit val pravdaConfigCryptoKeyReader: JsonReader[PravdaConfig.CryptoKey] =
    jsonReader[PravdaConfig.CryptoKey]

  implicit val pravdaConfigPaymentWalletReader: JsonReader[PravdaConfig.Validator] =
    jsonReader[PravdaConfig.Validator]

  implicit val pravdaConfigApiConfigReader: JsonReader[PravdaConfig.HttpConfig] =
    jsonReader[PravdaConfig.HttpConfig]

  implicit val pravdaConfigTendermintConfigReader: JsonReader[PravdaConfig.TendermintConfig] =
    jsonReader[PravdaConfig.TendermintConfig]

  implicit val pravdaConfigCryptoKeyWriter: JsonWriter[PravdaConfig.CryptoKey] =
    jsonWriter[PravdaConfig.CryptoKey]

  implicit val pravdaConfigPaymentWalletWriter: JsonWriter[PravdaConfig.Validator] =
    jsonWriter[PravdaConfig.Validator]

  implicit val pravdaConfigApiConfigWriter: JsonWriter[PravdaConfig.HttpConfig] =
    jsonWriter[PravdaConfig.HttpConfig]

  implicit val pravdaConfigTendermintConfigWriter: JsonWriter[PravdaConfig.TendermintConfig] =
    jsonWriter[PravdaConfig.TendermintConfig]

  //---------------------------------------------------------------------------
  // VM RWs for tethys
  //---------------------------------------------------------------------------

  implicit val vmErrorWriter: JsonWriter[vm.Error] = (value: vm.Error, tokenWriter: TokenWriter) => {
    value match {
      case vm.Error.UserError(message) => tokenWriter.writeString(message)
      case error                       => tokenWriter.writeNumber(error.code)
    }
  }

  implicit val vmErrorReader: JsonReader[vm.Error] =
    new JsonReader[vm.Error] {
      override def read(it: TokenIterator)(implicit fieldName: FieldName): vm.Error = {
        val token = it.currentToken()
        val res = if (token.isNumberValue) {
          it.int() match {
            case 100 => vm.Error.StackOverflow
            case 101 => vm.Error.StackUnderflow
            case 102 => vm.Error.WrongStackIndex
            case 103 => vm.Error.WrongHeapIndex
            case 104 => vm.Error.WrongType
            case 105 => vm.Error.InvalidCoinAmount
            case 106 => vm.Error.InvalidAddress
            case 200 => vm.Error.OperationDenied
            case 201 => vm.Error.PcallDenied
            case 300 => vm.Error.NoSuchProgram
            case 302 => vm.Error.NoSuchMethod
            case 400 => vm.Error.NoSuchElement
            case 500 => vm.Error.OutOfWatts
            case 600 => vm.Error.CallStackOverflow
            case 601 => vm.Error.CallStackUnderflow
            case 602 => vm.Error.ExtCallStackOverflow
            case 603 => vm.Error.ExtCallStackUnderflow
          }
        } else if (token.isStringValue) {
          vm.Error.UserError(it.string())
        } else {
          throw new Exception(s"Unable to read JSON. Unexpected token $token")
        }
        it.nextToken()
        res
      }
    }

  implicit val effectReader: JsonReader[vm.Effect] = JsonReader.builder
    .addField[String]("eventType")
    .selectReader[vm.Effect] {
      case "Event"         => jsonReader[vm.Effect.Event]
      case "ProgramCreate" => jsonReader[vm.Effect.ProgramCreate]
      case "ProgramSeal"   => jsonReader[vm.Effect.ProgramSeal]
      case "ProgramUpdate" => jsonReader[vm.Effect.ProgramUpdate]
      case "ShowBalance"   => jsonReader[vm.Effect.ShowBalance]
      case "StorageRead"   => jsonReader[vm.Effect.StorageRead]
      case "StorageRemove" => jsonReader[vm.Effect.StorageRemove]
      case "StorageWrite"  => jsonReader[vm.Effect.StorageWrite]
      case "Transfer"      => jsonReader[vm.Effect.Transfer]
    }

  implicit val effectEventWriter: JsonObjectWriter[vm.Effect.Event] = jsonWriter[vm.Effect.Event]

  implicit val effectProgramCreateWriter: JsonObjectWriter[vm.Effect.ProgramCreate] =
    jsonWriter[vm.Effect.ProgramCreate]

  implicit val effectProgramSealWriter: JsonObjectWriter[vm.Effect.ProgramSeal] = jsonWriter[vm.Effect.ProgramSeal]

  implicit val effectProgramUpdateWriter: JsonObjectWriter[vm.Effect.ProgramUpdate] =
    jsonWriter[vm.Effect.ProgramUpdate]

  implicit val effectShowBalanceWriter: JsonObjectWriter[vm.Effect.ShowBalance] = jsonWriter[vm.Effect.ShowBalance]

  implicit val effectStorageReadWriter: JsonObjectWriter[vm.Effect.StorageRead] = jsonWriter[vm.Effect.StorageRead]

  implicit val effectStorageRemoveWriter: JsonObjectWriter[vm.Effect.StorageRemove] =
    jsonWriter[vm.Effect.StorageRemove]

  implicit val effectStorageWriteWriter: JsonObjectWriter[vm.Effect.StorageWrite] = jsonWriter[vm.Effect.StorageWrite]

  implicit val effectTransferWriter: JsonObjectWriter[vm.Effect.Transfer] = jsonWriter[vm.Effect.Transfer]

  implicit val effectWriter: JsonWriter[vm.Effect] =
    JsonWriter.obj[vm.Effect].addField[String]("eventType")(_.getClass.getSimpleName) ++ jsonWriter[vm.Effect]

  implicit val finalStateReader: JsonReader[vm.FinalState] =
    jsonReader[vm.FinalState]

  implicit val finalStateWriter: JsonWriter[vm.FinalState] =
    jsonWriter[vm.FinalState]

  implicit val runtimeExceptionReader: JsonReader[vm.RuntimeException] =
    jsonReader[vm.RuntimeException]

  implicit val runtimeExceptionWriter: JsonWriter[vm.RuntimeException] =
    jsonWriter[vm.RuntimeException]

  //---------------------------------------------------------------------------
  // Domain RWs for tethys
  //---------------------------------------------------------------------------

  implicit val execResultReader: JsonReader[Abci.TransactionResult] =
    jsonReader[Abci.TransactionResult]

  implicit val execResultWriter: JsonWriter[Abci.TransactionResult] =
    jsonWriter[Abci.TransactionResult]

  //---------------------------------------------------------------------------
  // Misc RWs
  //---------------------------------------------------------------------------

  implicit val domainNodeSettingsReader: JsonReader[ApplicationStateInfo] =
    jsonReader[ApplicationStateInfo]

  implicit val domainNodeSettingsWriter: JsonWriter[ApplicationStateInfo] =
    jsonWriter[ApplicationStateInfo]

  implicit val initialDistributionReader: JsonReader[CoinDistributionMember] =
    jsonReader[CoinDistributionMember]

  implicit val initialDistributionWriter: JsonWriter[CoinDistributionMember] =
    jsonWriter[CoinDistributionMember]

  implicit val eventItemReader: JsonReader[ApiRoute.EventItem] =
    jsonReader[ApiRoute.EventItem]

  implicit val eventItemWriter: JsonWriter[ApiRoute.EventItem] =
    jsonWriter[ApiRoute.EventItem]

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
