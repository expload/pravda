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

package pravda.plaintest
import java.io.{File, PrintWriter}

import org.json4s._
import pravda.yaml4s
import utest._
import utest.framework.{TestCallTree, Tree}

import scala.io.Source
import scala.util.Try

abstract class Plaintest[Input: Manifest, Output: Manifest] extends TestSuite {
  def dir: File
  def ext: String = "yaml"
  def allowOverwrite: Boolean = false
  def formats: Formats = DefaultFormats

  def produce(input: Input): Either[String, Output]

  val tests = constructTests(false)

  private def parseTest(yaml: String): Either[String, (Input, Output)] =
    yaml4s.parseAllYamlOpt(yaml, false) match {
      case Some(io) =>
        io match {
          case List(input, output) =>
            for {
              i <- Try { input.extract[Input](formats, Manifest[Input]) }.toEither.left.map(_.toString)
              o <- Try { output.extract[Output](formats, Manifest[Output]) }.toEither.left.map(_.toString)
            } yield (i, o)
          case _ => Left("File must contain exactly two yaml documents")
        }
      case None => Left("Couldn't parse yaml file")
    }

  private def constructTests(overwrite: Boolean): utest.Tests = {

    def walkDir(prefix: String, dir: File): Option[(Tree[String], TestCallTree)] =
      if (dir.isDirectory) {
        val files = dir.listFiles.filter(f => f.isFile && f.getName.endsWith(s".$ext"))
        val fileNames = files.map(f => Tree(f.getName.stripSuffix(s".$ext")))
        val fileCalls = files.map(f =>
          new TestCallTree(Left {
            val test = parseTest(Source.fromFile(f).mkString)
            test match {
              case Right((input, output)) =>
                if (overwrite) {
                  if (allowOverwrite) {
                    produce(input) match {
                      case Right(correct) =>
                        val pw = new PrintWriter(f)
                        pw.write(yaml4s.renderYaml(Extraction.decompose(input)(formats)))
                        pw.write("---\n")
                        pw.write(yaml4s.renderYaml(Extraction.decompose(correct)(formats)))
                        pw.close()
                      case Left(err) =>
                        Predef.assert(false, s"${f.getName}: $err")
                    }
                  } else {
                    Predef.assert(false, s"${f.getName}: Overwriting is not allowed")
                  }
                } else {
                  produce(input) match {
                    case Right(res) => res ==> output
                    case Left(err)  => Predef.assert(false, s"${f.getName}: $err")
                  }
                }
              case Left(err) => Predef.assert(false, s"${f.getName}: $err")
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

    val (tree, callTree) = walkDir("", dir).getOrElse((Tree(""), new TestCallTree(Right(IndexedSeq.empty))))
    Tests(tree, callTree)
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
