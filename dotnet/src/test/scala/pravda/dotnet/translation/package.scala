package pravda.dotnet

import pravda.common.TestUtils
import pravda.vm.asm
import pravda.vm.asm.PravdaAssembler

package object translation {

  def assertWithAsmDiff(asm1: Seq[asm.Operation], asm2: Seq[asm.Operation]): Unit =
    TestUtils.assertEqual(PravdaAssembler.render(asm1), PravdaAssembler.render(asm2))
}
