package pravda.cli

object Main {

//  def genHexKeyPair(): (String, String) = {
//    import java.security.SecureRandom
//    import io.mytc.pravda.contrib.ed25519
//    val secureRandom = new SecureRandom()
//    val secKey = new Array[Byte](64)
//    val pubKey = new Array[Byte](32)
//    secureRandom.nextBytes(secKey)
//    ed25519.generateKey(pubKey, secKey)
//    val secHexKey = secKey.map(x => (x & 0xFF).toHexString).mkString
//    val pubHexKey = pubKey.map(x => (x & 0xFF).toHexString).mkString
//    (secHexKey, pubHexKey)
//  }
//
//  def run(config: Config): Unit = {
//    if (config.generateKeyPair) {
//      val (sec, pub) = genHexKeyPair()
//      print("public key: ")
//      println(pub)
//      print("secret key: ")
//      println(sec)
//    }
//  }

//  def main(argv: Array[String]): Unit = {
//
//    optParser.parse(argv, Config()) match {
//      case Some(config) => run(config)
//      case None         => ()
//    }
//  }

}
