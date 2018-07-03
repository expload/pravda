package pravda.codegen.dotnet

import pravda.common.DiffUtils
import pravda.vm.Meta
import utest._

import scala.io.Source

object BasicTests extends TestSuite {

  val tests = Tests {
    'one_method - {
      DiffUtils.assertEqual(
        DotnetCodegen.generateMethods(
          "ERC20",
          List(
            Meta.MethodSignature("balanceOf",
                                 Meta.TypeSignature.Uint32,
                                 List((Some("tokenOwner"), Meta.TypeSignature.BigInt)))
          )
        ),
        Source.fromResource("OneMethod.generated.cs").mkString
      )
    }

    'ERC20 - {
      DiffUtils.assertEqual(
        DotnetCodegen.generateMethods(
          "ERC20",
          List(
            Meta.MethodSignature("balanceOf",
                                 Meta.TypeSignature.Uint32,
                                 List((Some("tokenOwner"), Meta.TypeSignature.BigInt))),
            Meta.MethodSignature("allowance",
                                 Meta.TypeSignature.Uint32,
                                 List((Some("tokenOwner"), Meta.TypeSignature.BigInt),
                                      (Some("spender"), Meta.TypeSignature.BigInt))),
            Meta.MethodSignature("transfer",
                                 Meta.TypeSignature.Null,
                                 List((Some("to"), Meta.TypeSignature.BigInt),
                                      (Some("tokens"), Meta.TypeSignature.Uint32))),
            Meta.MethodSignature("approve",
                                 Meta.TypeSignature.Null,
                                 List((Some("spender"), Meta.TypeSignature.BigInt),
                                      (Some("tokens"), Meta.TypeSignature.Uint32))),
            Meta.MethodSignature(
              "transferFrom",
              Meta.TypeSignature.Null,
              List((Some("from"), Meta.TypeSignature.BigInt),
                   (Some("to"), Meta.TypeSignature.BigInt),
                   (Some("tokens"), Meta.TypeSignature.Uint32))
            )
          )
        ),
        Source.fromResource("ERC20.generated.cs").mkString
      )
    }
  }
}
