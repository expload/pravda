package pravda.vm

import pravda.vm
import tethys._
import tethys.derivation.semiauto.{jsonReader, jsonWriter}
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.writers.tokens.TokenWriter
import pravda.common.json._

trait TethysInstances {

  //----------------------------------------------------------------------
  // vm.Data support for tethys
  //----------------------------------------------------------------------

  implicit val dataReader: JsonReader[Data] =
    JsonReader.stringReader.map(s => Data.parser.all.parse(s).get.value)

  implicit val dataWriter: JsonWriter[Data] =
    JsonWriter.stringWriter.contramap(_.mkString())

  implicit val primitiveReader: JsonReader[Data.Primitive] =
    JsonReader.stringReader.map(s => Data.parser.primitive.parse(s).get.value)

  implicit val primitiveWriter: JsonWriter[Data.Primitive] =
    JsonWriter.stringWriter.contramap(_.mkString())

  implicit val primitiveRefReader: JsonReader[Data.Primitive.Ref] =
    JsonReader.stringReader.map(s => Data.parser.ref.parse(s).get.value)

  implicit val primitiveRefWriter: JsonWriter[Data.Primitive.Ref] =
    JsonWriter.stringWriter.contramap(_.mkString())

  implicit val primitiveBytesReader: JsonReader[Data.Primitive.Bytes] =
    JsonReader.stringReader.map(s => Data.parser.bytes.parse(s).get.value)

  implicit val primitiveBytesWriter: JsonWriter[Data.Primitive.Bytes] =
    JsonWriter.stringWriter.contramap(_.mkString())

  implicit val primitiveBigIntReader: JsonReader[Data.Primitive.BigInt] =
    JsonReader.stringReader.map(s => Data.parser.bigint.parse(s).get.value)

  implicit val primitiveBigIntWriter: JsonWriter[Data.Primitive.BigInt] =
    JsonWriter.stringWriter.contramap(_.mkString())

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
