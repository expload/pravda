package io.mytc.timechain

package persistence

import java.io.{File, PrintWriter}

import io.mytc.timechain.data.TimechainConfig
import io.mytc.timechain.data.common.ApplicationStateInfo
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.data.serialization.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object FileStore {

  import Config._

  private lazy val applicationStateInfoFile = new File(timeChainConfig.dataDirectory, "application-state-info.json")

  def readApplicationStateInfoAsync() = Future(readApplicationStateInfo())

  def readApplicationStateInfo(): Option[ApplicationStateInfo] = {
    if (applicationStateInfoFile.exists()) {
      Some(transcode(Json @@ Source.fromFile(applicationStateInfoFile).mkString).to[ApplicationStateInfo])
    } else {
      None
    }
  }

  def updateApplicationStateInfoAsync(settings: ApplicationStateInfo) = Future(updateApplicationStateInfo(settings))

  def updateApplicationStateInfo(settings: ApplicationStateInfo): Unit = {
    val pw = new PrintWriter(applicationStateInfoFile)
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
