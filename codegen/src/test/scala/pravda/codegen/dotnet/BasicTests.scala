package pravda.codegen.dotnet

import pravda.common.TestUtils
import pravda.common.vm.Meta
import utest._

import scala.io.Source

object BasicTests extends TestSuite {

  val tests = Tests {
    'OneMethod - {
      TestUtils.assertEqual(
        DotnetCodegen.generateMethods(
          "OneMethod",
          List(
            Meta.MethodSignature("BalanceOf", Meta.TypeSignature.Int64, List(Meta.TypeSignature.Bytes))
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
            Meta.MethodSignature("BalanceOf", Meta.TypeSignature.Int64, List(Meta.TypeSignature.Bytes)),
            Meta.MethodSignature("Allowance",
                                 Meta.TypeSignature.Int64,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Bytes)),
            Meta.MethodSignature("Transfer",
                                 Meta.TypeSignature.Null,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Int64)),
            Meta.MethodSignature("Approve",
                                 Meta.TypeSignature.Null,
                                 List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Int64)),
            Meta.MethodSignature(
              "TransferFrom",
              Meta.TypeSignature.Null,
              List(Meta.TypeSignature.Bytes, Meta.TypeSignature.Bytes, Meta.TypeSignature.Int64)
            )
          )
        ),
        Source.fromResource("ERC20.generated.cs").mkString
      )
    }
  }
}
