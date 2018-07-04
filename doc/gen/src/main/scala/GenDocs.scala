import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import com.google.protobuf.ByteString
import pravda.cli.PravdaArgsParser
import pravda.vm.asm.Operation
import pravda.vm.operations.annotation.OpcodeImplementation

import scala.language.higherKinds

object GenDocs extends App {

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

  def genVmDocs() = {

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

    val table = List("Code", "Mnemonic", "Description") :: {
      Operation.mnemonicByOpcode.toList.sortBy(_._1) map {
        case (opcode, mnemonic) =>
          List("0x%02X".format(opcode), mnemonic, descriptionByOpcde.getOrElse(opcode, ""))
      }
    }

    // measure columns
    val sizes = table.foldLeft(List.fill(table.head.length)(0)) {
      case (acc, xs) =>
        acc.zip(xs).map {
          case (l, r) => Math.max(l, r.length)
        }
    }

    val header :: rows = table map { cols =>
      cols
        .zip(sizes)
        .map { case (col, size) => col + (" " * (size - col.length)) }
        .mkString("|")
    }

    var hline = sizes.map("-" * _).mkString("|")

    new PrintWriter("doc/ref/vm/opcodes.md") {
      write(s"<!--\nTHIS FILE IS GENERATED. DO NOT EDIT MANUALLY!\n-->\n")
      write("# Pravda VM opcodes\n")
      write(header + '\n')
      write(hline + '\n')
      write(rows.mkString("\n"))
      close()
    }
  }

  genCliDocs()
  genVmDocs()
}
