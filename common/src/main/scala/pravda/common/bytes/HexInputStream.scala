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

package pravda.common.bytes

import java.io.InputStream

final class HexInputStream extends InputStream {

  import HexInputStream._

  private var i = 0
  private var hex: CharSequence = ""

  def setHex(hex: CharSequence): Unit = {
    if (hex.length() % 2 != 0)
      throw new IllegalArgumentException(NotHex)
    this.hex = hex
    this.i = 0
  }

  def read(): Int = {
    if (i < hex.length) {
      val l = to16(hex.charAt(i))
      val r = to16(hex.charAt(i + 1))
      if (l == -1 || r == -1)
        throw new IllegalArgumentException(NotHex)
      i += 2
      l << 4 | r
    } else {
      -1
    }
  }
}

object HexInputStream {

  final val NotHex = "This is not hex string"

  def to16(code: Char): Int = {
    if (code > 0x2F && code < 0x3A) code - 0x30 // 0-9
    else if (code > 0x40 && code < 0x47) code - 0x41 + 10 // A-F
    else if (code > 0x60 && code < 0x67) code - 0x61 + 10 // a-f
    else -1
  }
}
