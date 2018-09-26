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

package pravda.common

import com.google.protobuf.ByteString
import pravda.common.contrib.ed25519

object crypto {

  /**
    * Generates ed25519 key pair.
    * @param randomBytes64 base random 64 bytes
    * @return (pub[32], sec[64])
    */
  def ed25519KeyPair(randomBytes64: ByteString): (ByteString, ByteString) = {
    // FIXME use Address and PrivateKey tagged types
    val sec = randomBytes64.toByteArray
    val pub = new Array[Byte](32)
    ed25519.generateKey(pub, sec)
    (ByteString.copyFrom(pub), ByteString.copyFrom(sec))
  }
}
