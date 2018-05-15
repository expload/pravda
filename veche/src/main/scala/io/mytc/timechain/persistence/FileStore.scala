package io.mytc.timechain

package persistence

import java.io.{File, PrintWriter}

import io.mytc.timechain.data.TimechainConfig
import io.mytc.timechain.data.common.NodeSettings
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.data.serialization.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object FileStore {

  import Config._

  private lazy val nodeSettingsFile = new File(timeChainConfig.dataDirectory, "node-settings.json")

  def readNodeSettingsAsync() = Future(readNodeSettings())

  def readNodeSettings(): Option[NodeSettings] = {
    if (nodeSettingsFile.exists()) {
      Some(transcode(Json @@ Source.fromFile(nodeSettingsFile).mkString).to[NodeSettings])
    } else {
      None
    }
  }

  def updateNodeSettingsAsync(settings: NodeSettings) = Future(updateNodeSettings(settings))

  def updateNodeSettings(settings: NodeSettings): Unit = {
    val pw = new PrintWriter(nodeSettingsFile)
    try {
      pw.write(transcode(settings).to[Json])
    } finally {
      pw.close()
    }
  }

  // From config
  def readPaymentWalletAsync(): Future[TimechainConfig.PaymentWallet] = {
    Future.successful(timeChainConfig.paymentWallet)
  }

}
