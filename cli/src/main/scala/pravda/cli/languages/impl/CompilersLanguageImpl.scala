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

package pravda.cli.languages

package impl

import com.google.protobuf.ByteString
import pravda.dotnet.parser.{FileParser => DotnetParser}
import pravda.dotnet.translation.{Translator => DotnetTranslator}
import pravda.vm.asm.PravdaAssembler
import cats.instances.either._
import cats.instances.option._
import cats.syntax.traverse._
import pravda.dotnet.parser.FileParser.ParsedDotnetFile

import scala.concurrent.{ExecutionContext, Future}

final class CompilersLanguageImpl(implicit executionContext: ExecutionContext) extends CompilersLanguage[Future] {

  def asm(fileName: String, source: String): Future[Either[String, ByteString]] = Future {
    PravdaAssembler.assemble(source, saveLabels = true).left.map(s => s"$fileName:${s.mkString}")
  }

  def asm(source: String): Future[Either[String, ByteString]] = Future {
    PravdaAssembler.assemble(source, saveLabels = true).left.map(_.mkString)
  }

  def disasm(source: ByteString): Future[String] = Future {
    PravdaAssembler.render(PravdaAssembler.disassemble(source))
  }

  def dotnet(sources: Seq[(ByteString, Option[ByteString])],
             mainClass: Option[String]): Future[Either[String, ByteString]] = Future {
    for {
      files <- sources.map {
        case (pe, pdb) =>
          for {
            parsedPe <- DotnetParser.parsePe(pe.toByteArray)
            parsedPdb <- pdb.map(p => DotnetParser.parsePdb(p.toByteArray)).sequence
          } yield ParsedDotnetFile(parsedPe, parsedPdb)
      }.sequence
      ops <- DotnetTranslator.translateAsm(files, mainClass).left.map(_.mkString)
    } yield PravdaAssembler.assemble(ops, saveLabels = true)
  }
}
