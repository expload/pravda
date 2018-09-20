/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.proverka
import java.io.{File, PrintWriter}

import utest._
import utest.framework.{TestCallTree, Tree}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.Source

object Proverka {
  type Error = String
  type Text = String

  final case class Section(name: String, text: Text)

  sealed trait SchemePart[State] {
    def sectionName: String
  }
  final case class InputPart[State](sectionName: String, parse: Text => Either[Error, State => State])
      extends SchemePart[State]
  final case class OutputPart[State](sectionName: String, produce: State => Either[Error, Text])
      extends SchemePart[State]
}

trait Proverka extends TestSuite {
  import Proverka._

  def dir: File
  def ext: String = ".sct"

  type State
  def scheme: Seq[SchemePart[State]]
  def initState: State

  val tests = constructTests(false)

  private def validateScheme: Option[String] = {
    if (scheme.distinct.length != scheme.length) {
      Some("Scheme must not contain parts with equal names")
    } else {
      None
    }
  }

  private def parseSections(text: Text): List[Section] = {
    var sectionName = Option.empty[String]
    val section = ArrayBuffer.empty[String]
    val sections = ListBuffer.empty[Section]

    def addSection() = {
      val name = sectionName.getOrElse("")
      sections += Section(name, section.mkString("\n"))
      section.clear()
    }

    text.lines.foreach { line =>
      if (line.startsWith("---")) {
        if (sectionName.isDefined) addSection()
        sectionName = Some(line.stripPrefix("---"))
      } else {
        section += line
      }
    }

    addSection()

    sections.toList
  }

  private def applyScheme(sections: List[Section]): Either[Error, State] = {
    def aux(s: State, curScheme: List[SchemePart[State]]): Either[Error, State] =
      curScheme match {
        case head :: tail =>
          val sectO = sections.find(_.name == head.sectionName)
          sectO match {
            case Some(sect) =>
              head match {
                case Proverka.InputPart(_, parse) =>
                  for {
                    toS <- parse(sect.text)
                    res <- aux(toS(s), tail)
                  } yield res
                case _ => Right(s)
              }
            case None => Left(s"${head.sectionName} section is missing")
          }
        case _ => Right(s)
      }

    if (sections.map(_.name).sorted != scheme.map(_.sectionName).sorted) {
      Left("Scheme and sections don't match each other")
    } else {
      aux(initState, scheme.toList)
    }
  }

  private def constructTests(overwrite: Boolean): utest.Tests = {

    def walkDir(prefix: String, dir: File): Option[(Tree[String], TestCallTree)] =
      if (dir.isDirectory) {
        val files = dir.listFiles.filter(f => f.isFile && f.getName.endsWith(s".$ext"))
        val fileNames = files.map(f => Tree(f.getName.stripSuffix(s".$ext")))
        val fileCalls = files.map(f =>
          new TestCallTree(Left {
            val sections = parseSections(Source.fromFile(f).mkString)
            val state = applyScheme(sections)

            if (overwrite) {
              val generatedSections = scheme
                .map {
                  case Proverka.InputPart(sectionName, _) =>
                    sections.find(_.name == sectionName) match {
                      case Some(s) => Right(sectionName -> s.text)
                      case None    => Left(s"$sectionName section is missing")
                    }
                  case Proverka.OutputPart(sectionName, produce) =>
                    for {
                      s <- state
                      test <- produce(s)
                    } yield sectionName -> test
                }
                .map {
                  case Left(err) =>
                    throw new java.lang.AssertionError("assertion failed: " + err)
                  case Right(pair) =>
                    pair
                }
                .toMap

              val generatedFile = sections
                .flatMap { s =>
                  for {
                    text <- generatedSections.get(s.name)
                  } yield s"---${s.name}\n$text"
                }
                .mkString("\n")

              val pw = new PrintWriter(f)
              pw.write(generatedFile)
              pw.close()
            } else {
              scheme.foreach {
                case Proverka.OutputPart(sectionName, produce) =>
                  val sectionText = sections.find(_.name == sectionName) match {
                    case Some(s) => Right(s.text)
                    case None    => Left(s"$sectionName section is missing")
                  }

                  val res =
                    for {
                      target <- sectionText
                      s <- state
                      test <- produce(s)
                    } yield {
                      test ==> target
                    }

                  res.left.foreach(err => Predef.assert(false, s"${f.getName}: $err"))

                case _ =>
              }
            }
          }))

        val dirs = dir.listFiles.filter(_.isDirectory)
        val (dirTrees, dirCallTrees) = dirs.flatMap(d => walkDir(d.getName, d)).unzip

        if (files.nonEmpty || dirTrees.nonEmpty) {
          Some((Tree(prefix, fileNames ++ dirTrees: _*), new TestCallTree(Right(fileCalls ++ dirCallTrees))))
        } else {
          None
        }
      } else {
        None
      }

    val errO = validateScheme
    errO match {
      case Some(err) =>
        Tests(Tree("scheme validation"), new TestCallTree(Left(Predef.assert(false, err))))
      case None =>
        val (tree, callTree) = walkDir("", dir).getOrElse((Tree(""), new TestCallTree(Right(IndexedSeq.empty))))
        Tests(tree, callTree)
    }
  }

  def main(args: Array[String]): Unit = {
    val overwrite = args.contains("--overwrite")
    if (!overwrite) {
      println("Running proverka tests...")
      TestRunner.runAndPrint(constructTests(false), dir.getName)
    } else {
      println("Overwriting proverka tests...")
      TestRunner.runAndPrint(constructTests(true), dir.getName)
    }
  }
}
