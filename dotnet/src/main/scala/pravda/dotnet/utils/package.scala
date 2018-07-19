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

package pravda.dotnet

import fastparse.byte.all._
import fastparse.core.Parsed._

package object utils {
  private[dotnet] def nullTerminatedString(len: Int): P[String] =
    AnyBytes(len).!.map(bs => new String(bs.takeWhile(_ != 0).toArray))

  private[dotnet] val nullTerminatedString: P[String] =
    P(BytesWhile(_ != 0, min = 0).! ~ BS(0)).map(bs => new String(bs.toArray))

  private[dotnet] implicit class ParserOps[T](p: Parsed[T]) {

    def toEither: Either[String, T] = p match {
      case Success(t, _)        => Right(t)
      case f @ Failure(_, _, _) => Left(s"An error occurred during parsing: ${f.extra.traced.traceParsers}")
    }
  }
}
