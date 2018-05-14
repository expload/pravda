package io.mytc.timechain
package persistence

import java.io.{File, PrintWriter}

import io.mytc.timechain.data.TimechainConfig
import io.mytc.timechain.data.common.{NodeSettings, OrganizationInfo}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import io.mytc.timechain.data.serialization._

import json._

object FileStore {

  import Config._

  private lazy val organizationInfoFile = new File(timeChainConfig.dataDirectory, "info-organization.json")
  private lazy val nodeSettingsFile = new File(timeChainConfig.dataDirectory, "node-settings.json")

  def readMyOrganizationInfoAsync(): Future[Option[OrganizationInfo]] = Future {
    readMyOrganizationInfo()
  }

  def readMyOrganizationInfo(): Option[OrganizationInfo] = {
    if (organizationInfoFile.exists()) {
      Some(transcode(Json @@ Source.fromFile(organizationInfoFile).mkString).to[OrganizationInfo])
    } else {
      None
    }
  }

  def updateMyOrganizationInfoAsync(info: OrganizationInfo): Future[Unit] =
    Future(updateMyOrganizationInfo(info))

  def updateMyOrganizationInfo(info: OrganizationInfo): Unit = {
    val pw = new PrintWriter(organizationInfoFile)
    try {
      pw.write(transcode(info).to[Json])
    } finally {
      pw.close()
    }
  }

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
