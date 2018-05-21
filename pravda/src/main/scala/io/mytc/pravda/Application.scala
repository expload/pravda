package io.mytc.pravda

object Application {

  final case class Config(
      generateKeyPair: Boolean = false
  )

  def genHexKeyPair(): (String, String) = {
    import java.security.SecureRandom
    import io.mytc.pravda.contrib.ed25519
    val secureRandom = new SecureRandom()
    val secKey = new Array[Byte](64)
    val pubKey = new Array[Byte](32)
    secureRandom.nextBytes(secKey)
    ed25519.generateKey(pubKey, secKey)
    val secHexKey = secKey.map("%02X".format(_)).mkString
    val pubHexKey = pubKey.map("%02X".format(_)).mkString
    (secHexKey, pubHexKey)
  }

  def run(config: Config): Unit = {
    if (config.generateKeyPair) {
      val (sec, pub) = genHexKeyPair()
      print("public key: ")
      println(pub)
      print("secret key: ")
      println(sec)
    }
  }

  def main(argv: Array[String]): Unit = {

    val optParser = new scopt.OptionParser[Config]("scopt") {
      head("MyTime SDK", "0.0.1")

      opt[Unit]('g', "genkey")
        .action { (_, c) =>
          c.copy(generateKeyPair = true)
        }
        .text("Generate ED25519 key pair")

      help("help").text("Unified CLI tool")
    }

    optParser.parse(argv, Config()) match {
      case Some(config) => run(config)
      case None         => ()
    }
  }

}
