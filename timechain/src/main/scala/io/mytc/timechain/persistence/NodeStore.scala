package io.mytc
package timechain.persistence

import java.time.ZonedDateTime

import com.google.protobuf.ByteString
import io.mytc.keyvalue.DB
import io.mytc.timechain.contrib.ripemd160
import io.mytc.timechain.data.common._
import io.mytc.timechain.data.domain._
import io.mytc.timechain.data.offchain.PurchaseIntention.{AuthorizedPurchaseIntention, SignedPurchaseIntention}
import shapeless.{::, HNil}
import io.mytc.timechain.data.{Action => MytimeAction}

import scala.concurrent.Future

// Implicits
import implicits._

import scala.concurrent.ExecutionContext.Implicits.global


import io.mytc.timechain.data.serialization._
import json._

object NodeStore {
  def apply(path: String): NodeStore = new NodeStore(path)
}

class NodeStore(path: String) {

  type FOpt[A] = Future[Option[A]]

  private implicit val db = DB(path)

  private val tariffDescriptionEntry = Entry[Int, TariffDescription]("tariff-description")
  private val walletEntry = Entry[String, Wallet]("wallet")
  private val actionEntry = Entry[Address :: Long :: HNil, ActionRecord]("action-record")
  private val actionPackageEntry = Entry[DataRef, ActionPackage]("action-package")
  private val intentionEntry = Entry[Address :: DataRef :: Long :: HNil, PurchaseIntentionEvidence]("intention")

  private def evidenceInc: Future[Long] = db.inc("evidence-sequence")
  private def actionInc: Future[Long] = db.inc("action-sequence")

  def getTariffDescriptions: Future[Map[Int, TariffDescription]] = {
    tariffDescriptionEntry.all.map {
      _.map { x =>
        (x.id, x)
      }.toMap
    }
  }

  def putTariffDescription(description: TariffDescription): Future[Unit] = {
    tariffDescriptionEntry.put(description.id, description)
  }

  def addPurchasingIntentionEvidence(purchaseIntention: AuthorizedPurchaseIntention): Future[Long] = {
    evidenceInc.flatMap { id =>
      val evidence = PurchaseIntentionEvidence(
        id = id,
        address = purchaseIntention.data.address,
        dataRef = purchaseIntention.data.dataRef,
        timestamp = System.currentTimeMillis(),
        intention = SignedPurchaseIntention(purchaseIntention.data, purchaseIntention.signature),
        repaid = false
      )
      intentionEntry.put(evidence.address :: evidence.dataRef :: id :: HNil, evidence).map {
        _ => id
      }
    }
  }

  def trackAction(address: Address, action: MytimeAction, maybeTime: Option[ZonedDateTime]): Future[Long] = {
    val time = maybeTime.getOrElse(ZonedDateTime.now())
    actionInc.flatMap { id =>
      val record = ActionRecord(
        id = id,
        address = address,
        action = action,
        timestamp = time.toInstant.toEpochMilli,
        timezone = time.getZone.getId,
        pending = true
      )
      actionEntry.put(record.address :: id :: HNil, record).map(_ => id)
    }
  }

  def commitActionPackage(address: Address, action: MytimeAction, time: Option[ZonedDateTime]): Future[ActionPackage] = {

    def createPkg(records: List[ActionRecord]): ActionPackage = {
      val pendingActions = records.map(r => MytimeAction.ActionsFileRecord(r.timestamp, r.timezone, r.action))
      val json = transcode(MytimeAction.ActionsFile(address, pendingActions)).to[Json]
      val hash = ripemd160.getHash(json.getBytes)

      ActionPackage(
        dataRef = DataRef(ByteString.copyFrom(hash)),
        userAddress = address,
        firstRecord = records.head.id,
        lastRecord = records.last.id,
        jsonData = json
      )
    }

    for {
      _ <- trackAction(address, action, time)
      records <- actionEntry.startsWith(address)
      pkg = createPkg(records)
      _ <- actionPackageEntry.put(pkg.dataRef, pkg)
    } yield pkg
  }

  def readActionPackage(dataRef: DataRef): FOpt[ActionPackage] = {
    actionPackageEntry.get(dataRef)
  }

  def wallets(): Future[List[Wallet]] = {
    walletEntry.all
  }

  def getWallet(name: String): Future[Option[Wallet]] = {
    walletEntry.get(name)
  }

  def putWallet(wallet: Wallet): Future[Unit] = {
    walletEntry.put(wallet.name, wallet)
  }

  def getIntentions: Future[List[PurchaseIntentionEvidence]] = {
    intentionEntry.all
  }

  def close(): Unit = {
    db.close()
  }

}
