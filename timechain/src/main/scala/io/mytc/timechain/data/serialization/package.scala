package io.mytc.timechain.data

import java.nio.ByteBuffer

import boopickle.Default.{Pickle, Unpickle}
import boopickle.{BufferPool, Pickler}
import com.google.protobuf.ByteString
import supertagged.TaggedType

/**
  * Unified interface for binary and json encoding/decoding
  */
package object serialization {

  object Json extends TaggedType[String]
  type Json = Json.Type

  object BooPickle extends TaggedType[Array[Byte]]
  type BooPickle = BooPickle.Type

  object Bson extends TaggedType[Array[Byte]]
  type Bson = Bson.Type

  object Composite extends TaggedType[Array[Byte]]
  type Composite = Composite.Type

  trait Transcoder[From, To] extends (From => To)

  final class TranscodingDsl[From](val value: From) extends AnyVal {
    def to[To](implicit transcoder: Transcoder[From, To]): To =
      transcoder(value)
  }

  def transcode[From](value: From): TranscodingDsl[From] = new TranscodingDsl(value)
}
