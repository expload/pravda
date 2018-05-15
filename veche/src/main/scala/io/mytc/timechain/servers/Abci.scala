package io.mytc.timechain

package servers

import com.tendermint.abci._
import io.mytc.timechain.clients.AbciClient
import scala.concurrent.Future

class Abci(abciClient: AbciClient) extends io.mytc.tendermint.abci.Api {

  def info(request: RequestInfo): Future[ResponseInfo] = ???

  def initChain(request: RequestInitChain): Future[ResponseInitChain] = ???

  def beginBlock(request: RequestBeginBlock): Future[ResponseBeginBlock] = ???

  def deliverTx(request: RequestDeliverTx): Future[ResponseDeliverTx] = ???

  def endBlock(request: RequestEndBlock): Future[ResponseEndBlock] = ???

  def commit(request: RequestCommit): Future[ResponseCommit] = ???

  def flush(request: RequestFlush): Future[ResponseFlush] = ???

  def checkTx(request: RequestCheckTx): Future[ResponseCheckTx] = ???

  def setOption(request: RequestSetOption): Future[ResponseSetOption] = ???

//  private def makeInitialDistribution(): Unit = {
//    // It is end of genesis block
//    // Let's make initial token distribution
//    val tokenSaleMembers = List(
//      /* Alice */ Address.tryFromHex("67EA4654C7F00206215A6B32C736E75A77C0B066D9F5CEDD656714F1A8B64A45").getOrElse(Address.Void) -> Mytc(BigDecimal(100)),
//      /*  Bob  */ Address.tryFromHex("17681F651544420EB9C89F055500E61F09374B605AA7B69D98B2DEF74E8789CA").getOrElse(Address.Void) -> Mytc(BigDecimal(300))
//    )
//    abciClient.broadcastTransaction(
//      // Address and private key of Satoshi incarnation
//      from = Address.fromHex("BCA442F4C7BA41DA2E704890B236A1459EE5311061F17193CE95060B3AA3583B"),
//      privateKey = PrivateKey.fromHex("AED1C5D3CB5BDC1D06B1CE69D3B2636F74F6465AF6160EF5E8B8A09D8C285EC2BCA442F4C7BA41DA2E704890B236A1459EE5311061F17193CE95060B3AA3583B"),
//      data = TransactionData.Distribution(tokenSaleMembers),
//      fee = Mytc.zero,
//      mode = "async"
//    ) onComplete {
//      case Success(_) => ()
//      case Failure(e) =>
//        println(e)
//    }
//  }

  override def query(req: RequestQuery): Future[ResponseQuery] = ???
}

object Abci {
  final val TxStatusOk = 0
  final val TxStatusUnauthorized = 1
  final val TxStatusError = 2
}
