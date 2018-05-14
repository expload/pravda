package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.tendermint.abci._
import io.mytc.timechain.clients.{AbciClient, OrganizationClient}
import io.mytc.timechain.data.blockchain.{Transaction, TransactionData}
import io.mytc.timechain.data.common.{Address, Mytc}
import io.mytc.timechain.data.cryptography.PrivateKey
import io.mytc.timechain.data.processing.{ProcessingEffect, ProcessingError, ProcessingState}
import io.mytc.timechain.data.{cryptography, processing}
import io.mytc.timechain.{launcher, tendermint}
import io.mytc.timechain.utils.Var

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import cats.data.EitherT
import cats.implicits._
import io.mytc.timechain.persistence.BlockChainStore
import io.mytc.timechain.data.serialization._
import boopick._

class Abci(
      organizationClient: OrganizationClient,
      abciClient: AbciClient)(implicit
    system: ActorSystem,
    materializer: ActorMaterializer,
    blockChainStore: BlockChainStore,
    executionContext: ExecutionContextExecutor) extends io.mytc.tendermint.abci.Api {

  import Abci._

  private val mempoolState = Var(ProcessingState())
  private val consensusState = Var(ConsensusState())

  def consensusStateReader: Var.VarReader[ConsensusState] =
    consensusState

  def echo(request: RequestEcho): Future[ResponseEcho] = {
    Future.successful(ResponseEcho(request.message))
  }

  def info(request: RequestInfo): Future[ResponseInfo] = {
    for {
      info <- blockChainStore.getBlockchainInfo()
      state = ProcessingState(lastBlockHash = blockChainStore.appHash, lastBlockHeight = info.height)
      _ <- mempoolState.set(state)
      _ <- consensusState.set(ConsensusState(state))
    } yield {
      ResponseInfo(
        lastBlockHeight = info.height,
        lastBlockAppHash = blockChainStore.appHash
      )
    }
  }

  def initChain(request: RequestInitChain): Future[ResponseInitChain] = {
    val validators = request
      .validators
      .toVector
      .map(x => tendermint.unpackAddress(x.pubKey))
    for {
      info <- blockChainStore.getBlockchainInfo()
      withValidators = info.copy(validators = validators)
      _ <- blockChainStore.putBlockChainInfo(withValidators)
    } yield {
      ResponseInitChain()
    }
  }

  def beginBlock(request: RequestBeginBlock): Future[ResponseBeginBlock] = {
    println(s"Block begin")
    for {
      info <- blockChainStore.getBlockchainInfo()
      malicious = request.byzantineValidators.map(x => tendermint.unpackAddress(x.pubKey))
      absent = request.absentValidators
      validators = info.validators.zipWithIndex.collect {
        case (address, i) if !malicious.contains(address) && !absent.contains(i) => address
      }
      _ <- consensusState.update(_.copy(validators = validators))
    } yield ResponseBeginBlock()
  }

  def deliverTx(request: RequestDeliverTx): Future[ResponseDeliverTx] =
    cryptography.checkTransactionSignature(transcode(request.tx).to[Transaction.SignedTransaction]) match {
      case Some(transaction) =>
        // Retrieve data required by transaction from database
        consensusState
          .updateAsync { state =>
            blockChainStore.restoreProcessingState(transaction, state.processing).map { restored =>
              state.copy(processing = restored)
            }
          }
          .flatMap { state =>
            processing.processTransaction(transaction, state.processing) match {
              case Left(error) =>
                Future(ResponseDeliverTx(code = TxStatusError, log = error.toString))
              case Right(effects) =>
                consensusState
                  .update { state =>
                    state.copy(
                      processing = effects.foldLeft(state.processing)(processing.applyEffectToState),
                      effects = state.effects ++ effects
                    )
                  }
                  .map(_ => ResponseDeliverTx(code = 0))
            }
          }
      case None =>
        Future.successful(ResponseDeliverTx(code = TxStatusUnauthorized))
    }


  def endBlock(request: RequestEndBlock): Future[ResponseEndBlock] = {
    println(s"Block end (Height: ${request.height})")
    consensusState
      .update { state =>
        val processing = state.processing.copy(lastBlockHeight = request.height)
        state.copy(processing = processing)
      }
      .map { _ => ResponseEndBlock() }
  }

  def commit(request: RequestCommit): Future[ResponseCommit] = {
    for {
      state <- consensusState.get()
      height = state.processing.lastBlockHeight
      newProcessing = ProcessingState(lastBlockHeight = height)
      _ = println(state.effects)
      appHash <- blockChainStore.applyPersistentEffects(state.effects, state.validators, height)
      _ <- consensusState.set(ConsensusState(newProcessing))
      _ <- mempoolState.set(newProcessing)
    } yield {
      println("Block committed")
      // Hack for initial distribution
      if (launcher.timeChainConfig.genesis.distribution && height == 1)
        makeInitialDistribution()
      // Commit successful
      ResponseCommit(data = appHash)
    }
  }

  def flush(request: RequestFlush): Future[ResponseFlush] = {
    Future.successful(ResponseFlush())
  }

  def checkTx(request: RequestCheckTx): Future[ResponseCheckTx] = {

    def dirtyChecks(effects: List[ProcessingEffect]): EitherT[Future, ProcessingError, Unit] = {
      val checksF =
        effects.collect {
          case x: ProcessingEffect.CheckDomainAndUpdateInfo =>
            organizationClient.readOrganizationInfo(x.domain)
              .map(_.fold(false)(_.address == x.address))
              .map(Either.cond(_, (), ProcessingError.InvalidDomain))
        }
      EitherT {
        Future.sequence(checksF).map { checks =>
          checks.foldLeft(Either.right[ProcessingError, Unit](())) {
            case (acc, _) if acc.isLeft => acc
            case (_, y) if y.isLeft => y
            case (acc, _) => acc
          }
        }
      }
    }

    val signedTransaction = transcode(request.tx).to[Transaction.SignedTransaction]

    cryptography.checkTransactionSignature(signedTransaction) match {
      case Some(transaction) =>
        val either =
          for {
            state <- EitherT.right(mempoolState.updateAsync(blockChainStore.restoreProcessingState(transaction, _)))
            effects <- EitherT.fromEither[Future](processing.processTransaction(transaction, state))
            _ <- dirtyChecks(effects)
            _ <- EitherT.right[ProcessingError].apply[Future, Unit](mempoolState.set(effects.foldLeft(state)(processing.applyEffectToState)))
          } yield {
            ResponseCheckTx(code = TxStatusOk)
          }
        either.valueOr(error => ResponseCheckTx(code = TxStatusError, log = error.toString))
      case None =>
        Future.successful(ResponseCheckTx(code = TxStatusUnauthorized))
    }
  }

  def setOption(request: RequestSetOption): Future[ResponseSetOption] = ???

  //

  private def makeInitialDistribution(): Unit = {
    // It is end of genesis block
    // Let's make initial token distribution
    val tokenSaleMembers = List(
      /* Alice */ Address.tryFromHex("67EA4654C7F00206215A6B32C736E75A77C0B066D9F5CEDD656714F1A8B64A45").getOrElse(Address.Void) -> Mytc(BigDecimal(100)),
      /*  Bob  */ Address.tryFromHex("17681F651544420EB9C89F055500E61F09374B605AA7B69D98B2DEF74E8789CA").getOrElse(Address.Void) -> Mytc(BigDecimal(300))
    )
    abciClient.broadcastTransaction(
      // Address and private key of Satoshi incarnation
      from = Address.fromHex("BCA442F4C7BA41DA2E704890B236A1459EE5311061F17193CE95060B3AA3583B"),
      privateKey = PrivateKey.fromHex("AED1C5D3CB5BDC1D06B1CE69D3B2636F74F6465AF6160EF5E8B8A09D8C285EC2BCA442F4C7BA41DA2E704890B236A1459EE5311061F17193CE95060B3AA3583B"),
      data = TransactionData.Distribution(tokenSaleMembers),
      fee = Mytc.zero,
      mode = "async"
    ) onComplete {
      case Success(_) => ()
      case Failure(e) =>
        println(e)
    }
  }

  override def query(req: RequestQuery): Future[ResponseQuery] = ???
}

object Abci {
  case class ConsensusState(
    processing: ProcessingState = ProcessingState(),
    validators: Vector[Address] = Vector.empty,
    effects: List[ProcessingEffect] = Nil
  )

  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2
}
