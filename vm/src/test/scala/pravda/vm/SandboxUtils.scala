package pravda.vm

import pravda.vm.VmSandbox.MacroHandler
import utest.framework._
import utest._

import scala.io.Source

object SandboxUtils {

  def constructTestsFromDir(dir: java.io.File, macroHandler: MacroHandler = PartialFunction.empty): utest.Tests = {

    def walkDir(prefix: String, dir: java.io.File): Option[(Tree[String], TestCallTree)] =
      if (dir.isDirectory) {
        val files = dir.listFiles.filter(f => f.isFile && f.getName.endsWith(".sbox"))
        val fileNames = files.map(f => Tree(f.getName.stripSuffix(".sbox")))
        val fileCalls = files.map(f =>
          new TestCallTree(Left {
            val caseE = VmSandbox.parseCase(Source.fromFile(f).mkString)
            caseE match {
              case Left(err) => Predef.assert(false, err)
              case Right(c)  => VmSandbox.assertCase(c, macroHandler)
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
}
