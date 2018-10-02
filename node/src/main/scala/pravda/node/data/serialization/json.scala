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
import pravda.node.clients.AbciClient._
import pravda.node.data.PravdaConfig
import pravda.node.data.common.{ApplicationStateInfo, CoinDistributionMember}
import pravda.common.bytes._
import tethys._
import tethys.derivation.builder._
import tethys.derivation.semiauto._
import jackson.jacksonTokenIteratorProducer
import jackson.pretty.prettyJacksonTokenWriterProducer
import pravda.node.data.blockchain.ExecutionInfo
import pravda.node.servers.ApiRoute
import pravda.vm.Data
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
  // vm.Data support for tethys
  //----------------------------------------------------------------------

  implicit val dataReader: JsonReader[Data] =
    JsonReader.stringReader.map(s => Data.parser.all.parse(s).get.value)

  implicit val dataWriter: JsonWriter[Data] =
    JsonWriter.stringWriter.contramap(_.mkString())

  //----------------------------------------------------------------------
  // Protobufs' ByteString support for tethys
  //----------------------------------------------------------------------

  implicit val protobufByteStringReader: JsonReader[ByteString] =
    JsonReader.stringReader.map(hex2byteString)

  implicit val protobufByteStringWriter: JsonWriter[ByteString] =
    JsonWriter.stringWriter.contramap(byteString2hex)

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
}
