package io.mytc
package timechain.persistence


import com.google.protobuf.ByteString
import io.mytc.keyvalue.{DB, Operation}
import io.mytc.timechain.data.common._
import io.mytc.timechain.data.domain._
import io.mytc.timechain.data.misc.BlockChainInfo

import scala.concurrent.Future

import implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object BlockChainStore {
  def apply(path: String) = new BlockChainStore(path)
}

class BlockChainStore(path: String) {

  type FOpt[A] = Future[Option[A]]

  private implicit val db = DB(path, hashCounter = true)
  db.initHash

  private val accountEntry = Entry[Address, Account]("account")
  private val blockChainInfoEntry = SingleEntry[BlockChainInfo]("blockchain")



  def putAccount(account: Account, state: BatchState): BatchState =  {
    state.addOperations(accountEntry.putBatch(account.address, account))
  }

  def getAccount(address: Address): FOpt[Account] = {
    accountEntry.get(address)
  }

  def addOrCreateAccount(
                                  address: Address,
                                  free: BigDecimal,
                                  frozen: BigDecimal,
                                  state: BatchState
                                ): BatchState = {
    val account = state.accounts.get(address).orElse(accountEntry.syncGet(address))
    val newAccount = account.map(
      a => a.copy(free = a.free + free, frozen = a.frozen + frozen)
    ).getOrElse(Account(address, Mytc(free), Mytc(frozen)))
    state.addOperations(accountEntry.putBatch(address, newAccount)).putAccount(newAccount)
  }

  def getBlockchainInfo(): Future[BlockChainInfo] = {
    blockChainInfoEntry.get().map {
      _.getOrElse(BlockChainInfo(0, Vector.empty))
    }
  }

  def putBlockChainInfo(blockChainInfo: BlockChainInfo): Future[Unit] = {
    blockChainInfoEntry.put(blockChainInfo)
  }

  def putBlockChainInfo(blockChainInfo: BlockChainInfo, state: BatchState): BatchState =  {
    state.addOperations(blockChainInfoEntry.putBatch(blockChainInfo))
  }

  case class BatchState(
    newHeight: Long,
    validators: Vector[Address],
    accounts: Map[Address, Account] = Map.empty,
    operations: Vector[Operation] = Vector.empty
  ) {
    def addOperations(ops: Operation*): BatchState = copy(operations = operations ++ ops)
    def putAccount(account: Account): BatchState = copy(accounts = accounts + (account.address -> account))
  }

  def appHash: ByteString = {
    if(db.stateHash.forall(_ == 0)) ByteString.EMPTY
    else ByteString.copyFrom(db.stateHash)
  }

  def close(): Unit = {
    db.close()
  }

}
