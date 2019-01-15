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

package pravda.node.data

import java.security.SecureRandom

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{BadPaddingException, Cipher, SecretKeyFactory}
import com.google.protobuf.ByteString
import pravda.common.contrib.ed25519
import pravda.node.data.blockchain.Transaction
import pravda.node.data.serialization.json._
import pravda.node.data.serialization.bjson._
import pravda.node.data.serialization._
import pravda.common.bytes._
import pravda.common.domain.Address
import supertagged.TaggedType

object cryptography {

  import Transaction._

  object PrivateKey extends TaggedType[ByteString] {

    def fromHex(hex: String): PrivateKey =
      PrivateKey(ByteString.copyFrom(hex2bytes(hex)))
  }
  type PrivateKey = PrivateKey.Type

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

  def signTransaction(privateKey: PrivateKey, tx: UnsignedTransaction): SignedTransaction =
    signTransaction(privateKey.toByteArray, tx)

  def addWattPayerSignature(privateKey: PrivateKey, tx: SignedTransaction): SignedTransaction = {
    val message = transcode(tx.forSignature).to[BJson]
    val signature = ed25519.sign(privateKey.toByteArray, message)
    tx.copy(wattPayerSignature = Some(ByteString.copyFrom(signature)))
  }

  private def signTransaction(privateKey: Array[Byte], tx: UnsignedTransaction): SignedTransaction = {
    val message = transcode(tx.forSignature).to[BJson]
    val signature = ed25519.sign(privateKey, message)

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
    val message = transcode(tx.forSignature).to[BJson]
    val signature = tx.signature.toByteArray
    if (ed25519.verify(pubKey, message, signature)) {
      (tx.wattPayer, tx.wattPayerSignature) match {
        case (Some(wattPayer), Some(wattPayerSignature)) =>
          if (ed25519.verify(wattPayer.toByteArray, message, wattPayerSignature.toByteArray)) authorizedTransaction
          else None
        case (None, _) => authorizedTransaction
        case _         => None
      }
    } else {
      None
    }
  }

  def generateKeyPair(): (Address, PrivateKey) = {
    val secureRandom = new SecureRandom()
    val privateKeyBytes = new Array[Byte](64)
    val publicKeyBytes = new Array[Byte](32)
    secureRandom.nextBytes(privateKeyBytes)
    ed25519.generateKey(publicKeyBytes, privateKeyBytes)
    val privateKey = PrivateKey(ByteString.copyFrom(privateKeyBytes))
    val address = Address(ByteString.copyFrom(publicKeyBytes))

    (address, privateKey)
  }

  def generateKeyPair(password: String): (Address, EncryptedPrivateKey) = {
    val (address, priv) = generateKeyPair()
    val epk = encryptPrivateKey(password, priv)
    (address, epk)
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
