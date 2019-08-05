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
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{BadPaddingException, Cipher, SecretKeyFactory}
import pravda.common.bytes._
import pravda.common.data.blockchain.Transaction
import pravda.common.data.blockchain._
import pravda.common.serialization._
import pravda.common.serialization.protobuf._

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.math.ec.rfc8032.Ed25519
import pravda.common.data.blockchain._

object cryptography {

  import Transaction._

  final case class EncryptedPrivateKey(
      keyEncryptedData: ByteString,
      keyIv: ByteString,
      keySalt: ByteString
  )

  final case class SecurePasswordHash(
      passwordAlgorithm: String,
      passwordIterations: Int,
      passwordSalt: ByteString,
      passwordHash: ByteString
  ) {

    def mkString: String = {
      val hashString = byteString2hex(passwordHash)
      val saltString = byteString2hex(passwordSalt)
      s"$passwordAlgorithm:$passwordIterations:$saltString:$hashString"
    }
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

  def generateKeyPair(): (Address, PrivateKey) = {
    val secureRandom = new SecureRandom()
    val privateKeySource = new Array[Byte](64)
    secureRandom.nextBytes(privateKeySource)
    generateKeyPair(ByteString.copyFrom(privateKeySource))
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

  def signTransaction(privateKey: PrivateKey, tx: UnsignedTransaction): SignedTransaction =
    signTransaction(privateKey.toByteArray, tx)

  def addWattPayerSignature(privateKey: PrivateKey, tx: SignedTransaction): SignedTransaction = {
    val message = transcode(tx.forSignature).to[Protobuf]
    val signature = sign(privateKey.toByteArray, message)
    tx.copy(wattPayerSignature = Some(ByteString.copyFrom(signature)))
  }

  private def signTransaction(privateKey: Array[Byte], tx: UnsignedTransaction): SignedTransaction = {
    val message = transcode(tx.forSignature).to[Protobuf]
    val signature = sign(privateKey, message)

    SignedTransaction(
      tx.from,
      tx.program,
      ByteString.copyFrom(signature),
      tx.wattLimit,
      tx.wattPrice,
      tx.wattPayer,
      None,
      tx.nonce
    )
  }

  def checkTransactionSignature(tx: SignedTransaction): Option[AuthorizedTransaction] = {

    lazy val authorizedTransaction = Some(
      AuthorizedTransaction(
        tx.from,
        tx.program,
        tx.signature,
        tx.wattLimit,
        tx.wattPrice,
        tx.wattPayer,
        tx.wattPayerSignature,
        tx.nonce
      )
    )

    val pubKey = tx.from.toByteArray
    val message = transcode(tx.forSignature).to[Protobuf]
    val signature = tx.signature.toByteArray
    if (verify(pubKey, message, signature)) {
      (tx.wattPayer, tx.wattPayerSignature) match {
        case (Some(wattPayer), Some(wattPayerSignature)) =>
          if (verify(wattPayer.toByteArray, message, wattPayerSignature.toByteArray)) authorizedTransaction
          else None
        case (None, _) => authorizedTransaction
        case _         => None
      }
    } else {
      None
    }
  }

  def encryptPrivateKey(password: String, privateKey: PrivateKey): EncryptedPrivateKey =
    encryptPrivateKey(password, privateKey.toByteArray)

  def encryptPrivateKey(password: String, privateKey: Array[Byte]): EncryptedPrivateKey = {

    val ivSize = 8
    val salt = new Array[Byte](ivSize)
    val random = new SecureRandom()

    random.nextBytes(salt)

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secret = secretFromPassword(password, salt)

    cipher.init(Cipher.ENCRYPT_MODE, secret)

    val params = cipher.getParameters
    val iv = params.getParameterSpec(classOf[IvParameterSpec]).getIV
    val ciphertext = cipher.doFinal(privateKey)

    EncryptedPrivateKey(
      keyEncryptedData = ByteString.copyFrom(ciphertext),
      keyIv = ByteString.copyFrom(iv),
      keySalt = ByteString.copyFrom(salt)
    )
  }

  def decryptPrivateKey(epk: EncryptedPrivateKey, password: String): Option[PrivateKey] = {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val secret = secretFromPassword(password, epk.keySalt.toByteArray)
    val ciphertext = epk.keyEncryptedData.toByteArray

    try {
      cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(epk.keyIv.toByteArray))
      Some(PrivateKey(ByteString.copyFrom(cipher.doFinal(ciphertext))))
    } catch {
      case _: BadPaddingException => None
    }
  }

  def encryptData(byteString: ByteString): (PrivateKey, ByteString) = {
    import javax.crypto.KeyGenerator
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256)
    val secretKey = keyGen.generateKey
    println(s"secretKey.getAlgorithm=${secretKey.getAlgorithm},secretKey.getFormat=${secretKey.getFormat}")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encodedKey = ByteString.copyFrom(secretKey.getEncoded)
    val encryptedData = cipher.doFinal(byteString.toByteArray)
    (PrivateKey @@ encodedKey, ByteString.copyFrom(encryptedData))
  }

  private def secretFromPassword(password: String, salt: Array[Byte]) = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(password.toArray, salt, 65536, 256)
    val tmp = factory.generateSecret(spec)
    new SecretKeySpec(tmp.getEncoded, "AES")
  }
}
