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

package pravda.node

import java.io.{File, FileOutputStream, PrintWriter}

import com.google.protobuf.ByteString
import pravda.common.contrib.ripemd160
import pravda.node.data.PravdaConfig
import pravda.node.utils.{CpuArchitecture, OperationSystem}
import pravda.node.data.serialization._
import json._
import pravda.common.domain.Address

import scala.concurrent.{ExecutionContext, Future}
import pravda.common.{bytes => byteUtils}

object tendermint {

  val PubKeyEd25519 = "tendermint/PubKeyEd25519"
  val PrivKeyEd25519 = "tendermint/PrivKeyEd25519"

  private final val GoWireAddressHeader =
    ByteString.copyFrom(Array[Byte](0x01, 0x01, 0x20))

  def unpackAddress(pubKey: ByteString): Address =
    Address(pubKey.substring(1))

  /**
    * Pack address (pubkey) in tendermint's inner go-wire format
    */
  def packAddress(address: Address): ByteString =
    GoWireAddressHeader.concat(address)

  def selectTendermintExecutable(name: OperationSystem, arch: CpuArchitecture) = {
    val exeName = s"tendermint_${name.toString.toLowerCase}_$arch"
    name match {
      case OperationSystem.Windows => exeName + ".exe"
      case _                       => exeName
    }
  }

  def run(config: PravdaConfig)(implicit ec: ExecutionContext): Future[Process] = {

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
        fileOut.foreach { s =>
          s.close()
        }
        resourceInput.foreach { s =>
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
      if (privValidatorFile.exists())
        privValidatorFile.delete()
      config.validator foreach { validator =>
        writeFile(privValidatorFile) {
          val pubKey = byteUtils.byteStringToBase64(validator.address)
          val privKey = byteUtils.byteStringToBase64(validator.privateKey)
          val address = {
            val withType = packAddress(validator.address)
            val hash = ripemd160.getHash(withType.toByteArray)
            byteUtils.bytes2hex(hash)
          }
          s"""
            |{
            |  "address": "$address",
            |  "pub_key": {
            |    "type": "$PubKeyEd25519",
            |    "value": "$pubKey"
            |  },
            |  "last_height": "0",
            |  "last_round": "0",
            |  "last_step": 0,
            |  "last_signature": null,
            |  "priv_key": {
            |    "type": "$PrivKeyEd25519",
            |    "value": "$privKey"
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
           |log_level = "*:info"
           |abci = "socket"
           |proxy_app = "$proxyAppAddr"
           |[consensus]
           |create_empty_blocks = false
           |[p2p]
           |addr_book_strict = false
           |seeds="${config.seeds}"
           |laddr = "tcp://0.0.0.0:${config.tendermint.peerPort}"
           |[rpc]
           |laddr = "tcp://127.0.0.1:${config.tendermint.rpcPort}"
           |""".stripMargin

      writeFile(new File(configDir, "config.toml"))(tomlConfig)

      new ProcessBuilder(executable.getAbsolutePath, "--home", config.dataDirectory.getAbsolutePath, "node")
        .directory(config.dataDirectory)
        .inheritIO()
        .start()
    }
  }
}
