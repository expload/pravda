package pravda.dotnet

package parser

import pravda.dotnet.data.Heaps.SequencePoint
import pravda.dotnet.data.TablesData._
import utest._

// all *.exe files was compiled by csc *.cs

object MiscTests extends TestSuite {

  val tests = Tests {
    'helloWorldParse - {
      val Right(pdb) = parsePdbFile("hello_world.pdb")

      pdb.tablesData.methodDebugInformationTable ==> Vector(
        MethodDebugInformationData(Some("/tmp/pravda/hello_world.cs"),
                                   List(SequencePoint(0, 6, 5, 6, 6),
                                        SequencePoint(1, 7, 9, 7, 43),
                                        SequencePoint(12, 8, 5, 8, 6))),
        MethodDebugInformationData(None, List())
      )
    }
  }
}
