package pravda.evm.abi.parser

import pravda.evm.abi.parse.ABIParser
import pravda.evm.abi.parse.ABIParser.{ABIConstructor, ABIEvent, ABIFunction, Variable}
import pravda.evm.evm._
import utest._

object ABIParserTests extends TestSuite {

  val tests = Tests {

    "ABI parse" - {
      val abi = readSolidityABI("SimpleStorageABIj.json")
      val parsedAbi = ABIParser.getContract(abi)

      parsedAbi ==> Right(
        List(
          ABIFunction(true, "get", Vector(), Vector(Variable("", "uint256", None)), false, "view", None),
          ABIFunction(false, "set", Vector(Variable("x", "uint256", None)), Vector(), false, "nonpayable", None)
        )
      )
    }

    "Complex abi parse" - {

      val abi = readSolidityABI("complex/ComplexContractABI.json")
      val parsedAbi = ABIParser.getContract(abi)

      parsedAbi ==> Right(
        List(
          ABIEvent("Transfer",
                   Vector(Variable("from", "address", Some(true)),
                          Variable("to", "address", Some(true)),
                          Variable("value", "uint256", Some(false))),
                   false),
          ABIEvent("Burn",
                   Vector(Variable("from", "address", Some(true)), Variable("value", "uint256", Some(false))),
                   false),
          ABIConstructor(Vector(Variable("initialSupply", "uint256", None),
                                Variable("tokenName", "string", None),
                                Variable("tokenSymbol", "string", None)),
                         false,
                         "nonpayable"),
          ABIFunction(true,
                      "allowance",
                      Vector(Variable("", "address", None), Variable("", "address", None)),
                      Vector(Variable("", "uint256", None)),
                      false,
                      "view",
                      None),
          ABIFunction(
            false,
            "approveAndCall",
            Vector(Variable("_spender", "address", None),
                   Variable("_value", "uint256", None),
                   Variable("_extraData", "bytes", None)),
            Vector(Variable("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(false,
                      "transfer",
                      Vector(Variable("_to", "address", None), Variable("_value", "uint256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      None),
          ABIFunction(true, "symbol", Vector(), Vector(Variable("", "string", None)), false, "view", None),
          ABIFunction(
            false,
            "burnFrom",
            Vector(Variable("_from", "address", None), Variable("_value", "uint256", None)),
            Vector(Variable("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true,
                      "balanceOf",
                      Vector(Variable("", "address", None)),
                      Vector(Variable("", "uint256", None)),
                      false,
                      "view",
                      None),
          ABIFunction(false,
                      "burn",
                      Vector(Variable("_value", "uint256", None)),
                      Vector(Variable("success", "bool", None)),
                      false,
                      "nonpayable",
                      None),
          ABIFunction(true, "decimals", Vector(), Vector(Variable("", "uint8", None)), false, "view", None),
          ABIFunction(
            false,
            "transferFrom",
            Vector(Variable("_from", "address", None),
                   Variable("_to", "address", None),
                   Variable("_value", "uint256", None)),
            Vector(Variable("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true, "totalSupply", Vector(), Vector(Variable("", "uint256", None)), false, "view", None),
          ABIFunction(
            false,
            "approve",
            Vector(Variable("_spender", "address", None), Variable("_value", "uint256", None)),
            Vector(Variable("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true, "name", Vector(), Vector(Variable("", "string", None)), false, "view", None)
        )
      )
    }

    "Overloading abi parse" - {
      val abi = readSolidityABI("ABIExampleWithOverloading.json")
      val parsedAbi = ABIParser.getContract(abi)

      parsedAbi ==> Right(
        List(
          ABIFunction(false,
                      "set",
                      Vector(Variable("x", "int256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      Option("set0")),
          ABIFunction(true, "get", Vector(), Vector(Variable("", "uint256", None)), false, "view", None),
          ABIFunction(false, "set", Vector(Variable("x", "uint256", None)), Vector(), false, "nonpayable", None)
        ))

    }

  }
}
