package io.mytc.keyvalue.hash

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object utils {
  val DIGEST = MessageDigest.getInstance("SHA-256")

  def hex(arr: Array[Byte]) = String.format("%032x", new BigInteger(1, arr))

  def hash(value: Array[Byte]): Array[Byte] = DIGEST.digest(value)

  def hash(value: String, charset: String = StandardCharsets.UTF_8.name()): Array[Byte] =
    hash(value.getBytes(charset))

  def hashPair(key: Array[Byte], value: Array[Byte]): Array[Byte] = {
    hash(hash(key) ++ hash(value))
  }

  def hashPair(key: String, value: String): Array[Byte] = {
    hash(hash(key) ++ hash(value))
  }

  def xor(arr1: Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    arr1.zip(arr2).map {
      case (el1, el2) =>
        (el1 ^ el2).toByte
    }
  }

  lazy val zeroHash = Array.fill[Byte](DIGEST.getDigestLength)(0.toByte)

}
