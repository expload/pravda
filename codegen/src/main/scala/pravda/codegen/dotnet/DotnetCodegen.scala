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

object DotnetCodegen {

  type GeneratedFile = (String, String) // (name, content)

  final case class ParseClass(resultTpeClass: String, resultTpe: String)
  final case class MethodTemplate(methodName: String,
                                  methodTpe: String,
                                  methodArgsDef: String,
                                  methodArgs: String,
                                  jsonFormat: String,
                                  argsFormat: String,
                                  methodParseResult: String)
  final case class DotnetTemplate(programName: String,
                                  methods: java.util.List[MethodTemplate],
                                  parseClasses: java.util.List[ParseClass],
                                  client: String)

  private def tpeToDotnetFormat(tpe: Meta.TypeSignature): String => String = {
    def primitiveFormat(p: Meta.TypeSignature.PrimitiveType): String => String = p match {
      case Meta.TypeSignature.Null  => ???
      case Meta.TypeSignature.Int8  => identity
      case Meta.TypeSignature.Int16 => identity
      case Meta.TypeSignature.Int32 => identity
      case Meta.TypeSignature.BigInt => ???
      case Meta.TypeSignature.Uint8  => identity
      case Meta.TypeSignature.Uint16 => identity
      case Meta.TypeSignature.Uint32 => identity
      case Meta.TypeSignature.Number => identity
      case Meta.TypeSignature.Boolean =>
        arg =>
          s"""$arg ? "true" : "false" """
      case Meta.TypeSignature.Ref => ???
      case Meta.TypeSignature.Utf8 =>
        arg =>
          s""" "\\"" + $arg" + "\\"" """
      case Meta.TypeSignature.Bytes =>
        arg =>
          s""" "\\"" + BitConverter.ToString($arg).Replace("-","") + "\\"" """
    }

    tpe match {
      case p: Meta.TypeSignature.PrimitiveType => primitiveFormat(p)
      case Meta.TypeSignature.Array(p) =>
        arg =>
          s""" "[" + string.Join(",", $arg) + "]" """ // FIXME doesn't work for bool, utf8, bytes
      case Meta.TypeSignature.Struct(name, fields) => ???
    }
  }

  private def tpeToDotnetTpe(tpe: Meta.TypeSignature): String = {
    def primitiveToDotnetTpe(p: Meta.TypeSignature.PrimitiveType): String =
      p match {
        case Meta.TypeSignature.Null    => "object"
        case Meta.TypeSignature.Int8    => "sbyte"
        case Meta.TypeSignature.Int16   => "short"
        case Meta.TypeSignature.Int32   => "int"
        case Meta.TypeSignature.BigInt  => "BigInteger"
        case Meta.TypeSignature.Uint8   => "byte"
        case Meta.TypeSignature.Uint16  => "ushort"
        case Meta.TypeSignature.Uint32  => "uint"
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

  private def jsonArgsFormat(method: Meta.MethodSignature): (String, String) = {
    val argsFormat = method.argTpes.zipWithIndex
      .map {
        case (tpe, i) =>
          s"""{{ \\"value\\": {${i + 1}}, \\"tpe\\": \\"${tpe.mnemonic}\\" }}"""
      }
      .mkString(", ")

    val args = "ProgramAddress" +: method.argNames
    val tpes = Meta.TypeSignature.Bytes +: method.argTpes

    (
      s"""{{ \\"address\\": {0}, \\"method\\": \\"${method.name}\\", \\"args\\": [$argsFormat] }}""",
      tpes.map(tpeToDotnetFormat).zip(args).map { case (formatF, arg) => formatF(arg) }.mkString(", ")
    )
  }

  private def consturctTemplate(programName: String, methods: List[Meta.MethodSignature]): DotnetTemplate = {

    def constructArgs(method: Meta.MethodSignature): (String, String) = {
      (method.argTpes.zip(method.argNames).map { case (tpe, name) => s"${tpeToDotnetTpe(tpe)} $name" }.mkString(", "),
       method.argNames.mkString(", "))
    }

    DotnetTemplate(
      programName.capitalize,
      methods
        .map(m => {
          val (jsonFormat, argsFormat) = jsonArgsFormat(m)
          val (argsDef, args) = constructArgs(m)
          val tpe = tpeToDotnetTpe(m.returnTpe)
          val parseResult = if (m.returnTpe == Meta.TypeSignature.Null) {
            "null"
          } else {
            s"${tpe.capitalize}Result.FromJson(json).value"
          }
          MethodTemplate(m.name.capitalize, tpe, argsDef, args, jsonFormat, argsFormat, parseResult)
        })
        .asJava,
      methods
        .filter(_.returnTpe != Meta.TypeSignature.Null)
        .map(m => tpeToDotnetTpe(m.returnTpe))
        .distinct
        .map(t => ParseClass(t.capitalize, t))
        .asJava,
      "localhost:8087/api/program/method"
    )
  }

  def extractInfo(bytecode: ByteString): (String, List[Meta.MethodSignature]) = {
    val ops = asm.PravdaAssembler.disassemble(bytecode)

    val programName = ops.collectFirst {
      case asm.Operation.Meta(ProgramName(name)) => name
    }

    val methods = ops.collect {
      case asm.Operation.Meta(m: Meta.MethodSignature) => m
    }

    (programName.getOrElse("Program"), methods.toList)
  }

  def generateMethods(programName: String, methods: List[Meta.MethodSignature]): String = {
    val mf = new DefaultMustacheFactory
    val mustache = mf.compile("DotnetHttpMethods.mustache.cs")
    val sw = new StringWriter()
    mustache.execute(sw, consturctTemplate(programName, methods))
    sw.toString
  }

  def generate(byteCode: ByteString): GeneratedFile = { // (BigInteger, Methods)
    val (name, methods) = extractInfo(byteCode)
    (name.capitalize + ".cs", generateMethods(name, methods))
  }
}
