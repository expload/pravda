package pravda.cli

import cats.implicits._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pravda.cli.languages.impl._
import pravda.cli.programs._

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

  lazy val api = new NodeApiLanguageImpl()
  lazy val io = new IoLanguageImpl()
  lazy val compilers = new CompilersLanguageImpl()
  lazy val random = new RandomLanguageImpl()
  lazy val vm = new VmLanguageImpl()

  lazy val compile = new Compile(io, compilers)
  lazy val genAddress = new GenAddress(io, random)
  lazy val runner = new RunBytecode(io, vm)
  lazy val broadcast = new Broadcast(io, api, compilers)

  val eventuallyExitCode = Parser.parse(args, Config.Nope) match {
    case Some(config: Config.Compile)     => compile(config).map(_ => 0)
    case Some(config: Config.RunBytecode) => runner(config).map(_ => 0)
    case Some(config: Config.GenAddress)  => genAddress(config).map(_ => 0)
    case Some(config: Config.Broadcast)   => broadcast(config).map(_ => 0)
    case _ =>
      Future {
        stderr.println(Parser.renderTwoColumnsUsage)
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
