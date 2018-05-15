package io.mytc.timechain.data

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

  def transcode[From](value: From): TranscodingDsl[From] = new TranscodingDsl(value)
}
