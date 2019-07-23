package pravda.common.serialization

import pravda.common.data.blockchain.TransactionEffects
import tethys._
import tethys.derivation.builder._
import tethys.derivation.semiauto._
import tethys.jackson.jacksonTokenIteratorProducer
import tethys.jackson.pretty.prettyJacksonTokenWriterProducer

import pravda.common.bytes
import pravda.common.vm
import tethys._
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter
import pravda.common.vm.{Data, Effect, FinalState, MarshalledData, RuntimeException}
import pravda.common.vm.Error
import tethys.commons.Token

import scala.collection.mutable

import com.google.protobuf.ByteString
import pravda.common.bytes.{byteString2hex, hex2byteString}
import supertagged.{Tagged, lifterF}
import tethys.{JsonReader, JsonWriter}
import tethys.commons.Token
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter

import scala.collection.mutable

trait CommonTethysInstances {
  private def throwUtrj(token: Token) =
    throw new Exception(s"Unable to read JSON. Unexpected token $token")

  def sealedTraitReader[T](f: FieldName => PartialFunction[(TokenIterator, String), T]): JsonReader[T] =
    new JsonReader[T] {

      def read(it: TokenIterator)(implicit fieldName: FieldName): T = {
        if (it.currentToken().isObjectStart) {
          it.nextToken()
          if (it.currentToken().isFieldName) {
            val n = it.fieldName()
            it.nextToken()
            val tpl = (it, n)
            val pf = f(fieldName)
            val res =
              if (pf.isDefinedAt(tpl)) pf(tpl)
              else throw new Exception(s"Unexpected field: $n")
            it.nextToken()
            res
          } else throwUtrj(it.currentToken())
        } else throwUtrj(it.currentToken())
      }
    }

  implicit def eitherReader[L: JsonReader, R: JsonReader]: JsonReader[Either[L, R]] =
    sealedTraitReader { implicit fieldName =>
      {
        case (it, "failure") => Left(JsonReader[L].read(it))
        case (it, "success") => Right(JsonReader[R].read(it))
      }
    }

  implicit def eitherWriter[L: JsonWriter, R: JsonWriter]: JsonWriter[Either[L, R]] =
    (value: Either[L, R], tw: TokenWriter) => {
      tw.writeObjectStart()
      value match {
        case Left(l)  => JsonWriter[L].write("failure", l, tw)
        case Right(r) => JsonWriter[R].write("success", r, tw)
      }
      tw.writeObjectEnd()
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
  // Protobufs' ByteString support for tethys
  //----------------------------------------------------------------------

  implicit val protobufByteStringReader: JsonReader[ByteString] =
    JsonReader.stringReader.map(hex2byteString)

  implicit val protobufByteStringWriter: JsonWriter[ByteString] =
    JsonWriter.stringWriter.contramap(byteString2hex)

  //---------------------------------------------------------------------------
  // scala.Map support
  //---------------------------------------------------------------------------

  trait MapKeySupport[T] { // TODO use KeyReader, KeyWriter instead
    def show(x: T): String
    def mk(x: String): T
  }

  object MapKeySupport {

    def apply[T: MapKeySupport]: MapKeySupport[T] =
      implicitly[MapKeySupport[T]]
  }

  implicit def mapReader[K: MapKeySupport, V: JsonReader]: JsonReader[Map[K, V]] = new JsonReader[Map[K, V]] {

    def read(it: TokenIterator)(implicit fieldName: FieldName): Map[K, V] = {
      val token = it.currentToken()
      val res = mutable.Map.empty[K, V]
      if (token.isObjectStart) {
        var ct = it.nextToken()
        while (!ct.isObjectEnd) {
          val k = MapKeySupport[K].mk(it.fieldName())
          it.nextToken()
          res.put(k, JsonReader[V].read(it))
          ct = it.currentToken()
        }
        it.nextToken()
      } else throwUtrj(token)
      res.toMap
    }
  }

  implicit def mapWriter[K: MapKeySupport, V: JsonWriter]: JsonWriter[Map[K, V]] = new JsonWriter[Map[K, V]] {

    def write(value: Map[K, V], w: TokenWriter): Unit = {
      val ms = MapKeySupport[K]
      w.writeObjectStart()
      value.foreach {
        case (k, v) =>
          JsonWriter[V].write(ms.show(k), v, w)
      }
      w.writeObjectEnd()
    }
  }

  implicit val intMapSupport = new MapKeySupport[Int] {
    def show(x: Int): String = x.toString
    def mk(x: String): Int = x.toInt
  }
}

trait TethysInstances extends CommonTethysInstances {

  ////////// VM ///////////
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

  implicit val dataReader: JsonReader[Data] = new JsonReader[Data] {

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

  implicit val primitiveInt64Reader: JsonReader[Data.Primitive.Int64] =
    JsonReader.stringReader.map(s => Data.Primitive.Int64(s.stripPrefix("int64.").toLong))

  implicit val primitiveInt64Writer: JsonWriter[Data.Primitive.Int64] =
    JsonWriter.stringWriter.contramap(b => s"int64.$b")

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
      case ErrorJson(205, _)       => vm.Error.NonValidatorManager
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

  implicit val effectReader: JsonReader[Effect] = JsonReader.builder
    .addField[String]("eventType")
    .selectReader[Effect] {
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

  implicit val effectEventReader: JsonReader[vm.Effect.Event] = jsonReader[vm.Effect.Event]
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

  implicit val effectTransferReader: JsonReader[vm.Effect.Transfer] = jsonReader[vm.Effect.Transfer]
  implicit val effectTransferWriter: JsonObjectWriter[vm.Effect.Transfer] = jsonWriter[vm.Effect.Transfer]

  implicit val effectWriter: JsonWriter[Effect] =
    JsonWriter.obj[Effect].addField[String]("eventType")(_.getClass.getSimpleName) ++ jsonWriter[Effect]

  implicit val finalStateReader: JsonReader[FinalState] =
    jsonReader[FinalState]

  implicit val finalStateWriter: JsonWriter[FinalState] =
    jsonWriter[FinalState]

  implicit val runtimeExceptionReader: JsonReader[RuntimeException] =
    jsonReader[RuntimeException]

  implicit val runtimeExceptionWriter: JsonWriter[RuntimeException] =
    jsonWriter[RuntimeException]

  /////// OTHER /////
  implicit val transferEffectsReader: JsonReader[TransactionEffects.Transfers] =
    jsonReader[TransactionEffects.Transfers]

  implicit val transferEffectsWriter: JsonObjectWriter[TransactionEffects.Transfers] =
    jsonWriter[TransactionEffects.Transfers]

  implicit val programEventsReader: JsonReader[TransactionEffects.ProgramEvents] =
    jsonReader[TransactionEffects.ProgramEvents]

  implicit val programEventsWriter: JsonObjectWriter[TransactionEffects.ProgramEvents] =
    jsonWriter[TransactionEffects.ProgramEvents]

  implicit val transactionAllEffectsReader: JsonReader[TransactionEffects.AllEffects] =
    jsonReader[TransactionEffects.AllEffects]

  implicit val transactionAllEffectsWriter: JsonObjectWriter[TransactionEffects.AllEffects] =
    jsonWriter[TransactionEffects.AllEffects]
}

object json extends TethysInstances {

  import tethys._
  import tethys.json4s._
  import tethys.writers.tokens.SimpleTokenWriter
  import tethys.readers.tokens.QueueIterator
  import tethys.readers.FieldName
  import org.json4s._

  def json4sFormat[T: tethys.JsonWriter: tethys.JsonReader: Manifest]: CustomSerializer[T] =
    new CustomSerializer[T](
      formats =>
        (
          {
            case j4s =>
              val writer = new SimpleTokenWriter
              j4s.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[T].read(iter)(FieldName())
          }, {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[JValue].read(iter)(FieldName())
          }
      ))

  def json4sKeyFormat[T: tethys.JsonWriter: tethys.JsonReader: Manifest]: CustomKeySerializer[T] =
    new CustomKeySerializer[T](
      formats =>
        (
          {
            case j4s =>
              val writer = new SimpleTokenWriter
              j4s.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[T].read(iter)(FieldName())
          }, {
            case t: T =>
              val writer = new SimpleTokenWriter
              t.writeJson(writer)
              val iter = QueueIterator(writer.tokens)
              tethys.JsonReader[String].read(iter)(FieldName())
          }
      ))
}
