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

package pravda.codegen.dotnet

import java.io.StringWriter

import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.ByteString
import pravda.vm.Meta
import pravda.vm.Meta.ProgramName
import pravda.vm.asm

import scala.collection.JavaConverters._
import scala.io.Source

object DotnetCodegen {

  type GeneratedFile = (String, String) // (name, content)

  final case class ParseClass(resultTpeClass: String, resultTpe: String)
  final case class MethodTemplate(methodName: String,
                                  methodTpe: String,
                                  methodParseResult: String,
                                  methodArgs: String,
                                  methodPrintArgs: String)
  final case class DotnetTemplate(programName: String, methods: java.util.List[MethodTemplate])

  private def dotnetPrintArg(tpe: Meta.TypeSignature): String = {

    def primitivePrint(p: Meta.TypeSignature.PrimitiveType): String = p match {
      case Meta.TypeSignature.Null    => "PrintNull"
      case Meta.TypeSignature.Int8    => "PrintInt8"
      case Meta.TypeSignature.Int16   => "PrintInt16"
      case Meta.TypeSignature.Int32   => "PrintInt32"
      case Meta.TypeSignature.Int64   => "PrintInt64"
      case Meta.TypeSignature.BigInt  => ??? //"PrintBigInt"
      case Meta.TypeSignature.Number  => ??? //"PrintNumber"
      case Meta.TypeSignature.Boolean => "PrintBool"
      case Meta.TypeSignature.Ref     => ???
      case Meta.TypeSignature.Utf8    => "PrintUtf8"
      case Meta.TypeSignature.Bytes   => "PrintBytes"
    }

    tpe match {
      case p: Meta.TypeSignature.PrimitiveType      => primitivePrint(p)
      case Meta.TypeSignature.Array(p)              => s"${primitivePrint(p)}Array"
      case Meta.TypeSignature.Struct(sname, fields) => ???
    }
  }

  private def dotnetPringArgs(m: Meta.MethodSignature): String =
    m.args
      .zip(argsNames(m))
      .map {
        case (tpe, name) => s"ExploadTypeConverters.${dotnetPrintArg(tpe)}($name)"
      }
      .mkString(", ")

  private def dotnetTpe(tpe: Meta.TypeSignature): String = {

    def primitiveToDotnetTpe(p: Meta.TypeSignature.PrimitiveType): String =
      p match {
        case Meta.TypeSignature.Null    => "object"
        case Meta.TypeSignature.Int8    => "sbyte"
        case Meta.TypeSignature.Int16   => "short"
        case Meta.TypeSignature.Int32   => "int"
        case Meta.TypeSignature.Int64   => "long"
        case Meta.TypeSignature.BigInt  => "BigInteger"
        case Meta.TypeSignature.Number  => ???
        case Meta.TypeSignature.Boolean => "bool"
        case Meta.TypeSignature.Ref     => ???
        case Meta.TypeSignature.Utf8    => "string"
        case Meta.TypeSignature.Bytes   => "byte[]"
      }

    tpe match {
      case p: Meta.TypeSignature.PrimitiveType      => primitiveToDotnetTpe(p)
      case Meta.TypeSignature.Array(p)              => s"${primitiveToDotnetTpe(p)}[]"
      case Meta.TypeSignature.Struct(sname, fields) => ???
    }
  }

  private def dotnetParseResult(tpe: Meta.TypeSignature): String = {

    def primitiveToDotnetTpe(p: Meta.TypeSignature.PrimitiveType): String =
      p match {
        case Meta.TypeSignature.Null    => "ParseNull"
        case Meta.TypeSignature.Int8    => "ParseInt8"
        case Meta.TypeSignature.Int16   => "ParseInt16"
        case Meta.TypeSignature.Int32   => "ParseInt32"
        case Meta.TypeSignature.Int64   => "ParseInt64"
        case Meta.TypeSignature.BigInt  => "ParseBigInt"
        case Meta.TypeSignature.Number  => "ParseNumber"
        case Meta.TypeSignature.Boolean => "ParseBool"
        case Meta.TypeSignature.Ref     => ???
        case Meta.TypeSignature.Utf8    => "ParseUtf8"
        case Meta.TypeSignature.Bytes   => "ParseBytes"
      }

    tpe match {
      case p: Meta.TypeSignature.PrimitiveType      => primitiveToDotnetTpe(p)
      case Meta.TypeSignature.Array(p)              => s"${primitiveToDotnetTpe(p)}Array"
      case Meta.TypeSignature.Struct(sname, fields) => ???
    }
  }

  private def argsNames(method: Meta.MethodSignature): List[String] =
    method.args.indices.map(i => s"arg$i").toList

  private def dotnetArgs(method: Meta.MethodSignature) =
    method.args.zip(argsNames(method)).map { case (tpe, name) => s"${dotnetTpe(tpe)} $name" }.mkString(", ")

  private def consturctTemplate(programName: String, methods: List[Meta.MethodSignature]) =
    DotnetTemplate(
      programName,
      methods
        .map(
          m =>
            MethodTemplate(m.name,
                           dotnetTpe(m.returnTpe),
                           dotnetParseResult(m.returnTpe),
                           dotnetArgs(m),
                           dotnetPringArgs(m)))
        .asJava
    )

  def extractInfo(bytecode: ByteString): (String, List[Meta.MethodSignature]) = {
    val ops = asm.PravdaAssembler.disassemble(bytecode)

    val programName = ops.collectFirst {
      case (_, asm.Operation.Meta(ProgramName(name))) => name
    }

    val methods = ops.collect {
      case (_, asm.Operation.Meta(m: Meta.MethodSignature)) => m
    }

    (programName.getOrElse("Program"), methods.toList)
  }

  def generateMethods(programName: String, methods: List[Meta.MethodSignature]): String = {
    val mf = new DefaultMustacheFactory
    val mustache = mf.compile("ExploadPravdaProgram.cs.mustache")
    val sw = new StringWriter()
    mustache.execute(sw, consturctTemplate(programName, methods))
    sw.toString
  }

  def generate(byteCode: ByteString): Seq[GeneratedFile] = {
    val (name, methods) = extractInfo(byteCode)
    Seq(
      (name.capitalize + ".cs", generateMethods(name, methods.filter(_.name != "ctor"))),
      ("ExploadUnityCodegen.cs", Source.fromResource("ExploadUnityCodegen.cs").mkString)
    )
  }
}
