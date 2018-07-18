package pravda.vm

import pravda.vm.VmSandbox.MacroHandler
import utest.framework._
import utest._

import scala.io.Source

object SandboxUtils {

  def constructTestsFromFolder(folder: java.io.File,
                               macroHandler: MacroHandler = PartialFunction.empty): utest.Tests = {
    if (folder.isDirectory) {
      val files = folder.listFiles.filter(f => f.isFile && f.getName.endsWith(".sbox"))
      val names = files.map(f => Tree(f.getName.stripSuffix(".sbox")))
      val calls = files.map(f =>
        new TestCallTree(Left {
          val caseE = VmSandbox.parseCase(Source.fromFile(f).mkString)
          caseE match {
            case Left(err) => Predef.assert(false, err)
            case Right(c)  => VmSandbox.assertCase(c, macroHandler)
          }
        }))

      new Tests(Tree("", names: _*), new TestCallTree(Right(calls)))
    } else {
      new Tests(Tree(""), new TestCallTree(Left(())))
    }
  }
}
