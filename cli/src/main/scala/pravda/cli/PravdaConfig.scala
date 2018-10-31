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

package pravda.cli

import pravda.common.domain.NativeCoin

sealed trait PravdaConfig

object PravdaConfig {

  final val DefaultExecutor = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86"

  sealed trait CompileMode

  object CompileMode {
    case object Nope   extends CompileMode
    case object Asm    extends CompileMode
    case object Disasm extends CompileMode
    case object DotNet extends CompileMode
  }

  case object Nope extends PravdaConfig

  object DefaultValues {

    object Broadcast {
      val WATT_LIMIT = 300L
      val WATT_PRICE = NativeCoin.amount(1)
      val ENDPOINT = "http://localhost:8080/api/public"
    }
  }

  final case class GenAddress(output: Option[String] = None) extends PravdaConfig

  final case class Broadcast(mode: Broadcast.Mode = Broadcast.Mode.Nope,
                             wallet: Option[String] = None,
                             programWallet: Option[String] = None,
                             wattPayerWallet: Option[String] = None,
                             input: Option[String] = None,
                             dryRun: Boolean = false,
                             wattLimit: Long = DefaultValues.Broadcast.WATT_LIMIT,
                             wattPrice: NativeCoin = DefaultValues.Broadcast.WATT_PRICE,
                             endpoint: String = DefaultValues.Broadcast.ENDPOINT)
      extends PravdaConfig

  object Broadcast {

    sealed trait Mode

    object Mode {
      case object Nope                                                    extends Mode
      case object Deploy                                                  extends Mode
      case object Run                                                     extends Mode
      case object Update                                                  extends Mode
      case object Seal                                                    extends Mode
      final case class Transfer(to: Option[String], amount: Option[Long]) extends Mode
    }
  }

  final case class Compile(compiler: CompileMode,
                           input: List[String] = List.empty,
                           output: Option[String] = None,
                           mainClass: Option[String] = None)
      extends PravdaConfig

  final case class RunBytecode(storage: Option[String] = None,
                               input: Option[String] = None,
                               executor: String = DefaultExecutor)
      extends PravdaConfig

  object Node {

    sealed trait Network

    object Network {
      final case class Local(coinDistribution: Option[String]) extends Network

      case object Testnet extends Network
    }

    sealed trait Mode

    object Mode {
      case object Nope extends Mode
      case object Run  extends Mode

      final case class Init(network: Network) extends Mode
    }
  }

  final case class Node(mode: Node.Mode, dataDir: Option[String]) extends PravdaConfig

  sealed trait CodegenMode

  object CodegenMode {
    case object Dotnet extends CodegenMode
  }

  final case class Codegen(codegenMode: CodegenMode,
                           input: Option[String] = None)
      extends PravdaConfig
}
