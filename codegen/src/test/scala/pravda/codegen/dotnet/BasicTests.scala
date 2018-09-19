package pravda.codegen.dotnet

import pravda.common.TestUtils
import pravda.vm.Meta
import utest._

import scala.io.Source

object BasicTests extends TestSuite {

  val tests = Tests {
    'one_method - {
      TestUtils.assertEqual(
        DotnetCodegen.generateMethods(
          "ERC20",
          List(
            Meta.MethodSignature("balanceOf", Meta.TypeSignature.Uint32, List(Meta.TypeSignature.Bytes))
          )
        ),
        Source.fromResource("OneMethod.generated.cs").mkString
      )
    }

    'ERC20 - {
      TestUtils.assertEqual(
        DotnetCodegen.generateMethods(
          "ERC20",
          List(
            Meta.MethodSignature("balanceOf", Meta.TypeSignature.Uint32, List(Meta.TypeSignature.Bytes)),
            Meta.MethodSignature("allowance",
                                 Meta.TypeSignature.Uint32,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Bytes)),
            Meta.MethodSignature("transfer",
                                 Meta.TypeSignature.Null,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Uint32)),
            Meta.MethodSignature("approve",
                                 Meta.TypeSignature.Null,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Uint32)),
            Meta.MethodSignature(
              "transferFrom",
              Meta.TypeSignature.Null,
              List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Bytes, Meta.TypeSignature.Uint32)
            )
          )
        ),
        Source.fromResource("ERC20.generated.cs").mkString
      )
    }
  }
}
