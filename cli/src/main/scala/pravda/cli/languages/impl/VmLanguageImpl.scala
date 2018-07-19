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
