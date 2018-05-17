package io.mytc.timechain

import java.io.{File, FileOutputStream, PrintWriter}
import com.google.protobuf.ByteString
import io.mytc.timechain.contrib.ripemd160
import io.mytc.timechain.data.TimechainConfig
import io.mytc.timechain.data.common.Address
import io.mytc.timechain.utils.{CpuArchitecture, OperationSystem}
import io.mytc.timechain.data.serialization._
import json._

import scala.concurrent.{ExecutionContext, Future}

object tendermint {

  private final val GoWireAddressHeader =
    ByteString.copyFrom(Array[Byte](0x01, 0x01, 0x20))

  def unpackAddress(pubKey: ByteString): Address =
    Address(pubKey.substring(1))

  /**
    * Pack address (pubkey) in tendermint's inner go-wire format
    */
  def packAddress(address: Address): ByteString =
    GoWireAddressHeader.concat(address)

  def selectTendermintExecutable(name: OperationSystem, arch: CpuArchitecture) =
    s"tendermint_${name.toString.toLowerCase}_$arch"

  def run(config: TimechainConfig)(implicit ec: ExecutionContext): Future[Process] = {

    def writeFile(file: File)(s: String): Unit = {
      val pw = new PrintWriter(file)
      try {
        pw.write(s)
      } finally {
        pw.close()
      }
    }

    def createExecutableFile(os: OperationSystem, dir: File) =
      new File(dir, os match {
        case OperationSystem.Windows => "tendermint.exe"
        case _                       => "tendermint"
      })

    def copyFromResources(resName: String, dest: File): Unit = {
      def copy(in: java.io.InputStream, out: java.io.OutputStream): Unit = {
        var cnt = 0
        val arr = new Array[Byte](1024 * 32)
        cnt = in.read(arr)
        while (cnt != -1) {
          out.write(arr, 0, cnt)
          cnt = in.read(arr)
        }
      }
      val fileOut = Option(new FileOutputStream(dest))
      val resourceInput = Option(getClass.getClassLoader.getResourceAsStream(resName))
      println("copy from " + resName)
      println("copy to " + dest)
      try {
        for (in ← resourceInput; out ← fileOut) {
          copy(in, out)
          out.flush()
        }
      } finally {
        fileOut.foreach { s ⇒
          s.close()
        }
        resourceInput.foreach { s ⇒
          s.close()
        }
      }
    }

    for {
      os <- utils.detectOperationSystem()
      cpu <- utils.detectCpuArchitecture()
    } yield {

      if (os == OperationSystem.Unsupported) {
        throw new RuntimeException("Failed to determine OS type")
      }

      if (cpu == CpuArchitecture.Unsupported) {
        throw new RuntimeException("Failed to determine CPU type")
      }

      val configDir = new File(config.dataDirectory, "config")

      if (!configDir.exists())
        configDir.mkdirs()
      // Make executable
      val executable = createExecutableFile(os, config.dataDirectory)
      if (!executable.exists()) {
        val resName = selectTendermintExecutable(os, cpu)
        copyFromResources(resName, executable)
      }
      executable.setExecutable(true)
      // Make genesis
      val genesisFile = new File(configDir, "genesis.json")
      if (!genesisFile.exists()) {
        val genesisConf = transcode(config.genesis).to[Json]
        writeFile(genesisFile)(genesisConf)
      }
      // Make priv_validator
      val privValidatorFile = new File(configDir, "priv_validator.json")
      if (config.isValidator && !privValidatorFile.exists()) {
        writeFile(privValidatorFile) {
          val pubKey = utils.bytes2hex(config.paymentWallet.address)
          val privKey = utils.bytes2hex(config.paymentWallet.privateKey)
          val address = {
            val withType = packAddress(config.paymentWallet.address)
            val hash = ripemd160.getHash(withType.toByteArray)
            utils.bytes2hex(hash)
          }
          s"""
            |{
            |  "address": "$address",
            |  "pub_key": {
            |    "type": "ed25519",
            |    "data": "$pubKey"
            |  },
            |  "last_height": 0,
            |  "last_round": 0,
            |  "last_step": 0,
            |  "last_signature": null,
            |  "priv_key": {
            |    "type": "ed25519",
            |    "data": "$privKey"
            |  }
            |}
          """.stripMargin
        }
      }
//      val tmDataDir = new File(config.dataDirectory, "tm_data")
//      if (!tmDataDir.exists()) tmDataDir.mkdir()
//      val cswal = new File(tmDataDir, "cswal")
//      println(tmpDir)

      val proxyAppAddr = if (!config.tendermint.useUnixDomainSocket) {
        s"tcp://127.0.0.1:${config.tendermint.proxyAppPort}"
      } else {
        s"unix://${new File(config.dataDirectory, "abci.sock").getAbsolutePath}"
      }

      val tomlConfig =
        s"""
           |log_level="state:info,*:debug"
           |abci="socket"
           |proxy_app = "${proxyAppAddr}"
           |[consensus]
           |create_empty_blocks = false
           |[p2p]
           |addr_book_strict=false
           |seeds="${config.seeds}"
           |laddr="tcp://0.0.0.0:${config.tendermint.peerPort}"
           |[rpc]
           |laddr="tcp://0.0.0.0:${config.tendermint.rpcPort}"
         """.stripMargin
      writeFile(new File(configDir, "config.toml"))(tomlConfig)
      new ProcessBuilder(executable.getAbsolutePath, "--home", ".", "node")
        .directory(config.dataDirectory)
        .inheritIO()
        .start()
    }
  }
}
