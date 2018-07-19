import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.google.protobuf.ByteString
import pravda.cli.PravdaArgsParser
import pravda.vm.{Data, StandardLibrary}
import pravda.vm.asm.Operation
import pravda.vm.operations.annotation.OpcodeImplementation

import scala.language.higherKinds

object GenDocs extends App {

  final val DoNotEditWarn = s"<!--\nTHIS FILE IS GENERATED. DO NOT EDIT MANUALLY!\n-->\n"

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

  def genCliDocs(): Unit = {

    val outDir = new File("doc/ref/cli")
    val mainPageName = "main.md"

    val mainPage = (
      new File(outDir, mainPageName).getAbsolutePath,
      ByteString.copyFrom(PravdaArgsParser.root.toMarkdown, StandardCharsets.UTF_8)
    )

    val pages = PravdaArgsParser.paths.map { path =>
      val name = new File(outDir, s"${path.toString}.md")
      val content = path.toMarkdown
      val bcontent = ByteString.copyFrom(content, StandardCharsets.UTF_8)
      (name.getAbsolutePath, bcontent)
    } :+ mainPage

    outDir.mkdirs()

    for ((name, content) <- pages) {
      Files.write(Paths.get(name), content.toByteArray)
    }
  }

  def genOpcodesDocs() = {

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

    new PrintWriter("doc/ref/vm/opcodes.md") {
      write(DoNotEditWarn)
      write("# Pravda VM opcodes\n")
      write(view)
      close()
    }
  }

  def genStdlibDocs() = {

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
        case Data.Type.Uint8   => "uint8"
        case Data.Type.Uint16  => "uint16"
        case Data.Type.Uint32  => "uint32"
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

    val dir = new File("doc/ref/vm/stdlib")
    if (!dir.exists()) dir.mkdirs()

    docs.foreach {
      case (name, content) =>
        new PrintWriter(s"doc/ref/vm/stdlib/$name.md") {
          write(DoNotEditWarn)
          write(content)
          close()
        }
        ()
    }

    new PrintWriter(s"doc/ref/vm/stdlib.md") {
      write(DoNotEditWarn)
      write(
        table("Name", "Description") {
          StandardLibrary.All.toList.map { f =>
            List(s"[${f.name}](stdlib/${nameToFileName(f.name)}.md)", f.description)

          }
        }
      )
      close()
    }
  }

  genCliDocs()
  genOpcodesDocs()
  genStdlibDocs()
}
