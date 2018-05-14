package io.mytc.timechain.clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import akka.stream.ActorMaterializer
import akka.util.{ByteString => AkkaByteString}
import com.google.protobuf.ByteString
import io.mytc.timechain.contrib.ripemd160
import io.mytc.timechain.data.Action.ActionsFile
import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.common.{Address, DataRef, Mytc, OrganizationInfo}
import io.mytc.timechain.data.cryptography.PrivateKey
import io.mytc.timechain.data.offchain.{PurchaseIntention, PurchaseIntentionData}
import io.mytc.timechain.data.cryptography

import scala.concurrent.{ExecutionContextExecutor, Future}
import io.mytc.timechain.data.serialization._
import io.mytc.timechain.persistence.BlockChainStore
import json._

import io.mytc.timechain.utils.AkkaHttpSpecials

class OrganizationClient(abciClient: AbciClient)(implicit
    system: ActorSystem,
    materializer: ActorMaterializer,
    executionContext: ExecutionContextExecutor,
    blockChainStore: BlockChainStore
) {

  import AkkaHttpSpecials._

  def readOrganizationInfo(domain: String): Future[Option[OrganizationInfo]] = {
    Http()
      .singleRequest(HttpRequest(uri = s"https://$domain/.mytime"))
      .flatMap { response =>
        response
          .entity
          .dataBytes
          .runReduce(_++_)
          .map { bs =>
            try {
              Some(transcode(Json @@ bs.utf8String).to[OrganizationInfo])
            } catch {
              case _: Throwable =>
                None
            }
          }
      }
  }

  def readRawActionFile(domain: String,
                        path: String,
                        intention: PurchaseIntention.SignedPurchaseIntention): Future[AkkaByteString] = {

    //val dataRefHex = bytes2hex(dataRef)
    val preparedPath = {
      val preS = path match {
        case s if s.startsWith("/") => s
        case s => "/" + s
      }
      preS.stripSuffix("/")
    }
    println(s"https://$domain$preparedPath")
    Marshal(intention).to[RequestEntity] flatMap { entity =>
      val request = HttpRequest(
        uri = s"https://$domain$preparedPath",
        method = HttpMethods.POST,
        entity = entity
      )
      Http().singleRequest(request).flatMap { response =>
        response
          .entity
          .dataBytes
          .runReduce(_++_)
      }
    }
  }

  def parseActionsFile(json: Json): ActionsFile =
    transcode(json).to[ActionsFile]

  def purchaseActions(from: Address, vendor: Address, dataRef: DataRef, privateKey: PrivateKey, fee: Mytc, nonce: Int): Future[Either[String, ActionsFile]] = {
    def error(s: String) = Future.successful(Left(s): Either[String, ActionsFile])
    val signedPurchaseIntention = cryptography.signIntention(privateKey, PurchaseIntentionData(nonce, from, dataRef))
    blockChainStore.getOrganization(vendor) flatMap { maybeOrganization =>
      maybeOrganization.fold(error("vendor not found")) { organization =>
        readOrganizationInfo(organization.domain) flatMap { info =>
          info.flatMap(_.path).fold(error("path is not defined or organization info is unreadable")) { path =>
            readRawActionFile(organization.domain, path, signedPurchaseIntention) flatMap {
              case s if ByteString.copyFrom(ripemd160.getHash(s.toArray)) != dataRef =>
                println(s.utf8String)
                error("data checksum doesn't match")
              case s =>
                val confirmation = TransactionData.DataPurchasingConfirmation(dataRef)
                abciClient
                  .broadcastTransaction(from, privateKey, confirmation, fee)
                  .map(_ => Right(parseActionsFile(Json @@ s.utf8String)))
            }
          }
        }
      }
    }
  }

}
