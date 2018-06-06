package pravda.dotnet.data

import fastparse.byte.all._
import pravda.dotnet.utils._

object Heaps {
  private val blobBytes: P[Bytes] = P(Int8).flatMap(b => {
    if ((b & (1 << 7)) == 0) {
      val size = b.toInt
      AnyBytes(size).!
    } else if ((b & (1 << 6)) == 0) {
      P(Int8).flatMap(x => {
        val size = x + ((b & 0x3f) << 8)
        AnyBytes(size).!
      })
    } else {
      P(Int8 ~ Int8 ~ Int8).flatMap {
        case (x, y, z) =>
          val size = z + (y << 8) + (z << 16) + ((b & 0x1f) << 24)
          AnyBytes(size).!
      }
    }
  })

  def blob(blobHeap: Bytes, idx: Long): Either[String, Bytes] = {
    blobBytes.parse(blobHeap, idx.toInt).toEither
  }

  def string(stringHeap: Bytes, idx: Long): Either[String, String] =
    nullTerminatedString.parse(stringHeap, idx.toInt).toEither

  def userString(userStringHeap: Bytes, idx: Long): Either[String, String] =
    blobBytes
      .map(bs => new String(bs.dropRight(1L).toArray, "UTF-16LE"))
      .parse(userStringHeap, idx.toInt)
      .toEither

}
