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
import pravda.vm.Data
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer}
import reactivemongo.bson.{BSONDocument, BSONValue}
import supertagged.{Tagged, lifterF}

import scala.util.Success

object bson extends BsonTranscoder

trait BsonTranscoder {
  // --- MAIN PART -------------
  type BsonEncoder[T] = Transcoder[T, Bson]
  type BsonDecoder[T] = Transcoder[Bson, T]

  implicit def bsonEncoder[T](implicit pickle: bsonpickle.default.Writer[T]): BsonEncoder[T] = (value: T) => {
    val pickled = pickle.write(value)
    val doc = pickled match {
      case d: BSONDocument => d
      case o               => BSONDocument("" -> o)
    }
    val buffer = new ArrayBSONBuffer()
    BSONDocument.write(doc, buffer)
    Bson @@ buffer.array
  }

  implicit def bsonDecoder[T](implicit pickle: bsonpickle.default.Reader[T]): BsonDecoder[T] = (bson: Bson) => {
    val doc = BSONDocument.read(ArrayReadableBuffer(bson))
    val value = if (doc.elements.head.name.isEmpty) doc.elements.head.value else doc
    pickle.read(value)
  }
  //-----------------------

  import bsonpickle.default.{Reader => BsonReader, Writer => BsonWriter}

  // Tagged types

  implicit def taggedReaderLifter[T: BsonReader, U]: BsonReader[Tagged[T, U]] =
    lifterF[BsonReader].lift[T, U]

  implicit def taggedWriterLifter[T: BsonWriter, U]: BsonWriter[Tagged[T, U]] =
    lifterF[BsonWriter].lift[T, U]

  // Data

  implicit def dataWriter(implicit arrayWriter: BsonWriter[Array[Byte]]): BsonWriter[Data] =
    new BsonWriter[Data] {
      override def write0: Data => BSONValue = { x =>
        arrayWriter.write0(x.toByteString.toByteArray)
      }
    }

  implicit val dataReader: BsonReader[Data] =
    new BsonReader[Data] {
      private val arrayReader = implicitly[BsonReader[Array[Byte]]]
      override def read0: PartialFunction[BSONValue, Data] = {
        case x if arrayReader.read0.isDefinedAt(x) => Data.fromBytes(arrayReader.read0(x))
      }
    }

  // Protobuf

  // fomkin: I dont get why this reader is not derived by autoderivation
  // Looks like it some kind of bug in implicit macros.
  implicit val mapRefDataReader: BsonReader[Map[Data.Primitive.Ref, Data]] = {
    new BsonReader[Map[Data.Primitive.Ref, Data]] {
      override def read0: PartialFunction[BSONValue, Map[Data.Primitive.Ref, Data]] = {
        case BSONDocument(xs) =>
          xs.collect {
            case Success(x) =>
              val k = Data.Primitive.Ref(x.name.substring(x.name.indexOf(':') + 1).toInt)
              val v = dataReader.read(x.value)
              k -> v
          }.toMap
      }
    }
  }

  implicit def protoWriter(implicit arrayWriter: BsonWriter[Array[Byte]]): BsonWriter[ByteString] =
    new BsonWriter[ByteString] {
      override def write0: ByteString => BSONValue = { x =>
        arrayWriter.write0(x.toByteArray)
      }
    }

  implicit def protoReader(implicit arrayReader: BsonReader[Array[Byte]]): BsonReader[ByteString] =
    new BsonReader[ByteString] {
      override def read0: PartialFunction[BSONValue, ByteString] = {
        case x if arrayReader.read0.isDefinedAt(x) => ByteString.copyFrom(arrayReader.read0(x))
      }
    }

}
