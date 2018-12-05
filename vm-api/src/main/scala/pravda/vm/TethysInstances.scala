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

package pravda.vm

import pravda.common.bytes
import pravda.vm
import tethys._
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter
import pravda.common.json._
import tethys.commons.Token

import scala.collection.mutable

trait TethysInstances {
  private def throwUtrj(token: Token) =
    throw new Exception(s"Unable to read JSON. Unexpected token $token")

  //----------------------------------------------------------------------
  // vm.Data support for tethys
  //----------------------------------------------------------------------

  implicit val dataPrimitiveReader: JsonReader[vm.Data.Primitive] = new JsonReader[Data.Primitive] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Data.Primitive = {
      val token = it.currentToken()
      val s =
        if (token.isStringValue) it.string()
        else if (token.isFieldName) it.fieldName()
        else throwUtrj(token)
      it.nextToken()
      if (s == "null") {
        Data.Primitive.Null
      } else {
        val i = s.indexOf('.')
        val t = s.substring(0, i)
        val v = s.substring(i + 1)
        t match {
          case "int8"   => Data.Primitive.Int8(v.toByte)
          case "int16"  => Data.Primitive.Int16(v.toShort)
          case "int32"  => Data.Primitive.Int32(v.toInt)
          case "int64"  => Data.Primitive.Int64(v.toLong)
          case "bigint" => Data.Primitive.BigInt(BigInt(v))
          case "number" => Data.Primitive.Number(v.toDouble)
          case "ref"    => Data.Primitive.Ref(v.toInt)
          case "offset" => Data.Primitive.Offset(v.toInt)
          case "bytes"  => Data.Primitive.Bytes(bytes.hex2byteString(v))
          case "utf8"   => Data.Primitive.Utf8(v)
          case "bool"   => Data.Primitive.Bool(v.toBoolean)
        }
      }
    }
  }

  implicit val dataReader: JsonReader[vm.Data] = new JsonReader[Data] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Data = {
      val token = it.currentToken()
      if (token.isArrayStart) {
        def readArray[T](f: TokenIterator => T): mutable.Buffer[T] = {
          val ar = mutable.Buffer.empty[T]
          while (!it.nextToken().isArrayEnd) {
            ar += f(it)
          }
          it.nextToken()
          ar
        }
        if (it.nextToken().isStringValue) {
          it.string() match {
            case "int8"   => Data.Array.Int8Array(readArray(_.string().toByte))
            case "int16"  => Data.Array.Int16Array(readArray(_.string().toShort))
            case "int32"  => Data.Array.Int32Array(readArray(_.string().toInt))
            case "int64"  => Data.Array.Int64Array(readArray(_.string().toLong))
            case "bigint" => Data.Array.BigIntArray(readArray(x => BigInt(x.string())))
            case "number" => Data.Array.NumberArray(readArray(_.string().toDouble))
            case "ref"    => Data.Array.RefArray(readArray(_.string().toInt))
            case "bool"   => Data.Array.BoolArray(readArray(x => Data.Primitive.Bool(x.string().toBoolean)))
            case "utf8"   => Data.Array.Utf8Array(readArray(_.string()))
            case "bytes"  => Data.Array.BytesArray(readArray(x => bytes.hex2byteString(x.string())))
          }
        } else throwUtrj(token)
      } else if (token.isObjectStart) {
        val ar = mutable.Map.empty[Data.Primitive, Data.Primitive]
        var ct = it.nextToken()
        while (!ct.isObjectEnd) {
          val k = dataPrimitiveReader.read(it)
          val v = dataPrimitiveReader.read(it)
          ct = it.currentToken()
          ar.put(k, v)
        }
        it.nextToken()
        Data.Struct(ar)
      } else if (token.isStringValue) {
        dataPrimitiveReader.read(it)
      } else {
        throwUtrj(token)
      }
    }
  }

  private def writePrimitive[U](value: Data.Primitive, f: String => U) = value match {
    case Data.Primitive.Null      => f("null")
    case Data.Primitive.Int8(x)   => f(s"int8.$x")
    case Data.Primitive.Int16(x)  => f(s"int16.$x")
    case Data.Primitive.Int32(x)  => f(s"int32.$x")
    case Data.Primitive.Int64(x)  => f(s"int64.$x")
    case Data.Primitive.Number(x) => f(s"number.$x")
    case Data.Primitive.BigInt(x) => f(s"bigint.$x")
    case Data.Primitive.Ref(x)    => f(s"ref.$x")
    case Data.Primitive.Offset(x) => f(s"offset.$x")
    case Data.Primitive.Bool(x)   => f(s"bool.$x")
    case Data.Primitive.Bytes(x)  => f(s"bytes.${bytes.byteString2hex(x)}")
    case Data.Primitive.Utf8(x)   => f(s"utf8.$x")
  }

  implicit val dataPrimitiveWriter: JsonWriter[Data.Primitive] = (value: Data.Primitive, w: TokenWriter) => {
    writePrimitive(value, w.writeString)
  }

  implicit val dataWriter: JsonWriter[Data] = (value: Data, w: TokenWriter) => {

    def writeArray[T](xs: Seq[T], t: String)(f: T => Unit) = {
      w.writeArrayStart()
      w.writeString(t)
      xs.foreach(f(_))
      w.writeArrayEnd()
    }

    value match {
      case p: Data.Primitive          => dataPrimitiveWriter.write(p, w)
      case Data.Array.Int8Array(xs)   => writeArray(xs, "int8")(x => w.writeString(x.toString))
      case Data.Array.Int16Array(xs)  => writeArray(xs, "int16")(x => w.writeString(x.toString))
      case Data.Array.Int32Array(xs)  => writeArray(xs, "int32")(x => w.writeString(x.toString))
      case Data.Array.Int64Array(xs)  => writeArray(xs, "int64")(x => w.writeString(x.toString))
      case Data.Array.NumberArray(xs) => writeArray(xs, "number")(x => w.writeString(x.toString))
      case Data.Array.BigIntArray(xs) => writeArray(xs, "bigint")(x => w.writeString(x.toString))
      case Data.Array.RefArray(xs)    => writeArray(xs, "ref")(x => w.writeString(x.toString))
      case Data.Array.BoolArray(xs)   => writeArray(xs, "bool")(x => w.writeString(x.toBoolean.toString))
      case Data.Array.Utf8Array(xs)   => writeArray(xs, "utf8")(x => w.writeString(x))
      case Data.Array.BytesArray(xs)  => writeArray(xs, "bytes")(x => w.writeString(bytes.byteString2hex(x)))
      case Data.Struct(xs) =>
        w.writeObjectStart()
        xs.foreach {
          case (k, v) =>
            writePrimitive(k, w.writeFieldName(_))
            dataPrimitiveWriter.write(v, w)
        }
        w.writeObjectEnd()
    }
  }

  implicit val marshalledDataSimpleReader: JsonReader[MarshalledData.Simple] =
    jsonReader[MarshalledData.Simple]

  implicit val marshalledDataComplexReader: JsonReader[MarshalledData.Complex] =
    jsonReader[MarshalledData.Complex]

  implicit val marshalledDataSimpleWriter: JsonObjectWriter[MarshalledData.Simple] =
    jsonWriter[MarshalledData.Simple]

  implicit val marshalledDataComplexWriter: JsonObjectWriter[MarshalledData.Complex] =
    jsonWriter[MarshalledData.Complex]

  implicit val marshalledDataReader: JsonReader[MarshalledData] =
    sealedTraitReader { implicit fieldName =>
      {
        case (it, "simple")  => marshalledDataSimpleReader.read(it)
        case (it, "complex") => marshalledDataComplexReader.read(it)
      }
    }

  implicit val marshalledDataWriter: JsonWriter[MarshalledData] =
    (value: MarshalledData, tw: TokenWriter) => {
      tw.writeObjectStart()
      value match {
        case simple: MarshalledData.Simple   => marshalledDataSimpleWriter.write("simple", simple, tw)
        case complex: MarshalledData.Complex => marshalledDataComplexWriter.write("complex", complex, tw)
      }
      tw.writeObjectEnd()
    }

  implicit val primitiveRefReader: JsonReader[Data.Primitive.Ref] =
    JsonReader.stringReader.map(s => Data.Primitive.Ref(s.stripPrefix("ref.").toInt))

  implicit val primitiveRefWriter: JsonWriter[Data.Primitive.Ref] =
    JsonWriter.stringWriter.contramap(r => s"ref.${r.data}")

  implicit val primitiveBytesReader: JsonReader[Data.Primitive.Bytes] =
    JsonReader.stringReader.map(s => Data.Primitive.Bytes(bytes.hex2byteString(s.stripPrefix("bytes."))))

  implicit val primitiveBytesWriter: JsonWriter[Data.Primitive.Bytes] =
    JsonWriter.stringWriter.contramap(s => s"bytes.${bytes.byteString2hex(s.data)}")

  implicit val primitiveBigIntReader: JsonReader[Data.Primitive.BigInt] =
    JsonReader.stringReader.map(s => Data.Primitive.BigInt(BigInt(s.stripPrefix("bigint."))))

  implicit val primitiveBigIntWriter: JsonWriter[Data.Primitive.BigInt] =
    JsonWriter.stringWriter.contramap(b => s"bigint.$b")

  implicit val primitiveRefSupport: MapKeySupport[Data.Primitive.Ref] = new MapKeySupport[Data.Primitive.Ref] {
    def show(s: Data.Primitive.Ref): String = s"ref:${s.data}"
    def mk(s: String): Data.Primitive.Ref = Data.Primitive.Ref(s.substring(s.indexOf(':') + 1).toInt)
  }

  //---------------------------------------------------------------------------
  // VM RWs for tethys
  //---------------------------------------------------------------------------

  private case class ErrorJson(code: Int, message: String)

  implicit val vmErrorWriter: JsonWriter[vm.Error] =
    jsonWriter[ErrorJson].contramap {
      case e @ Error.UserError(message) => ErrorJson(e.code, message)
      case e @ Error.DataError(message) => ErrorJson(e.code, message)
      case e                            => ErrorJson(e.code, e.toString)
    }

  implicit val vmErrorReader: JsonReader[vm.Error] =
    jsonReader[ErrorJson].map {
      case ErrorJson(100, _)       => vm.Error.StackOverflow
      case ErrorJson(101, _)       => vm.Error.StackUnderflow
      case ErrorJson(102, _)       => vm.Error.WrongStackIndex
      case ErrorJson(103, _)       => vm.Error.WrongHeapIndex
      case ErrorJson(104, _)       => vm.Error.WrongType
      case ErrorJson(105, _)       => vm.Error.InvalidCoinAmount
      case ErrorJson(106, _)       => vm.Error.InvalidAddress
      case ErrorJson(107, _)       => vm.Error.InvalidArgument
      case ErrorJson(200, _)       => vm.Error.OperationDenied
      case ErrorJson(201, _)       => vm.Error.PcallDenied
      case ErrorJson(202, _)       => vm.Error.NotEnoughMoney
      case ErrorJson(203, _)       => vm.Error.AmountShouldNotBeNegative
      case ErrorJson(204, _)       => vm.Error.ProgramIsSealed
      case ErrorJson(300, _)       => vm.Error.NoSuchProgram
      case ErrorJson(302, _)       => vm.Error.NoSuchMethod
      case ErrorJson(400, _)       => vm.Error.NoSuchElement
      case ErrorJson(500, _)       => vm.Error.OutOfWatts
      case ErrorJson(600, _)       => vm.Error.CallStackOverflow
      case ErrorJson(601, _)       => vm.Error.CallStackUnderflow
      case ErrorJson(602, _)       => vm.Error.ExtCallStackOverflow
      case ErrorJson(603, _)       => vm.Error.ExtCallStackUnderflow
      case ErrorJson(700, message) => vm.Error.UserError(message)
      case ErrorJson(701, message) => vm.Error.DataError(message)
      case _                       => throw new Exception("Unknown error")
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
}
