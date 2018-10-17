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
      if (s == "null") {
        Data.Primitive.Null
      } else {
        val i = s.indexOf(':')
        val t = s.substring(0, i)
        val v = s.substring(i + 1)
        t match {
          case "int8"   => Data.Primitive.Int8(v.toByte)
          case "int16"  => Data.Primitive.Int16(v.toShort)
          case "int32"  => Data.Primitive.Int32(v.toInt)
          case "uint8"  => Data.Primitive.Uint8(v.toInt)
          case "uint16" => Data.Primitive.Uint16(v.toInt)
          case "uint32" => Data.Primitive.Uint32(v.toLong)
          case "bigint" => Data.Primitive.BigInt(BigInt(v))
          case "number" => Data.Primitive.Number(v.toDouble)
          case "ref"    => Data.Primitive.Ref(v.toInt)
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
          var ct = it.nextToken()
          while (!ct.isArrayEnd) {
            ar += f(it)
            ct = it.nextToken()
          }
          ar
        }
        if (it.nextToken().isStringValue) {
          it.string() match {
            case "int8"   => Data.Array.Int8Array(readArray(_.string().toByte))
            case "int16"  => Data.Array.Int16Array(readArray(_.string().toShort))
            case "int32"  => Data.Array.Int32Array(readArray(_.string().toInt))
            case "uint8"  => Data.Array.Uint8Array(readArray(_.string().toInt))
            case "uint16" => Data.Array.Uint16Array(readArray(_.string().toInt))
            case "uint32" => Data.Array.Uint32Array(readArray(_.string().toLong))
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
          it.nextToken()
          val v = dataPrimitiveReader.read(it)
          ar.put(k, v)
          ct = it.nextToken()
        }
        Data.Struct(ar)
      } else if (token.isStringValue || token.isBooleanValue || token.isNullValue) {
        dataPrimitiveReader.read(it)
      } else throwUtrj(token)
    }
  }

  private def writePrimitive[U](value: Data.Primitive, f: String => U) = value match {
    case Data.Primitive.Null      => f("null")
    case Data.Primitive.Int8(x)   => f(s"int8:$x")
    case Data.Primitive.Int16(x)  => f(s"int16:$x")
    case Data.Primitive.Int32(x)  => f(s"int32:$x")
    case Data.Primitive.Uint8(x)  => f(s"uint8:$x")
    case Data.Primitive.Uint16(x) => f(s"uint16:$x")
    case Data.Primitive.Uint32(x) => f(s"uint32:$x")
    case Data.Primitive.Number(x) => f(s"number:$x")
    case Data.Primitive.BigInt(x) => f(s"bigint:$x")
    case Data.Primitive.Ref(x)    => f(s"ref:$x")
    case Data.Primitive.Bool(x)   => f(s"bool:$x")
    case Data.Primitive.Bytes(x)  => f(s"bytes:${bytes.byteString2hex(x)}")
    case Data.Primitive.Utf8(x)   => f(s"utf8:$x")
  }

  implicit val dataPrimitiveWriter: JsonWriter[Data.Primitive] = (value: Data.Primitive, w: TokenWriter) => {
    writePrimitive(value, w.writeString)
  }

  implicit val dataWriter: JsonWriter[Data] = new JsonWriter[Data] {

    def write(value: Data, w: TokenWriter): Unit = {

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
        case Data.Array.Uint8Array(xs)  => writeArray(xs, "uint8")(x => w.writeString(x.toString))
        case Data.Array.Uint16Array(xs) => writeArray(xs, "uint16")(x => w.writeString(x.toString))
        case Data.Array.Uint32Array(xs) => writeArray(xs, "uint32")(x => w.writeString(x.toString))
        case Data.Array.NumberArray(xs) => writeArray(xs, "number")(x => w.writeString(x.toString))
        case Data.Array.BigIntArray(xs) => writeArray(xs, "bigint")(x => w.writeString(x.toString))
        case Data.Array.RefArray(xs)    => writeArray(xs, "ref")(x => w.writeString(x.toString))
        case Data.Array.BoolArray(xs)   => writeArray(xs, "bool")(x => w.writeString(x.toString))
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
  }

  implicit val primitiveRefReader: JsonReader[Data.Primitive.Ref] =
    JsonReader.stringReader.map(s => Data.Primitive.Ref(s.stripPrefix("ref:").toInt))

  implicit val primitiveRefWriter: JsonWriter[Data.Primitive.Ref] =
    JsonWriter.stringWriter.contramap(r => s"ref:${r.data}")

  implicit val primitiveBytesReader: JsonReader[Data.Primitive.Bytes] =
    JsonReader.stringReader.map(s => Data.Primitive.Bytes(bytes.hex2byteString(s.stripPrefix("bytes:"))))

  implicit val primitiveBytesWriter: JsonWriter[Data.Primitive.Bytes] =
    JsonWriter.stringWriter.contramap(s => s"bytes:${bytes.byteString2hex(s.data)}")

  implicit val primitiveBigIntReader: JsonReader[Data.Primitive.BigInt] =
    JsonReader.stringReader.map(s => Data.Primitive.BigInt(BigInt(s.stripPrefix("bigint:"))))

  implicit val primitiveBigIntWriter: JsonWriter[Data.Primitive.BigInt] =
    JsonWriter.stringWriter.contramap(b => s"bigint:$b")

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
            case 202 => vm.Error.NotEnoughMoney
            case 203 => vm.Error.AmountShouldNotBeNegative
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
}
