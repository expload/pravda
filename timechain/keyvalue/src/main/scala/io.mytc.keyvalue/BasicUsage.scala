package io.mytc.keyvalue

import io.mytc.keyvalue.serialyzer._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object BasicUsage {

  import hash.utils._
  import scala.concurrent.ExecutionContext.Implicits.global

  def printHash(str: String = ""): Unit = {
    println(s"$str: ${hex(db.stateHash)}")
  }

  implicit val strKeyWriter = new KeyWriter[String] {
    override def toBytes(value: String): Array[Byte] = ByteWriter.write(value)
  }
  implicit val strValueWriter = new ValueWriter[String] {
    override def toBytes(value: String): Array[Byte] = ByteWriter.write(value)
  }
  implicit val strValueReader= new ValueReader[String] {
    override def fromBytes(value: Array[Byte]): String = ByteReader.read[String](value)
  }

  val db = DB("/tmp/testdb")

  val f = for {
    _ <- db.put("newkey", "newvalue2")
    v <- db.getAs[String]("newkey")
  } yield(v)
  println(Await.result(f, Duration.Inf))

}

