package io.mytc.timechain.data.serialization

import com.google.protobuf.ByteString
import reactivemongo.bson.{BSONDocument, BSONValue}
import reactivemongo.bson.buffer.{ArrayBSONBuffer, ArrayReadableBuffer}
import supertagged.{Tagged, lifterF}

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

  import bsonpickle.default.{Writer => BsonWriter, Reader => BsonReader}

  // Tagged types
  implicit def taggedReaderLifter[T: BsonReader, U]: BsonReader[Tagged[T, U]] =
    lifterF[BsonReader].lift[T, U]

  implicit def taggedWriterLifter[T: BsonWriter, U]: BsonWriter[Tagged[T, U]] =
    lifterF[BsonWriter].lift[T, U]

  // Protobuf
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
