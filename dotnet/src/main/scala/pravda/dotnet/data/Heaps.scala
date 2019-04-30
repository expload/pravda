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

package pravda.dotnet.data

import fastparse.byte.all._
import pravda.dotnet.utils._

// See http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf p.272
object Heaps {
  private val blobBytes: P[Bytes] = P(Int8).flatMap(b => {
    if ((b & (1 << 7)) == 0) {
      val size = b.toInt
      AnyBytes(size).!
    } else if ((b & (1 << 6)) == 0) {
      P(Int8).flatMap(x => {
        val size = (x & 0xff) + ((b & 0x3f) << 8)
        AnyBytes(size).!
      })
    } else {
      P(Int8 ~ Int8 ~ Int8).flatMap {
        case (x, y, z) =>
          val size = (z & 0xff) + ((y & 0xff) << 8) + ((z & 0xff) << 16) + ((b & 0x1f) << 24)
          AnyBytes(size).!
      }
    }
  })

  val compressedUInt: P[Int] = P(Int8).flatMap(b => {
    if ((b & 0x80) == 0) {
      PassWith(b.toInt)
    } else if ((b & 0x40) == 0) {
      P(Int8).map(b2 => ((b & 0x3F) << 8) | (b2 & 0xff))
    } else {
      P(Int8 ~ Int8 ~ Int8).map {
        case (b2, b3, b4) => ((b & 0x1F) << 24) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 8) | (b4 & 0xff)
      }
    }
  })

  val compressedInt: P[Int] = P(Int8).flatMap(b => {
    if ((b & 0x80) == 0) {
      PassWith(((b & 0x7F) >> 1) | ((-(b & 1)) & 0xFFFFFFC0)) // Oh sh*t, I'm sorry!
    } else if ((b & 0x40) == 0) {
      P(Int8)
        .map(b2 => ((b & 0x3F) << 7) | ((b2 & 0xFE) >> 1) | ((-(b2 & 1)) & 0xFFFFE000)) // Sorry for what?
    } else {
      P(Int8 ~ Int8 ~ Int8).map {
        case (b2, b3, b4) =>
          ((b & 0x1F) << 23) | ((b2 & 0xff) << 15) | ((b3 & 0xff) << 7) | ((b4 & 0xFE) >> 1) | ((-(b4 & 1)) & 0xF0000000)
        // Our spec taught us not to be ashamed of our codded ints, 'specially since they're such good size and all
      }
    }
  }) // FIXME we really want to have tests here

  final case class SequencePoint(ilOffset: Int,
                                 startLine: Int,
                                 startColumn: Int,
                                 endLine: Int,
                                 endColumn: Int
                                 /* documentIdx is ignored */ )

  val sequencePoints: P[List[SequencePoint]] = {
    val header = P(compressedUInt).map(_ => ())
    def sequencePointRecord(first: Boolean, firstNonHidden: Boolean) =
      for {
        deltaIlOffset <- compressedUInt
        isDocumentRecord = !first && deltaIlOffset == 0
        _ <- if (isDocumentRecord) compressedUInt else PassWith(0)
        deltaLines <- if (!isDocumentRecord) compressedUInt else PassWith(0)
        deltaColumns <- if (!isDocumentRecord) {
          if (deltaLines == 0) compressedUInt else compressedInt
        } else PassWith(0)
        isHiddenSeqPoint = deltaLines == 0 && deltaColumns == 0
        deltaStartLine <- if (!isHiddenSeqPoint) {
          if (firstNonHidden) compressedUInt else compressedInt
        } else PassWith(0)
        deltaStartColumn <- if (!isHiddenSeqPoint) {
          if (firstNonHidden) compressedUInt else compressedInt
        } else PassWith(0)
      } yield (deltaIlOffset, deltaLines, deltaColumns, deltaStartLine, deltaStartColumn)

    def go(first: Boolean,
           firstNonHidden: Boolean,
           curIlOffset: Int,
           curStartLine: Int,
           curStartColumn: Int): P[List[SequencePoint]] =
      sequencePointRecord(first, firstNonHidden).?.flatMap {
        case Some((deltaIlOffset, deltaLines, deltaColumns, deltaStartLine, deltaStartColumn)) =>
          if (!first && deltaIlOffset == 0) {
            go(false, firstNonHidden, curIlOffset, curStartLine, curStartColumn)
          } else {
            val newDeltaIlOffset = curIlOffset + deltaIlOffset
            if (deltaLines == 0 && deltaColumns == 0) {
              go(false, firstNonHidden, newDeltaIlOffset, curStartLine, curStartColumn)
            } else {
              val newStartLine = curStartLine + deltaStartLine
              val newStartColumn = curStartColumn + deltaStartColumn
              val sequencePoint =
                SequencePoint(newDeltaIlOffset,
                              newStartLine,
                              newStartColumn,
                              newStartLine + deltaLines,
                              newStartColumn + deltaColumns)
              go(false, false, newDeltaIlOffset, newStartLine, newStartColumn).map(sequencePoint :: _)
            }
          }
        case None => PassWith(List.empty)
      }

    header ~ go(true, true, 0, 0, 0)
  }

  val documentName: P[(String, List[Int])] = {
    val sep = P(AnyBytes(1).!).map(bs => if (bs(0) == 0) "" else new String(bs.toArray, "UTF-8"))
    val parts = P(compressedUInt.rep).map(_.toList)
    sep ~ parts
  }

  val blobUtf8: P[String] = P(AnyByte.rep.!).map(bs => new String(bs.toArray, "UTF-8"))

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
