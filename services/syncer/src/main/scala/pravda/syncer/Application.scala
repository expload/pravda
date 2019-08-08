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

package pravda.syncer

import java.nio.file.{OpenOption, Paths, StandardOpenOption}
import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Path => UriPath}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import pravda.syncer.config.Config
import pravda.common.bytes

import scala.concurrent.{Await, Future}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success}

final case class RpcError(code: Int, message: String, data: String)

object Application extends App {
  import scala.concurrent.duration._
  // Base64 encoded transaction
  type EncodedTransaction = String

  implicit val system = ActorSystem("pravda-syncer")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val formats = DefaultFormats

  private val log = LoggerFactory.getLogger(this.getClass)

  import pureconfig.generic.auto._
  val config = pureconfig.loadConfigOrThrow[Config]

  val lineSep = System.getProperty("line.separator", "\n")

  val dataPathOptions: Set[OpenOption] = Set(
    StandardOpenOption.WRITE,
    StandardOpenOption.CREATE,
    StandardOpenOption.TRUNCATE_EXISTING
  )
  val dataPath = Paths.get("transactions.txt")

  args.headOption.fold(showHelpAndExit()) {
    case "import" => importTransactions()
    case "export" => exportTransactions()
    case _        => showHelpAndExit()
  }

  def importTransactions() = {
    val fDone = Source
      .unfoldAsync[(Long, Long), Long](0L -> 0L) {
        // At first (when state is zero) we should get the latest height from the tendermint
        case (0, _) =>
          latestBlockHeight(config.tendermintRpcUri).map { latestHeight =>
            Some(((latestHeight, 1), 1))
          }
        // In future calls, we just increment counter until latestHeight (it was stored in state)
        case (latestHeight, counter) =>
          if (counter < latestHeight) Future.successful {
            Some(((latestHeight, counter + 1), counter + 1))
          } else Future.successful(None)
      }
      .via(Flow[Long].mapAsync[(Long, List[EncodedTransaction])](1)(height =>
        readTransactions(height, config.tendermintRpcUri).map(r => (height, r))))
      .alsoTo(Sink.foreach[(Long, List[EncodedTransaction])] { case (h, txs) => showProgress(h, txs) })
      .via(Flow[(Long, List[EncodedTransaction])].map[ByteString] { case (h, txs) => formatRecord(h, txs) })
      .via(chunkBySize(1024, 5.seconds)) // flush each 1 Mb or every 5 seconds
      .runWith(FileIO.toPath(dataPath, dataPathOptions))

    fDone.onComplete {
      case Success(_)         => log.info("Transactions have imported")
      case Failure(exception) => log.error("importTransactions: ", exception)
    }
    Await.ready(fDone, Duration.Inf)
  }

  def exportTransactions() = {
    val bufferedSource = scala.io.Source.fromFile(dataPath.toFile)

    try {
      val fDone =
        Source
          .fromIterator(() => bufferedSource.getLines())
          .via(Flow[String].map[(Long, EncodedTransaction)](parseTransaction))
          .via(Flow[(Long, EncodedTransaction)].mapAsync[Unit](1) {
            case (h, tx) => broadcastTransaction(config.tendermintRpcUri, h, tx)
          })
          .runWith(Sink.ignore)
      fDone.onComplete {
        case Success(_)         => log.info("All transactionas have exported")
        case Failure(exception) => log.error("exportTransactions: ", exception)
      }
      Await.ready(fDone, Duration.Inf)
    } finally {
      bufferedSource.close
    }
  }

  system.terminate()

  sys.addShutdownHook {
    log.info("The EXIT signal has got. Terminating...")
    val f = system.terminate() map { _ =>
      log.info("The program has been properly shutdown")
    }
    // Give the program enough time to graceful shutdown
    Await.ready(f, 60.seconds)
  }

  def latestBlockHeight(tendermintRpcUri: Uri): Future[Long] = {
    val uri = tendermintRpcUri.withPath(UriPath("/status"))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { data =>
            val json = parse(data.utf8String)
            val latestHeightRaw = json \ "result" \ "sync_info" \ "latest_block_height"
            latestHeightRaw
              .extractOpt[String]
              .fold[Long] {
                val ex = new RuntimeException(s"Bad value of latest_block_height. Raw value is $latestHeightRaw")
                log.error("latestBlockHeight: ", ex)
                throw ex
              }(_.toLong)
          }
        case HttpResponse(code, _, entity, _) =>
          entity.discardBytes()
          val ex = new RuntimeException(s"Bad HTTP code: $code")
          log.error("latestBlockHeight: ", ex)
          throw ex
      }
  }

  def readTransactions(height: Long, tendermintRpcUri: Uri): Future[List[EncodedTransaction]] = {

    val uri = tendermintRpcUri.withPath(UriPath("/block")).withQuery(Uri.Query("height" -> height.toString))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { data =>
            val json = parse(data.utf8String)
            val txs = (json \ "result" \ "block" \ "data" \ "txs") \\ classOf[JString]
            txs
          }
        case HttpResponse(code, _, entity, _) =>
          entity.discardBytes()
          val ex = new RuntimeException(s"Bad HTTP code: $code")
          log.error("readTransactions: ", ex)
          throw ex
      }
  }

  def broadcastTransaction(tendermintRpcUri: Uri, fromHeight: Long, tx: EncodedTransaction): Future[Unit] = {
    def parseSuccessResult(json: JValue) = {
      val deliverTxCode = (json \ "result" \ "deliver_tx" \ "code").extractOrElse[String]("0")
      val checkTxCode = (json \ "result" \ "check_tx" \ "code").extractOrElse[String]("0")

      if (deliverTxCode != "0" || checkTxCode != "0") {
        val deliverTxLog = (json \ "result" \ "deliver_tx" \ "log").extractOrElse("")
        val checkTxLog = (json \ "result" \ "check_tx" \ "log").extractOrElse("")
        val ex = new RuntimeException(
          s"There are errors for height $fromHeight: deliver_tx_code=$deliverTxCode, check_tx_code=$checkTxCode\n" +
            s"deliverTxLog = $deliverTxLog, checkTxLog = $checkTxLog")
        log.error("broadcastTransaction: ", ex)
        throw ex
      } else {
        log.info(s"Successfully broadcasted transaction from height $fromHeight")
        ()
      }
    }

    val decoded = Base64.getDecoder.decode(tx)
    val uri = tendermintRpcUri
      .withPath(UriPath("/broadcast_tx_commit"))
      .withQuery(Uri.Query("tx" -> ("0x" + bytes.bytes2hex(decoded))))

    Http()
      .singleRequest(HttpRequest(uri = uri))
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map { data =>
            val json = parse(data.utf8String)

            (json \ "error")
              .extractOpt[RpcError]
              .fold(parseSuccessResult(json))(rpcError =>
                if (rpcError.data.contains("Tx already exists in cache")) {
                  log.info(s"Transaction for height $fromHeight already broadcasted. Skipping...")
                } else {
                  val ex = new RuntimeException(s"RpcError: $rpcError")
                  log.error("broadcastTransactions: ", ex)
                  throw ex
              })
          }
        case HttpResponse(code, _, entity, _) =>
          entity.discardBytes()
          val ex = new RuntimeException(s"Bad HTTP code: $code")
          log.error("broadcastTransactions: ", ex)
          throw ex
      }
  }

  def formatRecord(height: Long, txs: List[EncodedTransaction]): ByteString = {
    val sb = new StringBuilder()
    txs.zipWithIndex.foreach {
      case (tx, index) =>
        val formatted = f"$height%010d|$index%04d|$tx"
        sb.append(formatted)
        sb.append(lineSep)
    }
    ByteString(sb.result())
  }

  def parseTransaction(src: String): (Long, EncodedTransaction) = {
    src.split('|') match {
      case Array(height, _, tx) =>
        (height.toLong, tx)
      case _ =>
        val ex = new RuntimeException(s"Bad record: $src.")
        log.error("parseTransaction: ", ex)
        throw ex
    }
  }

  def showHelpAndExit() = {
    System.err.println("Mode [import | export] should be defined")
    System.exit(-1)
  }

  def showProgress(height: Long, txs: List[Application.EncodedTransaction]) = {
    log.trace(s"Height = $height, Txs count: ${txs.length}")
  }

  def chunkBySize(kb: Long, d: FiniteDuration): Flow[ByteString, ByteString, NotUsed] = {
    // 16Kb is the size of ByteString
    val n = (kb / 16).toInt
    Flow[ByteString]
      .groupedWithin(n, d)
      .map(_.foldLeft(ByteString.newBuilder) { case (b, bs) => b.append(bs) }.result())
  }
}
