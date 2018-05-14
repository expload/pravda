package io.mytc.keyvalue

import java.io.File

import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.scalatest._
import hash.utils._
import io.mytc.keyvalue.serialyzer._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DiffSpec extends FlatSpec with Matchers {

  behavior of "DB.diff"

  val path = "test"

  implicit val stringKeyWriter = new KeyWriter[String] {
    override def toBytes(value: String): Array[Byte] = ByteWriter.stringWriter.toBytes(value)
  }

  implicit val stringWriter = new ValueWriter[String] {
    override def toBytes(value: String): Array[Byte] = ByteWriter.stringWriter.toBytes(value)
  }
  implicit val stringReader = new ValueReader[String] {
    override def fromBytes(array: Array[Byte]): String = ByteReader.stringReader.fromBytes(array)
  }


  def test(body: DB => Unit) = {
    val db: DB = DB(path, true)
    try {
      body(db)
    } finally {
      db.close()
      val options = new Options
      factory.destroy(new File(path), options)
    }
  }


  it should "should work correct in batch case" in {
    test { db =>
      val putOps = List(
        Operation.Put("test", "test")
      )
      hex(db.batchDiff(putOps:_*)) shouldNot be(hex(zeroHash))

      val putDelOps = List(
        Operation.Put("test", "test"),
        Operation.Delete("test")
      )
      hex(db.batchDiff(putDelOps:_*)) shouldBe hex(zeroHash)

      val putDelPutOps = List(
        Operation.Put("test", "test"),
        Operation.Delete("test"),
        Operation.Put("test", "test")
      )
      hex(db.batchDiff(putDelPutOps:_*)) shouldNot be(hex(zeroHash))

      hex(db.batchDiff(putDelPutOps:_*)) shouldBe hex(db.batchDiff(putOps:_*))

    }
  }

  it should "work in case of emty byte array" in {
    test {
      db =>
        hex(db.putDiff(Array.empty[Byte], Array.empty[Byte])) shouldNot be(hex(zeroHash))
        hex(db.putDiff(Array.empty[Byte], stringWriter.toBytes("test"))) shouldNot be(hex(db.putDiff(stringWriter.toBytes("test"), Array.empty[Byte])))
    }
  }


  it should "work in case of put and delete" in {
    test {
      db =>
        hex(db.stateHash) shouldBe hex(zeroHash)
        Await.ready(db.put("test", "shmest"), 1 seconds)
        val putHash = db.stateHash
        hex(putHash) shouldNot be(hex(zeroHash))
        hex(putHash) shouldBe hex(db.deleteDiff(stringKeyWriter.toBytes("test")))
        Await.ready(db.delete("test"), 1 seconds)
        val deleteHash = db.stateHash
        hex(deleteHash) shouldBe hex(zeroHash)
    }
  }

}
