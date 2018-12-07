package pravda.evm
package abi.parser

import pravda.evm.abi.parse.AbiParser
import pravda.evm.abi.parse.AbiParser.{AbiConstructor, AbiEvent, AbiFunction, Argument}
import utest._

object ABIParserTests extends TestSuite {

  val tests = Tests {

    "ABI parse" - {
      val abi = readSolidityABI("SimpleStorageABIj.json")
      val parsedAbi = AbiParser.parseAbi(abi)

      parsedAbi ==> Right(
        List(
          AbiFunction(true, "get", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          AbiFunction(false, "set", Vector(Argument("x", "uint256", None)), Vector(), false, "nonpayable", None)
        )
      )
    }

    "Complex abi parse" - {
      val abi = readSolidityABI("complex/ComplexContractABI.json")
      val parsedAbi = AbiParser.parseAbi(abi)

      parsedAbi ==> Right(
        List(
          AbiEvent("Transfer",
                   Vector(Argument("from", "address", Some(true)),
                          Argument("to", "address", Some(true)),
                          Argument("value", "uint256", Some(false))),
                   false),
          AbiEvent("Burn",
                   Vector(Argument("from", "address", Some(true)), Argument("value", "uint256", Some(false))),
                   false),
          AbiConstructor(Vector(Argument("initialSupply", "uint256", None),
                                Argument("tokenName", "string", None),
                                Argument("tokenSymbol", "string", None)),
                         false,
                         "nonpayable"),
          AbiFunction(true,
                      "allowance",
                      Vector(Argument("", "address", None), Argument("", "address", None)),
                      Vector(Argument("", "uint256", None)),
                      false,
                      "view",
                      None),
          AbiFunction(
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
          AbiFunction(false,
                      "transfer",
                      Vector(Argument("_to", "address", None), Argument("_value", "uint256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      None),
          AbiFunction(true, "symbol", Vector(), Vector(Argument("", "string", None)), false, "view", None),
          AbiFunction(
            false,
            "burnFrom",
            Vector(Argument("_from", "address", None), Argument("_value", "uint256", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          AbiFunction(true,
                      "balanceOf",
                      Vector(Argument("", "address", None)),
                      Vector(Argument("", "uint256", None)),
                      false,
                      "view",
                      None),
          AbiFunction(false,
                      "burn",
                      Vector(Argument("_value", "uint256", None)),
                      Vector(Argument("success", "bool", None)),
                      false,
                      "nonpayable",
                      None),
          AbiFunction(true, "decimals", Vector(), Vector(Argument("", "uint8", None)), false, "view", None),
          AbiFunction(
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
          AbiFunction(true, "totalSupply", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          AbiFunction(
            false,
            "approve",
            Vector(Argument("_spender", "address", None), Argument("_value", "uint256", None)),
            Vector(Argument("success", "bool", None)),
            false,
            "nonpayable",
            None
          ),
          AbiFunction(true, "name", Vector(), Vector(Argument("", "string", None)), false, "view", None)
        )
      )
    }

    "Overloading abi parse" - {
      val abi = readSolidityABI("ABIExampleWithOverloading.json")
      val parsedAbi = AbiParser.parseAbi(abi)

      parsedAbi ==> Right(
        List(
          AbiFunction(false,
                      "set",
                      Vector(Argument("x", "int256", None)),
                      Vector(),
                      false,
                      "nonpayable",
                      Option("set0")),
          AbiFunction(true, "get", Vector(), Vector(Argument("", "uint256", None)), false, "view", None),
          AbiFunction(false, "set", Vector(Argument("x", "uint256", None)), Vector(), false, "nonpayable", None)
        ))

    }

  }
}
