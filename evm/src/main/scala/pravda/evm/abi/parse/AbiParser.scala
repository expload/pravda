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
import pravda.evm.EVM.{Bool, SInt, UInt, Unsupported}
import tethys._
import tethys.jackson._
import tethys.derivation.auto._
import tethys.derivation.semiauto._
import scala.annotation.tailrec

object AbiParser {

  implicit val aboObjReader: JsonReader[AbiObject] = JsonReader.builder
    .addField[String]("type")
    .selectReader[AbiObject] {
      case "function"    => jsonReader[AbiFunction]
      case "event"       => jsonReader[AbiEvent]
      case "constructor" => jsonReader[AbiConstructor]
    }

  trait AbiObject {
    def inputs: Seq[Argument]

    val arguments: List[(Int, EVM.AbiType)] = inputs
      .map(variable => nameToType(variable.`type`))
      .foldLeft((4, List.empty[(Int, EVM.AbiType)])) {
        case ((pos, types), varType) =>
          (pos + 32, (pos, varType) :: types)
      }
      ._2
      .reverse
  }

  object AbiObject {

    def unwrap(abi: List[AbiObject]): (List[AbiFunction], List[AbiEvent], List[AbiConstructor]) = {
      val funcs = abi.collect { case a: AbiFunction           => a }
      val events = abi.collect { case a: AbiEvent             => a }
      val constructors = abi.collect { case a: AbiConstructor => a }
      (funcs, events, constructors)
    }
  }

  case class AbiFunction(constant: Boolean,
                         name: String,
                         inputs: Seq[Argument],
                         outputs: Seq[Argument],
                         payable: Boolean,
                         stateMutability: String,
                         newName: Option[String])
      extends AbiObject {

    lazy val hashableName = s"$name(${inputs.map(_.`type`).mkString(",")})"
    lazy val id = hashKeccak256(hashableName).toList
  }

  case class AbiEvent(name: String, inputs: Seq[Argument], anonymous: Boolean)                extends AbiObject
  case class AbiConstructor(inputs: Seq[Argument], payable: Boolean, stateMutability: String) extends AbiObject
  case class Argument(name: String, `type`: String, indexed: Option[Boolean])

  def nameToType(tpe: String): EVM.AbiType = tpe match {
    case "bool"                    => Bool
    case t if t.startsWith("uint") => UInt(t.substring(4).toInt)
    case t if t.startsWith("int")  => SInt(t.substring(3).toInt)
    case _                         => Unsupported
  }

  def parseAbi(s: String): Either[String, List[AbiObject]] = s.jsonAs[List[AbiObject]] match {
    case Right(functions) =>
      @tailrec
      def buildUniqueNames(funcs: List[AbiFunction],
                           acc: List[AbiFunction],
                           names: Map[String, Int]): List[AbiFunction] =
        funcs match {
          case x :: xs =>
            if (names.contains(x.name))
              buildUniqueNames(xs,
                               x.copy(newName = Some(s"${x.name}${names(x.name)}")) :: acc,
                               names.updated(x.name, names(x.name) + 1))
            else buildUniqueNames(xs, x :: acc, names.updated(x.name, 0))
          case Nil => acc
        }

      val (funcs, events, consts) = AbiObject.unwrap(functions)
      Right(events ++ consts ++ buildUniqueNames(funcs, List.empty, Map.empty))
    case _ => Left(s"Invalid json $s")
  }

  def hashKeccak256(s: String): Array[Byte] = {
    import org.bouncycastle.jcajce.provider.digest._
    val digest = new Keccak.Digest256
    digest.digest(s.getBytes(Charset.forName("ASCII"))).slice(0, 4)
  }
}
