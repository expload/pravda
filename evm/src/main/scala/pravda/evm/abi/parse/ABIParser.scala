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
    def inputs: Seq[Argument]

    val arguments: List[(Int, EVM.AbiType)] = inputs
      .map(variable => nameToType(variable.`type`))
      .foldLeft((4, List.empty[(Int, EVM.AbiType)]))({
        case ((pos, types), varType) =>
          (pos + 32, (pos, varType) :: types)
      })
      ._2
      .reverse
  }

  object ABIObject {

    def unzip(abi: List[ABIObject]): (List[ABIFunction], List[ABIEvent], List[ABIConstructor]) = {
      val funcs = abi.collect({ case a @ ABIFunction(_, _, _, _, _, _, _) => a })
      val events = abi.collect({ case a @ ABIEvent(_, _, _)               => a })
      val constructors = abi.collect({ case a @ ABIConstructor(_, _, _)   => a })
      (funcs, events, constructors)
    }
  }

  case class ABIFunction(constant: Boolean,
                         name: String,
                         inputs: Seq[Argument],
                         outputs: Seq[Argument],
                         payable: Boolean,
                         stateMutability: String,
                         newName: Option[String])
      extends ABIObject {

    val hashableName = s"$name(${inputs.map(_.`type`).mkString(",")})"
    val id = hashKeccak256(hashableName).toList
  }

  case class ABIEvent(name: String, inputs: Seq[Argument], anonymous: Boolean)                extends ABIObject
  case class ABIConstructor(inputs: Seq[Argument], payable: Boolean, stateMutability: String) extends ABIObject
  case class Argument(name: String, `type`: String, indexed: Option[Boolean])

  val nameToType: PartialFunction[String, EVM.AbiType] = {
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
            if (names.contains(x.name))
              buildUniqueNames(xs,
                               x.copy(newName = Some(s"${x.name}${names(x.name)}")) :: acc,
                               names.updated(x.name, names(x.name) + 1))
            else buildUniqueNames(xs, x :: acc, names.updated(x.name, 0))
          case Nil => acc
        }

      val (funcs, events, consts) = ABIObject.unzip(functions)
      Right(events ++ consts ++ buildUniqueNames(funcs, List.empty, Map.empty))
    case _ => Left(s"Invalid json $s")
  }

  def hashKeccak256(s: String): Array[Byte] = {
    import org.bouncycastle.jcajce.provider.digest._
    val digest = new Keccak.Digest256
    digest.digest(s.getBytes(Charset.forName("ASCII"))).slice(0, 4)
  }
}
