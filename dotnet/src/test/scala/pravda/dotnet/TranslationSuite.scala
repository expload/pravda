package pravda.dotnet
import java.io.File

import pravda.dotnet.translation.Translator
import pravda.proverka.{Proverka, input, textOutput}
import pravda.vm.asm
import pravda.vm.asm.PravdaAssembler

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object TranslationSuite extends Proverka {
  lazy val dir = new File("dotnet/src/test/resources/translation")
  override lazy val ext = "trs"

  final case class Translation(ops: List[asm.Operation] = List.empty)

  type State = Translation
  lazy val initState = Translation()

  lazy val scheme = Seq(
    input("exe") { txt =>
      val lines = txt.lines.toList
      val parts = lines.head.split("\\s+").toList
      val mainClass = lines.tail.headOption
      val filesE = parts
        .groupBy(_.dropRight(4))
        .map {
          case (prefix, files) =>
            val exeO = files.find(_ == s"$prefix.exe")
            val dllO = files.find(_ == s"$prefix.dll")
            val pdbO = files.find(_ == s"$prefix.pdb")

            (exeO, dllO) match {
              case (Some(exe), Some(dll)) =>
                Left(s".dll and .exe files have the same name: $exe, $dll")
              case (None, None) =>
                Left(s".dll or .exe is not specified: $prefix")
              case (Some(exe), None) =>
                parseDotnetFile(exe, pdbO)
              case (None, Some(dll)) =>
                parseDotnetFile(dll, pdbO)
            }
        }
        .toList.sequence

      for {
        files <- filesE
        asm <- Translator.translateAsm(files, mainClass).left.map(_.mkString)
      } yield (s: State) => s.copy(ops = asm)
    },
    textOutput("translation") { s =>
      Right(PravdaAssembler.render(s.ops))
    }
  )
}
