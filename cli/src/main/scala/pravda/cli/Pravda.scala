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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.implicits._
import pravda.node.client.impl._
import pravda.cli.programs._
import pravda.yopt.CommandLine.{HelpNeeded, Ok, ParseError}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.sys.process.stderr

/**
  * Pravda CLI entry point.
  */
object Pravda extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  lazy val nodeLanguage = new NodeLanguageImpl()
  lazy val io = new IoLanguageImpl()
  lazy val compilers = new CompilersLanguageImpl()
  lazy val random = new RandomLanguageImpl()
  lazy val vm = new VmLanguageImpl()
  lazy val codeGenerators = new CodeGeneratorsLanguageImpl()

  lazy val compile = new Compile(io, compilers)
  lazy val genAddress = new GenAddress(io, random)
  lazy val runner = new RunBytecode(io, vm)
  lazy val broadcast = new Broadcast(io, nodeLanguage, compilers)
  lazy val nodeProgram = new Node(io, random, nodeLanguage)
  lazy val codegen = new Codegen(io, codeGenerators)

  // FIXME programs should be composed by another one
  val eventuallyExitCode = PravdaArgsParser.parse(args.toList, PravdaConfig.Nope) match {
    case Ok(config: PravdaConfig.Compile)     => compile(config).map(_ => 0)
    case Ok(config: PravdaConfig.RunBytecode) => runner(config).map(_ => 0)
    case Ok(config: PravdaConfig.GenAddress)  => genAddress(config).map(_ => 0)
    case Ok(config: PravdaConfig.Broadcast)   => broadcast(config).map(_ => 0)
    case Ok(config: PravdaConfig.Node)        => nodeProgram(config).map(_ => 0)
    case Ok(config: PravdaConfig.Codegen)     => codegen(config).map(_ => 0)
    case Ok(PravdaConfig.Nope) =>
      Future {
        print(PravdaArgsParser.root.toHelpString)
        0
      }
    case HelpNeeded(cli) =>
      Future {
        print(cli.toHelpString)
        0
      }
    case ParseError(msg) =>
      Future {
        stderr.println(msg)
        stderr.print(PravdaArgsParser.root.toHelpString)
        1 // every non zero exit code says about error
      }
  }

  // FIXME handle exceptions
  val exitCode = Await.result(
    awaitable = for (exitCode <- eventuallyExitCode; _ <- system.terminate()) yield exitCode,
    atMost = 5.days // FIXME: node run is long operation, so we should not stop it
  )

  sys.exit(exitCode)
}
