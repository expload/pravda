package io.mytc.keyvalue

import io.mytc.keyvalue.serialyzer._

import scala.concurrent.duration.Duration
import scala.concurrent.Await

object BasicUsage {

  import hash.utils._
  import scala.concurrent.ExecutionContext.Implicits.global

  def printHash(str: String = ""): Unit = {
    println(s"$str: ${hex(db.stateHash)}")
  }

  implicit val strKeyWriter: KeyWriter[String] = (value: String) => ByteWriter.stringWriter.toBytes(value)
  implicit val strValueWriter: ValueWriter[String] = (value: String) => ByteWriter.stringWriter.toBytes(value)
  implicit val strValueReader: ValueReader[String] = (value: Array[Byte]) => ByteReader.stringReader.fromBytes(value)

  val db = DB("/tmp/testdb")

  val f = for {
    _ <- db.put("newkey", "newvalue2")
    v <- db.getAs[String]("newkey")
  } yield(v)
  println(Await.result(f, Duration.Inf))

}

