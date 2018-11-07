/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.evm.abi.parse

import java.nio.charset.Charset

import pravda.evm.EVM
import pravda.evm.EVM.{Bool, SInt, UInt}
import tethys._
import tethys.jackson._
import tethys.derivation.auto._
import tethys.derivation.semiauto._
import scala.annotation.tailrec

object ABIParser {

  implicit val aboObjReader: JsonReader[ABIObject] = JsonReader.builder
    .addField[String]("type")
    .selectReader[ABIObject] {
      case "function"    => jsonReader[ABIFunction]
      case "event"       => jsonReader[ABIEvent]
      case "constructor" => jsonReader[ABIConstructor]
    }

  trait ABIObject {
    def inputs: Seq[Variable]
  }

  object ABIObject {

    def split(abi: List[ABIObject]): (List[ABIFunction], List[ABIEvent], List[ABIConstructor]) = {
      val funcs = abi.collect({ case a @ ABIFunction(_, _, _, _, _, _, _) => a })
      val events = abi.collect({ case a @ ABIEvent(_, _, _)               => a })
      val constructors = abi.collect({ case a @ ABIConstructor(_, _, _)   => a })
      (funcs, events, constructors)
    }
  }

  case class ABIFunction(constant: Boolean,
                         name: String,
                         inputs: Seq[Variable],
                         outputs: Seq[Variable],
                         payable: Boolean,
                         stateMutability: String,
                         newName: Option[String])
      extends ABIObject {

    val hashableName = s"$name(${inputs.map(_.`type`).mkString(",")})"
    val id = getHash(hashableName).toList

    val variables = inputs
      .map(variable => nameToType(variable.`type`))
      .foldLeft((4, List.empty[(Int, EVM.Type)]))({
        case ((pos, types), varType) =>
          (pos + 32, (pos, varType) :: types)
      })
      ._2
      .reverse
  }

  case class ABIEvent(name: String, inputs: Seq[Variable], anonymous: Boolean) extends ABIObject
  case class ABIConstructor(inputs: Seq[Variable], payable: Boolean, stateMutability: String) extends ABIObject
  case class Variable(name: String, `type`: String, indexed: Option[Boolean])

  val nameToType: PartialFunction[String, EVM.Type] = {
    case "bool" => Bool

    case name if name.startsWith("uint") => UInt(name.substring(4).toInt)
    case name if name.startsWith("int")  => SInt(name.substring(3).toInt)
    case _                               => Bool //FIXME add another types
  }

  def getContract(s: String): Either[String, List[ABIObject]] = s.jsonAs[List[ABIObject]] match {
    case Right(functions) =>
      @tailrec def buildUniqueNames(funcs: List[ABIFunction],
                                    acc: List[ABIFunction],
                                    names: Map[String, Int]): List[ABIFunction] =
        funcs match {
          case x :: xs =>
            import x._
            if (names.contains(name))
              buildUniqueNames(xs,
                               x.copy(newName = Some(s"$name${names(name)}")) :: acc,
                               names.updated(name, names(name) + 1))
            else buildUniqueNames(xs, x :: acc, names.updated(name, 0))
          case Nil => acc
        }

      val (funcs, events, consts) = ABIObject.split(functions)
      Right(events ++ consts ++ buildUniqueNames(funcs, List.empty, Map.empty))
    case _ => Left(s"Invalid json $s")
  }

  def getHash(s: String): Array[Byte] = {
    import org.bouncycastle.jcajce.provider.digest._
    val digest = new Keccak.Digest256
    digest.digest(s.getBytes(Charset.forName("ASCII"))).slice(0, 4)
  }

  def main(args: Array[String]): Unit = {

    val x = """[
                {
                  "constant": true,
                  "inputs": [],
                  "name": "name",
                  "outputs": [
                    {
                      "name": "",
                      "type": "string"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_spender",
                      "type": "address"
                    },
                    {
                      "name": "_value",
                      "type": "uint256"
                    }
                  ],
                  "name": "approve",
                  "outputs": [
                    {
                      "name": "success",
                      "type": "bool"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": true,
                  "inputs": [],
                  "name": "totalSupply",
                  "outputs": [
                    {
                      "name": "",
                      "type": "uint256"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_from",
                      "type": "address"
                    },
                    {
                      "name": "_to",
                      "type": "address"
                    },
                    {
                      "name": "_value",
                      "type": "uint256"
                    }
                  ],
                  "name": "transferFrom",
                  "outputs": [
                    {
                      "name": "success",
                      "type": "bool"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": true,
                  "inputs": [],
                  "name": "decimals",
                  "outputs": [
                    {
                      "name": "",
                      "type": "uint8"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_value",
                      "type": "uint256"
                    }
                  ],
                  "name": "burn",
                  "outputs": [
                    {
                      "name": "success",
                      "type": "bool"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": true,
                  "inputs": [
                    {
                      "name": "",
                      "type": "address"
                    }
                  ],
                  "name": "balanceOf",
                  "outputs": [
                    {
                      "name": "",
                      "type": "uint256"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_from",
                      "type": "address"
                    },
                    {
                      "name": "_value",
                      "type": "uint256"
                    }
                  ],
                  "name": "burnFrom",
                  "outputs": [
                    {
                      "name": "success",
                      "type": "bool"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": true,
                  "inputs": [],
                  "name": "symbol",
                  "outputs": [
                    {
                      "name": "",
                      "type": "string"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_to",
                      "type": "address"
                    },
                    {
                      "name": "_value",
                      "type": "uint256"
                    }
                  ],
                  "name": "transfer",
                  "outputs": [],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": false,
                  "inputs": [
                    {
                      "name": "_spender",
                      "type": "address"
                    },
                    {
                      "name": "_value",
                      "type": "uint256"
                    },
                    {
                      "name": "_extraData",
                      "type": "bytes"
                    }
                  ],
                  "name": "approveAndCall",
                  "outputs": [
                    {
                      "name": "success",
                      "type": "bool"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "function"
                },
                {
                  "constant": true,
                  "inputs": [
                    {
                      "name": "",
                      "type": "address"
                    },
                    {
                      "name": "",
                      "type": "address"
                    }
                  ],
                  "name": "allowance",
                  "outputs": [
                    {
                      "name": "",
                      "type": "uint256"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "view",
                  "type": "function"
                },
                {
                  "inputs": [
                    {
                      "name": "initialSupply",
                      "type": "uint256"
                    },
                    {
                      "name": "tokenName",
                      "type": "string"
                    },
                    {
                      "name": "tokenSymbol",
                      "type": "string"
                    }
                  ],
                  "payable": false,
                  "stateMutability": "nonpayable",
                  "type": "constructor"
                },
                {
                  "anonymous": false,
                  "inputs": [
                    {
                      "indexed": true,
                      "name": "from",
                      "type": "address"
                    },
                    {
                      "indexed": true,
                      "name": "to",
                      "type": "address"
                    },
                    {
                      "indexed": false,
                      "name": "value",
                      "type": "uint256"
                    }
                  ],
                  "name": "Transfer",
                  "type": "event"
                },
                {
                  "anonymous": false,
                  "inputs": [
                    {
                      "indexed": true,
                      "name": "from",
                      "type": "address"
                    },
                    {
                      "indexed": false,
                      "name": "value",
                      "type": "uint256"
                    }
                  ],
                  "name": "Burn",
                  "type": "event"
                }
              ]"""
    getContract(x) match {
      case Right(value) => value.foreach(println)
      case Left(v)      => println(v)
    }

  }
}
