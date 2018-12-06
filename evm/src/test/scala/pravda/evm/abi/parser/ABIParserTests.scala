package pravda.evm.abi.parser

import pravda.evm.abi.parse.ABIParser
import pravda.evm.abi.parse.ABIParser.{ABIConstructor, ABIEvent, ABIFunction, Argument}
import pravda.evm.evm._
import utest._

object ABIParserTests extends TestSuite {

  val tests = Tests {

    "ABI parse" - {
      val abi = readSolidityABI("SimpleStorageABIj.json")
      val parsedAbi = ABIParser.getContract(abi)

      parsedAbi ==> Right(
        List(
          ABIFunction(true, "get", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          ABIFunction(false, "set", Vector(Argument("x", "uint256", None)), Vector(), false, "nonpayable", None)
        )
      )
    }

    "Complex abi parse" - {

      val abi = readSolidityABI("complex/ComplexContractABI.json")
      val parsedAbi = ABIParser.getContract(abi)

      parsedAbi ==> Right(
        List(
          ABIEvent("Transfer",
                   Vector(Argument("from", "address", Some(true)),
                          Argument("to", "address", Some(true)),
                          Argument("value", "uint256", Some(false))),
                   false),
          ABIEvent("Burn",
                   Vector(Argument("from", "address", Some(true)), Argument("value", "uint256", Some(false))),
                   false),
          ABIConstructor(Vector(Argument("initialSupply", "uint256", None),
                                Argument("tokenName", "string", None),
                                Argument("tokenSymbol", "string", None)),
                         false,
                         "nonpayable"),
          ABIFunction(true,
                      "allowance",
                      Vector(Argument("", "address", None), Argument("", "address", None)),
                      Vector(Argument("", "uint256", None)),
                      false,
                      "view",
                      None),
          ABIFunction(
            false,
            "approveAndCall",
            Vector(Argument("_spender", "address", None),
                   Argument("_value", "uint256", None),
                   Argument("_extraData", "bytes", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(false,
                      "transfer",
                      Vector(Argument("_to", "address", None), Argument("_value", "uint256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      None),
          ABIFunction(true, "symbol", Vector(), Vector(Argument("", "string", None)), false, "view", None),
          ABIFunction(
            false,
            "burnFrom",
            Vector(Argument("_from", "address", None), Argument("_value", "uint256", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true,
                      "balanceOf",
                      Vector(Argument("", "address", None)),
                      Vector(Argument("", "uint256", None)),
                      false,
                      "view",
                      None),
          ABIFunction(false,
                      "burn",
                      Vector(Argument("_value", "uint256", None)),
                      Vector(Argument("success", "bool", None)),
                      false,
                      "nonpayable",
                      None),
          ABIFunction(true, "decimals", Vector(), Vector(Argument("", "uint8", None)), false, "view", None),
          ABIFunction(
            false,
            "transferFrom",
            Vector(Argument("_from", "address", None),
                   Argument("_to", "address", None),
                   Argument("_value", "uint256", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true, "totalSupply", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          ABIFunction(
            false,
            "approve",
            Vector(Argument("_spender", "address", None), Argument("_value", "uint256", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          ABIFunction(true, "name", Vector(), Vector(Argument("", "string", None)), false, "view", None)
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
                      Vector(Argument("x", "int256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      Option("set0")),
          ABIFunction(true, "get", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          ABIFunction(false, "set", Vector(Argument("x", "uint256", None)), Vector(), false, "nonpayable", None)
        ))

    }

  }
}
