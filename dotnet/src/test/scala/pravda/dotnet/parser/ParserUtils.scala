package pravda.dotnet.parser

import java.io.File

import fastparse.all._
import pravda.common.UtestUtils

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object ParserUtils {
  final case class ParserTest(sections: List[(String, String)])

  def parseAstTest(text: String): ParserTest = {
    var sectionName = Option.empty[String]
    var section = ArrayBuffer.empty[String]
    var sections = ListBuffer.empty[(String, String)]

    text.lines.foreach { line =>
      if (line.startsWith("###")) {
        val name = sectionName.getOrElse("")
        sections += name -> section.mkString("\n")

        sectionName = Some(line.stripPrefix("###"))
        section.clear()
      } else {
        section += line
      }
    }

    val name = sectionName.getOrElse("")
    sections += name -> section.mkString("\n")

    ParserTest(sections.toList)
  }

  def constructTests(dir: File): utest.Tests =
    UtestUtils.constructTestsFromDir[(dir, "ast", f => Right(f), f => )
}
