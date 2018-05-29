package pravda.node.data

import java.security.SecureRandom

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{BadPaddingException, Cipher, SecretKeyFactory}
import com.google.protobuf.ByteString
import pravda.common.contrib.ed25519
import pravda.node.data.blockchain.Transaction
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
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

//  object SecurePasswordHash {
//
//    val hasher: String => String =
//      forPassword _ andThen (_.mkString)
//
//    def forPassword(password: String): SecurePasswordHash = {
//      val algorithm = "PBKDF2WithHmacSHA256"
//      val iterations = 20000
//      val random = new SecureRandom()
//      val salt = new Array[Byte](8)
//
//      random.nextBytes(salt)
//
//      val spec = new PBEKeySpec(password.toCharArray, salt, iterations)
//      val f = SecretKeyFactory.getInstance(algorithm)
//
//      SecurePasswordHash(
//        passwordAlgorithm = algorithm,
//        passwordIterations = iterations,
//        passwordSalt = ByteString.copyFrom(salt),
//        passwordHash = ByteString.copyFrom(f.generateSecret(spec).getEncoded)
//      )
//    }
//  }

  def signTransaction(privateKey: PrivateKey, tx: UnsignedTransaction): SignedTransaction =
    signTransaction(privateKey.toByteArray, tx)

  private def signTransaction(privateKey: Array[Byte], tx: UnsignedTransaction): SignedTransaction = {
    val message = transcode(tx.forSignature).to[Bson]
    val signature = ed25519.sign(privateKey, message)
    SignedTransaction(tx.from, tx.program, ByteString.copyFrom(signature), tx.fee, tx.wattPrice, tx.nonce)
  }

  def checkTransactionSignature(tx: SignedTransaction): Option[AuthorizedTransaction] = {

    val pubKey = tx.from.toByteArray
    val message = transcode(tx.forSignature).to[Bson]
    val signature = tx.signature.toByteArray

    if (ed25519.verify(pubKey, message, signature)) {
      Some(AuthorizedTransaction(tx.from, tx.program, tx.signature, tx.fee, tx.wattPrice, tx.nonce))
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
