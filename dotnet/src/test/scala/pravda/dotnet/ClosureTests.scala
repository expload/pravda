package pravda.dotnet

import utest._

object ClosureTests extends TestSuite {

  val tests = Tests {
    'closureParse - {
      //val Right((_, _, methods, signatures)) = FileParser.parsePe("closure.exe")
      //FIXME closure types aren't parsed yet
    }
  }
}
