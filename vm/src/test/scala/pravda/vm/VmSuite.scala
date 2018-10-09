package pravda.vm

import java.io.File

import pravda.proverka._
import fastparse.all._

object VmSuite extends Proverka {
  lazy val dir = new File("vm/src/test/resources")
  override lazy val ext = "sbox"

  type State = VmSandbox.Case
  lazy val initState: VmSandbox.Case = VmSandbox.Case()

  lazy val scheme = Seq(
    parserInput("preconditions")(VmSandbox.preconditions.map(p => s => s.copy(preconditions = Some(p)))),
    parserInput("code")(VmSandbox.program.map(p => s => s.copy(program = Some(p)))),
    textOutput("expectations") { c =>
      val res = for {
        pre <- c.preconditions
        prog <- c.program
      } yield VmSandbox.printExpectations(VmSandbox.sandboxRun(prog, pre))

      res.toRight("preconditions or program is missing")
    }
  )
}
