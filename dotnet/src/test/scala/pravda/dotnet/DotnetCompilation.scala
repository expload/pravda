package pravda.dotnet
import java.nio.file.{Files, Path, Paths}

import cats.implicits._
import pravda.dotnet.parser.FileParser
import pravda.dotnet.parser.FileParser.{ParsedDotnetFile, ParsedPdb, ParsedPe}

final case class DotnetCompilationStep(target: String, sources: Seq[String], optimize: Boolean = false)

final case class DotnetCompilation(steps: Seq[DotnetCompilationStep],
                                   `main-class`: Option[String] = None)

object DotnetCompilation {

  val pravdaDir = Paths.get("/tmp/pravda")

  private def readFileBytes(p: Path) = Files.readAllBytes(p)

  private def parsePeFile(p: Path): Either[String, ParsedPe] =
    FileParser.parsePe(readFileBytes(p))

  private def parsePdbFile(p: Path): Either[String, ParsedPdb] =
    FileParser.parsePdb(readFileBytes(p))

  def run(compilation: DotnetCompilation): Either[String, List[ParsedDotnetFile]] = {

    val (_, filesE) = compilation.steps.foldLeft(
      (Vector.empty[String], Vector.empty[Either[String, ParsedDotnetFile]])) {
      case ((labels, res), DotnetCompilationStep(target, sources, optimize)) =>
        if (!target.endsWith(".dll") && !target.endsWith(".exe")) {
          (labels, res :+ Left(s"Unknown extension of target file: $target"))
        } else {
          sources.find(s => !s.endsWith(".dll") && !s.endsWith(".cs")) match {
            case None =>
              def restorePath(s: String): Path =
                if (labels.contains(s)) pravdaDir.resolve(s) else Paths.get(s)

              val dlls = sources.filter(_.endsWith(".dll")).map(s => restorePath(s))
              val css = sources.filter(_.endsWith(".cs")).map(s => restorePath(s))
              val targetP = pravdaDir.resolve(target)
              val pdbP = pravdaDir.resolve(target.dropRight(4) + ".pdb")
              val isTargetDll = target.endsWith(".dll")

              val command =
                s"""
                  |csc ${css.map(_.toAbsolutePath.toString).mkString(" ")}
                  |-out:${targetP.toAbsolutePath.toString}
                  |${dlls.map(dll => s"-reference:${dll.toAbsolutePath.toString}").mkString("\n")}
                  |-debug:portable
                  |-pdb:${pdbP.toAbsolutePath.toString}
                  |${if (isTargetDll) "-target:library" else ""}
                """.stripMargin.trim.replace("\n\n", "\n")

              def errorWithCommand(err: String) =
                s"""
                 |command:
                 |$command
                 |error:
                 |$err
                """.stripMargin

              val commandE = {
                val stdoutS = StringBuilder.newBuilder
                val stderrS = StringBuilder.newBuilder

                {
                  import scala.sys.process._
                  command.!(new ProcessLogger {
                    override def out(s: => String): Unit = {
                      stdoutS ++= s
                      stdoutS += '\n'
                    }
                    override def err(s: => String): Unit = {
                      stderrS ++= s
                      stderrS += '\n'
                    }
                    override def buffer[T](f: => T): T = f
                  })
                }
                val msgLines = stdoutS.mkString.lines.toList ++ stderrS.mkString.lines.toList
                val errorLines = msgLines.filter(_.contains("error"))
                if (errorLines.isEmpty) {
                  Right(())
                } else {
                  Left(errorWithCommand(errorLines.mkString("\n")))
                }
              }

              val parsed = for {
                _ <- commandE
                pe <- parsePeFile(targetP)
                pdb <- parsePdbFile(pdbP)
              } yield ParsedDotnetFile(pe, Some(pdb))

              (labels :+ target, res :+ parsed)
            case Some(s) => (labels, res :+ Left(s"Unknown extension of source file: $s"))
          }
        }
    }

    filesE.toList.sequence
  }
}
