package pravda.cli.languages

package impl

import cats.Id
import com.google.protobuf.ByteString
import pravda.node.data.common.TransactionId
import pravda.node.db.DB
import pravda.node.servers
import pravda.vm.Vm
import pravda.vm.state.{Memory, VmErrorException}

final class VmLanguageImpl extends VmLanguage[Id] {

  def run(program: ByteString, executor: ByteString, storagePath: String): Id[Either[String, Memory]] = {
    try {
      val envProvider = new servers.Abci.EnvironmentProvider(DB(storagePath, None))
      val env = envProvider.transactionEnvironment(TransactionId.forEncodedTransaction(program))
      val memory = Vm.runRaw(program, executor, env)
      envProvider.commit(0) // TODO retrieve block height from db
      Right(memory)
    } catch {
      case VmErrorException(error, stackTrace) =>
        Left(s"$error: \n  ${stackTrace.stackTrace.mkString("\n  ")}")
    }
  }
}
