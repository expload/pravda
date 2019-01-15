/*
 * Copyright (C) 2018  Expload
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

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import pravda.cli.PravdaArgsParser
import pravda.vm.{Data, StandardLibrary}
import pravda.vm.asm.Operation
import pravda.vm.operations.annotation.OpcodeImplementation

import cats.data._
import cats.data.Validated._
import cats.implicits._

object GenDocs extends App {

  final val DoNotEditWarn =
    s"""<!--
       |THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
       |-->""".stripMargin

  private def table(header: String*)(rows: List[List[String]])(): String = {

    val table = header.toList :: rows

    // measure columns
    val sizes = table.foldLeft(List.fill(table.head.length)(0)) {
      case (acc, xs) =>
        acc.zip(xs).map {
          case (l, r) => Math.max(l, r.length)
        }
    }

    val headerView :: rowsView = table map { cols =>
      cols
        .zip(sizes)
        .map { case (col, size) => col + (" " * (size - col.length)) }
        .mkString("|")
    }

    val hline = sizes.map("-" * _).mkString("|")

    s"$headerView\n$hline\n${rowsView.mkString("\n")}"
  }

  private def writeToFile(f: File, content: Array[Byte], check: Boolean): ValidatedNel[String, Unit] =
    if (check) {
      if (!f.exists() || !f.isFile) {
        s"File ${f.getAbsoluteFile} doesn't exist.".invalidNel
      } else {
        val equals = Files.readAllBytes(Paths.get(f.getAbsolutePath)).sameElements(content)
        if (!equals) {
          s"File ${f.getAbsoluteFile} doesn't correspond to actual docs.".invalidNel
        } else {
          ().validNel
        }
      }
    } else {
      if (!f.getParentFile.exists()) {
        f.getParentFile.mkdirs()
      }
      Files.write(Paths.get(f.getAbsolutePath), content)
      ().validNel
    }

  private def writeToFile(f: File, content: String, check: Boolean): ValidatedNel[String, Unit] =
    writeToFile(f, content.getBytes(StandardCharsets.UTF_8), check)

  def genCliDocs(check: Boolean = false): ValidatedNel[String, Unit] = {

    val outDir = new File("doc/CLI")
    val mainPageName = "index.md"

    val mainPage = (
      new File(outDir, mainPageName),
      PravdaArgsParser.root.toMarkdown.getBytes(StandardCharsets.UTF_8)
    )

    val pages = PravdaArgsParser.paths.map { path =>
      val name = new File(outDir, s"${path.toString}.md")
      val content = path.toMarkdown
      val bcontent = content.getBytes(StandardCharsets.UTF_8)
      (name, bcontent)
    } :+ mainPage

    pages.map { case (f, content) => writeToFile(f, content, check) }.sequence_
  }

  def genOpcodesDocs(check: Boolean = false): ValidatedNel[String, Unit] = {

    val operationModules = List(
      classOf[pravda.vm.operations.DataOperations],
      classOf[pravda.vm.operations.NativeCoinOperations],
      classOf[pravda.vm.operations.SystemOperations],
      classOf[pravda.vm.operations.ControlOperations],
      classOf[pravda.vm.operations.StackOperations],
      classOf[pravda.vm.operations.StorageOperations],
      classOf[pravda.vm.operations.ArithmeticOperations],
      classOf[pravda.vm.operations.HeapOperations],
      classOf[pravda.vm.operations.LogicalOperations]
    )

    val descriptionByOpcde = operationModules.flatMap { `class` =>
      `class`.getMethods.toList.flatMap { method =>
        Option(method.getAnnotation(classOf[OpcodeImplementation])).toList.map { impl =>
          impl.opcode -> impl.description
        }
      }
    }.toMap

    val view = table("Code", "Mnemonic", "Description") {
      Operation.mnemonicByOpcode.toList.sortBy(_._1) map {
        case (opcode, mnemonic) =>
          List("0x%02X".format(opcode), mnemonic, descriptionByOpcde.getOrElse(opcode, ""))
      }
    }

    writeToFile(
      new File("doc/virtual-machine/opcodes.md"),
      s"""$DoNotEditWarn
         |# Pravda VM opcodes
         |$view""".stripMargin,
      check
    )
  }

  def genStdlibDocs(check: Boolean = false): ValidatedNel[String, Unit] = {

    def nameToFileName(x: String) = {
      x.charAt(0).toLower + x.toList.tail
        .flatMap {
          case s if s.isUpper => List('-', s.toLower)
          case s              => List(s)
        }
        .mkString
        .stripSuffix("$")
    }

    val docs = StandardLibrary.All.map { f =>
      val fileName = nameToFileName(f.name)

      def typeToName(x: Data.Type) = x match {
        case Data.Type.Bytes   => "bytes"
        case Data.Type.Number  => "number"
        case Data.Type.Null    => "null"
        case Data.Type.Boolean => "bool"
        case Data.Type.Utf8    => "utf8"
        case Data.Type.Array   => "array"
        case Data.Type.BigInt  => "bigint"
        case Data.Type.Struct  => "struct"
        case Data.Type.Ref     => "ref"
        case Data.Type.Int8    => "int8"
        case Data.Type.Int16   => "int16"
        case Data.Type.Int32   => "int32"
        case Data.Type.Int64   => "int64"
      }

      def typesToName(types: Seq[Data.Type]) = {
        types.map(typeToName).mkString(" | ")
      }

      fileName -> s"""## ${f.name}
         |
         |### Id
         |
         |`${"0x%02X".format(f.id)}`
         |### Signature
         |
         |`(${f.args.map { case (n, t) => s"$n: ${typesToName(t)}" }.mkString(", ")}): ${typesToName(f.returns)}`
         |
         |### Description
         |
         |${f.description}
         |""".stripMargin
    }

    val docsRes: ValidatedNel[String, Unit] = docs
      .map {
        case (name, content) =>
          writeToFile(
            new File(s"doc/standard-library/$name.md"),
            s"$DoNotEditWarn\n$content",
            check
          )
      }
      .toList
      .sequence_

//    val stdlibRes = {
//      val stdlibTable = table("Name", "Description") {
//        StandardLibrary.All.toList.map { f =>
//          List(s"[${f.name}](${nameToFileName(f.name)}.md)", f.description)
//        }
//      }
//      writeToFile(
//        new File(s"doc/standard-library/index.md"),
//        s"$DoNotEditWarn\n$stdlibTable",
//        check
//      )
//    }

    docsRes
    //(docsRes, stdlibRes).mapN { case _ => () }
  }

  val check = args.contains("--check")

  val res: ValidatedNel[String, Unit] = (genCliDocs(check), genOpcodesDocs(check), genStdlibDocs(check)).mapN {
    case _ => ()
  }

  res match {
    case Validated.Invalid(e) =>
      println(e.toList.mkString("\n"))
      sys.exit(1)
    case _ =>
  }
}
