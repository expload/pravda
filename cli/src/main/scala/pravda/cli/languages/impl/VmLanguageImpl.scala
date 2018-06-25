package pravda.cli.languages

package impl

import com.google.protobuf.ByteString
import pravda.common.domain.Address
import pravda.node.data.common.TransactionId
import pravda.node.db.DB
import pravda.node.servers
import pravda.vm.{Memory, Vm}
import pravda.vm.Memory

import scala.concurrent.{ExecutionContext, Future}

final class VmLanguageImpl(implicit executionContext: ExecutionContext) extends VmLanguage[Future] {

  def run(program: ByteString,
          executor: ByteString,
          storagePath: String,
          wattLimit: Long): Future[Either[String, Memory]] = Future {
    val envProvider = new servers.Abci.EnvironmentProvider(DB(storagePath, None))
    val env = envProvider.transactionEnvironment(TransactionId.forEncodedTransaction(program))
    val result = Vm.runRaw(program, Address @@ executor, env, wattLimit)
    envProvider.commit(0, Vector(Address @@ executor)) // TODO retrieve block height from db
    result.error
      .map {
        case VmErrorException(error, stackTrace) =>
          s"$error: \n  ${stackTrace.stackTrace.mkString("\n  ")}"
      }
      .toLeft(result.memory)
  }
}
