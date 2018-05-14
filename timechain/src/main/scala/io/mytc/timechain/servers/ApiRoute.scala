package io.mytc.timechain
package servers

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.mytc.timechain.clients.{AbciClient, OrganizationClient}
import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.common.{Address, DataRef, DepositId, Mytc}
import io.mytc.timechain.data.domain.{ActionPackage, Deposit, Offer}
import io.mytc.timechain.data.offchain.PurchaseIntention
import io.mytc.timechain.persistence.{BlockChainStore, FileStore, NodeStore}
import io.mytc.timechain.data.serialization.json._
import io.mytc.timechain.data.{cryptography, processing}
import io.mytc.timechain.servers.Abci.ConsensusState
import io.mytc.timechain.utils.Var


import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Random

class ApiRoute(
      consensusState: Var.VarReader[ConsensusState],
      abciClient: AbciClient,
      organizationClient: OrganizationClient)(implicit
    system: ActorSystem,
    materializer: ActorMaterializer,
    executionContext: ExecutionContextExecutor,
    nodeStore: NodeStore,
    blockChainStore: BlockChainStore
) {

  import ApiRoute._
  import io.mytc.timechain.utils.AkkaHttpSpecials._

  val route: Route =
    pathPrefix("public") {
      path(".mytime") {
        get {
          val future = FileStore.readMyOrganizationInfoAsync()
          onSuccess(future) { info =>
            rejectEmptyResponse {
              complete(info)
            }
          }
        }
      } ~ path("actions") {
        post {
          entity(as[PurchaseIntention.SignedPurchaseIntention]) { pi =>

            import cats.data.EitherT
            import cats.implicits._

            val eitherT =
              for {
                authorizedPi <- EitherT.fromOption[Future](cryptography.checkIntention(pi), "invalid signature")
                actionPackage <- EitherT.apply[Future, String, ActionPackage](nodeStore
                  .readActionPackage(authorizedPi.data.dataRef)
                  .map(_.toRight("data not found")))
                offer <- EitherT.apply[Future, String, Offer](blockChainStore
                  .getOffer(actionPackage.dataRef)
                  .map(_.toRight("offer not found")))
                wallet <- EitherT.right(FileStore.readPaymentWalletAsync())
                multiplier <- EitherT.right(blockChainStore.getMultiplier(wallet.address)
                  .map(_.getOrElse(BigDecimal(1.0))))
                tariffs <- EitherT.apply(
                  blockChainStore.getMatrix(wallet.address)
                    .map(_.toRight("tariffs not found"))
                )
                consensus <- EitherT.right(consensusState.get())
                deposit <- EitherT.apply[Future, String, Deposit](blockChainStore
                  .getDeposit(DepositId(authorizedPi.data.address, wallet.address))
                  .map(_.toRight("deposit not found")))
                // check deposit is recent and contains enough funds
                _ <- {
                  EitherT.cond[Future].apply[String, Unit](
                    test =
                      consensus.processing.lastBlockHeight - deposit.block < processing.DepositWithdrawTimeout &&
                      deposit.amount >= tariffs.price(
                        offer.tariff,
                        consensus.processing.lastBlockHeight - offer.blockHeight,
                        1, // TODO: Market Rate is always 1 by now, we need to calculate it properly
                        multiplier
                      ),
                    left = "deposit is obsolete",
                    right = ()
                  )
                }
                // save intention
                _ <- EitherT.right[String].apply[Future, Long](
                  nodeStore.addPurchasingIntentionEvidence(authorizedPi)
                )
              } yield {
                actionPackage
              }
            onSuccess(eitherT.value) {
              case Right(ap) => complete(ap.jsonData)
              case Left(error) => complete(HttpResponse(StatusCodes.BadRequest, entity = error))
            }
          }
        }
      }
    } ~ pathPrefix("private") {
      pathPrefix("accounts" / Segment) { (accountHex: String) =>
        path("actions" / "track") {
          post {
            entity(as[Track]) { track =>
              val address = Address.fromHex(accountHex)
              onSuccess(nodeStore.trackAction(address, track.action, None)) tapply { _ =>
                complete("ok")
              }
            }
          }
        } ~ path("actions" / "commit") {
          post {
            entity(as[Commit]) { commit =>
              val address = Address.fromHex(accountHex)
              val future =
                for {
                  pkg <- nodeStore.commitActionPackage(address, commit.valuableAction, None)
                  wallet <- FileStore.readPaymentWalletAsync()
                  _ <- abciClient.broadcastTransaction(
                    from = wallet.address,
                    wallet.privateKey,
                    fee = commit.fee,
                    data = TransactionData.Time(
                      user = address,
                      dataRef = pkg.dataRef,
                      reward = commit.reward,
                      tariff = commit.tariff
                    )
                  )
                } yield {
                  pkg
                }
              onSuccess(future) { pkg =>
                complete(pkg)
              }
            }
          }
        } ~ path("deposit") {
          // TODO withdraw deposit
          // TODO show deposit
          // Add deposit for vendor
          post {
            entity(as[AddDeposit]) { deposit =>
              val address = Address.fromHex(accountHex)
              val future = FileStore.readPaymentWalletAsync() flatMap { wallet =>
                abciClient.broadcastTransaction(
                  from = wallet.address,
                  wallet.privateKey,
                  fee = deposit.fee,
                  data = TransactionData.DataPurchasingDeposit(
                    vendor = address,
                    amount = deposit.amount
                  )
                )
              }
              // You ask me why tapply?
              // Why implicit addDirectiveApply doesn't work?
              // I do not know!
              onSuccess(future) tapply { _ =>
                complete("ok")
              }
            }
          }
        } ~ pathPrefix("offers") {
          pathEnd {
            get {
              val address = Address.fromHex(accountHex)
              val future = blockChainStore.getOffersByUser(address)
              onSuccess(future) { list =>
                complete(list)
              }
            }
          } ~ path(Segment / "purchase") { dataRefHex =>
            post {
              parameters('fee.as[Double]) { fee =>
                val future =
                  for {
                    wallet <- FileStore.readPaymentWalletAsync()
                    dataRef = DataRef.fromHex(dataRefHex)
                    maybeOffer <- blockChainStore.getOffer(dataRef)
                    offer = maybeOffer.getOrElse(throw new Exception("offer not found"))
                    errorOrData <- organizationClient.purchaseActions(
                      from = wallet.address,
                      vendor = offer.seller,
                      dataRef = dataRef,
                      privateKey = wallet.privateKey,
                      fee = Mytc(BigDecimal(fee)),
                      nonce = Random.nextInt()
                    )
                  } yield {
                    errorOrData match {
                      case Left(error) => throw new Exception(error)
                      case Right(data) => data
                    }
                  }
                onSuccess(future) { data =>
                  complete(data)
                }
              }
            }
          }
        }
      }
    }

}

object ApiRoute {

  final val DepositLife = processing.DepositWithdrawTimeout
  final val DepositHalfLife = 10 / 2

  case class Track(
    action: data.Action
  )

  case class Commit(
    valuableAction: data.Action,
    reward: Mytc,
    tariff: Int,
    fee: Mytc
  )

  case class AddDeposit(
    amount: Mytc,
    fee: Mytc
  )
}
