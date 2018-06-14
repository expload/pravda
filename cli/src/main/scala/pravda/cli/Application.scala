package pravda.cli

import cats.implicits._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pravda.cli.languages.impl._
import pravda.cli.programs._
import pravda.cmdopt.CommandLine.Ok
import pravda.cmdopt.CommandLine.ParseError
import pravda.cmdopt.CommandLine.HelpWanted

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import sys.process.stderr
import scala.concurrent.duration._

/**
  * Pravda CLI entry point.
  */
object Application extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  lazy val nodeLanguage = new NodeLanguageImpl()
  lazy val io = new IoLanguageImpl()
  lazy val compilers = new CompilersLanguageImpl()
  lazy val random = new RandomLanguageImpl()
  lazy val vm = new VmLanguageImpl()

  lazy val compile = new Compile(io, compilers)
  lazy val genAddress = new GenAddress(io, random)
  lazy val runner = new RunBytecode(io, vm)
  lazy val broadcast = new Broadcast(io, nodeLanguage, compilers)
  lazy val nodeProgram = new Node(io, random, nodeLanguage)

  // FIXME programs should be composed by another one
  val eventuallyExitCode = ArgumentsParser.parse(args, Config.Nope) match {
    case Ok(config: Config.Compile)     => compile(config).map(_ => 0)
    case Ok(config: Config.RunBytecode) => runner(config).map(_ => 0)
    case Ok(config: Config.GenAddress)  => genAddress(config).map(_ => 0)
    case Ok(config: Config.Broadcast)   => broadcast(config).map(_ => 0)
    case Ok(config: Config.Node)        => nodeProgram(config).map(_ => 0)
    case Ok(Config.Nope)                =>
      Future{
        import pravda.cmdopt.instances.show.console._
        print( ArgumentsParser.help() )
        0
      }
    case HelpWanted(cli) =>
      Future {
        import pravda.cmdopt.instances.show.console._
        stderr.print( cli.help() )
        0 // every non zero exit code says about error
      }
    case ParseError(msg) =>
      Future {
        import pravda.cmdopt.instances.show.console._
        stderr.println(msg)
        stderr.print( ArgumentsParser.help() )
        1 // every non zero exit code says about error
      }
  }

  // FIXME handle exceptions
  val exitCode = Await.result(
    awaitable = for (exitCode <- eventuallyExitCode; _ <- system.terminate()) yield exitCode,
    atMost = 1.day // should be enough :)
  )

  sys.exit(exitCode)
}
