package pravda.dotnet

package translation

import java.io.File

import pravda.proverka._
import pravda.vm.asm
import pravda.vm.asm.PravdaAssembler

object TranslationSuite extends Proverka {
  lazy val dir = new File("dotnet/src/test/resources")
  override lazy val ext = "trs"

  final case class Translation(ops: List[asm.Operation] = List.empty)

  type State = Translation
  lazy val initState = Translation()

  lazy val scheme = Seq(
    input("exe") { line =>
      val parts = line.split("\\s+").toList
      val exe = parts.head
      val pdbE = if (parts.length > 1) {
        parsePdbFile(parts(1)).map(p => Some(p._2))
      } else {
        Right(None)
      }
      for {
        pe <- parsePeFile(exe)
        pdb <- pdbE
        (_, cilData, methods, signatures) = pe
        asm <- Translator.translateAsm(methods, cilData, signatures, pdb).left.map(_.mkString)
      } yield (s: State) => s.copy(ops = asm)
    },
    textOutput("translation") { s =>
      Right(PravdaAssembler.render(s.ops))
    }
  )
}
