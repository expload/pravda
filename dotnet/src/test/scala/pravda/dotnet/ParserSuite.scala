package pravda.dotnet

import java.io.File

import pravda.proverka._
import pravda.dotnet.data.Method
import pravda.dotnet.parser.Signatures.Signature

object ParserSuite extends Proverka {
  lazy val dir = new File("dotnet/src/test/resources/parser")
  override lazy val ext = "prs"

  final case class Parsing(methods: List[Method] = List.empty, signatures: List[(Long, Signature)] = List.empty)

  type State = Parsing
  lazy val initState = Parsing()

  lazy val scheme = Seq(
    input("exe") { exe =>
      for {
        pe <- parsePeFile(exe)
      } yield (s: State) => s.copy(methods = pe.methods, signatures = pe.signatures.toList.sortBy(_._1))
    },
    output("methods") { s =>
      Right(s.methods)
    },
    output("signatures") { s =>
      Right(s.signatures)
    }
  )
}
