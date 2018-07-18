package pravda.vm

import java.io.File

import utest._

object VmSuite extends TestSuite {
  val tests = SandboxUtils.constructTestsFromFolder(new File(getClass.getResource("/").getPath))
}
