package pravda.dotnet
import java.nio.file._

import cats.implicits._
import pravda.dotnet.parser.FileParser
import pravda.dotnet.parser.FileParser.{ParsedDotnetFile, ParsedPdb, ParsedPe}

final case class DotnetCompilationStep(target: String, sources: Seq[String], optimize: Boolean = false)

final case class DotnetCompilation(steps: Seq[DotnetCompilationStep], `main-class`: Option[String] = None)

object DotnetCompilation {

  object dsl {
    def steps(pairs: (String, Seq[String])*): DotnetCompilation =
      DotnetCompilation(pairs.map { case (target, sources) => DotnetCompilationStep(target, sources) })

    implicit class DotnetCompilationOps(dc: DotnetCompilation) {
      def withMainClass(mainClass: String): DotnetCompilation = dc.copy(`main-class` = Some(mainClass))

      def run: Either[String, List[ParsedDotnetFile]] = DotnetCompilation.run(dc)
    }
  }

  val pravdaDir = Paths.get("/tmp/pravda")

  private def readFileBytes(p: Path) = Files.readAllBytes(p)

  private def parsePeFile(p: Path): Either[String, ParsedPe] =
    FileParser.parsePe(readFileBytes(p))

  private def parsePdbFile(p: Path): Either[String, ParsedPdb] =
    FileParser.parsePdb(readFileBytes(p))

  def run(compilation: DotnetCompilation): Either[String, List[ParsedDotnetFile]] = {

    Files.createDirectories(pravdaDir)

    val (_, filesE) =
      compilation.steps.foldLeft((Vector.empty[String], Vector.empty[Either[String, ParsedDotnetFile]])) {
        case ((labels, res), DotnetCompilationStep(target, sources, optimize)) =>
          if (!target.endsWith(".dll") && !target.endsWith(".exe")) {
            (labels, res :+ Left(s"Unknown extension of target file: $target"))
          } else {
            sources.find(s => !s.endsWith(".dll") && !s.endsWith(".cs")) match {
              case None =>
                def restorePath(s: String): Path =
                  if (labels.contains(s)) {
                    pravdaDir.resolve(s)
                  } else {
                    val path = Paths.get(s)
                    val copyPath = pravdaDir.resolve(path.getFileName)
                    Files.copy(path, copyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
                    copyPath
                  }

                val dlls = sources.filter(_.endsWith(".dll")).map(s => restorePath(s))
                val css = sources.filter(_.endsWith(".cs")).map(s => restorePath(s))
                val targetP = pravdaDir.resolve(target)
                val pdbP = pravdaDir.resolve(target.dropRight(4) + ".pdb")

                val isChanged =
                  if (Files.exists(targetP)) {
                    val allTimes = dlls.map(p => Files.getLastModifiedTime(p)) ++ css.map(p =>
                      Files.getLastModifiedTime(p))
                    allTimes.map(_.toMillis).max > Files.getLastModifiedTime(targetP).toMillis
                  } else {
                    true
                  }

                val commandE = if (isChanged) {
                  val command =
                    s"""
                      |csc ${css.map(_.toAbsolutePath.toString).mkString(" ")}
                      |-out:${targetP.toAbsolutePath.toString}
                      |${dlls.map(dll => s"-reference:${dll.toAbsolutePath.toString}").mkString("\n")}
                      |-debug:portable
                      |-pdb:${pdbP.toAbsolutePath.toString}
                      |${if (target.endsWith(".dll")) "-target:library" else ""}
                      |${if (optimize) "-optimize+" else ""}
                    """.stripMargin.trim.replace("\n\n", "\n")

                  def errorWithCommand(err: String) =
                    s"""
                     |command:
                     |$command
                     |error:
                     |$err
                    """.stripMargin

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
                } else {
                  Right(())
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
