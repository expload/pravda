package pravda.cli.languages

package impl

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.node.data.common.TransactionId
import pravda.node.db.DB
import pravda.node.servers
import pravda.vm.impl.{MemoryImpl, VmImpl, WattCounterImpl}
import pravda.vm.{ExecutionResult, VmErrorException}

import scala.concurrent.{ExecutionContext, Future}

final class VmLanguageImpl(implicit executionContext: ExecutionContext) extends VmLanguage[Future] {

  def run(program: ByteString,
          executor: ByteString,
          storagePath: String,
          wattLimit: Long): Future[Either[String, ExecutionResult]] = Future {

    val executorAddress = Address @@ executor
    val envProvider = new servers.Abci.BlockDependentEnvironment(DB(storagePath, None))
    val env = envProvider.transactionEnvironment(executorAddress, TransactionId.forEncodedTransaction(program))
    val vm = new VmImpl()
    val memory = MemoryImpl.empty
    val wattCounter = new WattCounterImpl(wattLimit)
    val result = vm.spawn(program, env, memory, wattCounter, executorAddress)
    envProvider.commit(0, Vector(executorAddress)) // TODO retrieve block height from db
    result.error
      .map {
        case VmErrorException(error, stackTrace) =>
          s"$error: \n  ${stackTrace.stackTrace.mkString("\n  ")}"
      }
      .toLeft(result)
  }
}
