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

import java.security.SecureRandom

import com.google.protobuf.ByteString
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.math.ec.rfc8032.Ed25519
import pravda.common.domain.{Address, PrivateKey}

object crypto {

  def generateKeyPair(): (Address, PrivateKey) = {
    val secureRandom = new SecureRandom()
    val privateKeySource = new Array[Byte](64)
    secureRandom.nextBytes(privateKeySource)
    generateKeyPair(ByteString.copyFrom(privateKeySource))
  }

  /**
    * Generates ed25519 key pair.
    * @param privateKeySource base random 64 bytes
    * @return (pub[32], sec[64])
    */
  def generateKeyPair(privateKeySource: ByteString): (Address, PrivateKey) = {
    // FIXME use Address and PrivateKey tagged types
//    val privateKey = randomBytes64.toByteArray
    val pk = new Ed25519PrivateKeyParameters(privateKeySource.toByteArray, 0)
    val publicKey = pk.generatePublicKey.getEncoded
//    System.arraycopy(pk.generatePublicKey.getEncoded, 0, publicKey, 0, 32)
//    System.arraycopy(publicKey, 0, privateKey, 32, 32)

    (Address(ByteString.copyFrom(publicKey)), PrivateKey(ByteString.copyFrom(pk.getEncoded.slice(0, 32) ++ publicKey)))
  }

  /**
    * Sign signs the message with privateKey and returns a signature.
    *
    * @param privateKey copy of private key 64 bytes length. array will be mutated
    * @param message    arbitrary length message
    */
  def sign(privateKey: Array[Byte], message: Array[Byte]): Array[Byte] = {
    val sig = new Array[Byte](Ed25519.SIGNATURE_SIZE)
    Ed25519.sign(privateKey, 0, message, 0, message.length, sig, 0)
    sig
  }

  def verify(publicKey: Array[Byte], message: Array[Byte], sig: Array[Byte]): Boolean =
    Ed25519.verify(sig, 0, publicKey, 0, message, 0, message.length)
}
