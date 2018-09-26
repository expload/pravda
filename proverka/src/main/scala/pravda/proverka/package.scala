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

package pravda
import fastparse.StringReprOps
import pravda.proverka.Proverka.{Error, Text}
import pravda.proverka.Proverka._
import fastparse.all._

package object proverka {

  private def parseEither[T](text: String, p: P[T]): Either[String, T] = {
    p.parse(text) match {
      case Parsed.Success(c, _) =>
        Right(c)
      case Parsed.Failure(_, index, extra) =>
        val in = extra.input
        def aux(start: Int, i: Int, lim: Int): String = {
          if (lim > 0 && i < text.length
              && text.charAt(i) != '\n'
              && text.charAt(i) != '\r'
              && text.charAt(i) != ' ') aux(start, i + 1, lim - 1)
          else text.substring(start, i - 1)
        }
        val pos = StringReprOps.prettyIndex(in, index)
        val found = aux(index, index, 20)
        Left(s"$pos: ${extra.traced.expected} expected but '$found' found.")
    }
  }

  def input[State](name: String)(parse: Text => Either[Error, State => State]): InputPart[State] =
    InputPart(name, parse)

  def parserInput[State](name: String)(parser: P[State => State]): InputPart[State] =
    input(name) { text =>
      parseEither(text, parser)
    }

  def textOutput[State](name: String)(produce: State => Either[Error, Text]): OutputPart[State] =
    OutputPart(name, produce)

  def output[State, T](name: String)(produce: State => Either[Error, T]): OutputPart[State] =
    textOutput(name) { s =>
      for {
        p <- produce(s)
      } yield pprint.apply(p, height = Int.MaxValue).plainText
    }
}
