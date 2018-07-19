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

package pravda.yopt

import java.io.File

trait CmdDecoder[T] {
  def decode(line: Line): Either[String, (T, Line)]
  def optInfo: Option[String]
}

object CmdDecoder {
  private val noValueError = Left("Option must have value. No value provided")
  private def parseError(ex: NumberFormatException) =
    Left(s"Error parsing option value: ${ex.getClass}:${ex.getMessage}")

  implicit val intDecoder: CmdDecoder[Int] = new CmdDecoder[Int] {
    override def decode(line: Line): Either[String, (Int, Line)] =
      line.headOption
        .map { item =>
          try {
            Right((Integer.parseInt(item), line.tail))
          } catch {
            case ex: NumberFormatException =>
              try {
                Right((Integer.parseInt(item.drop(2), 16), line.tail))
              } catch {
                case ex: NumberFormatException => parseError(ex)
              }
          }
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<int>")
  }

  implicit val longReader: CmdDecoder[Long] = new CmdDecoder[Long] {
    override def decode(line: Line): Either[String, (Long, Line)] =
      line.headOption
        .map { item =>
          try {
            Right((java.lang.Long.parseLong(item), line.tail))
          } catch {
            case ex: NumberFormatException =>
              try {
                Right((java.lang.Long.parseLong(item.drop(2), 16), line.tail))
              } catch {
                case ex: NumberFormatException => parseError(ex)
              }
          }
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<long>")
  }

  implicit val stringReader: CmdDecoder[String] = new CmdDecoder[String] {
    override def decode(line: Line): Either[String, (String, Line)] =
      line.headOption
        .map(item => Right((item, line.tail)))
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<string>")
  }

  implicit val fileReader: CmdDecoder[File] = new CmdDecoder[File] {
    override def decode(line: Line): Either[String, (File, Line)] =
      line.headOption
        .map(item => Right((new java.io.File(item), line.tail)))
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<file>")
  }

  implicit val unitReader: CmdDecoder[Unit] = new CmdDecoder[Unit] {
    override def decode(line: Line): Either[String, (Unit, Line)] =
      Right(((), line))

    override val optInfo: Option[String] = None
  }
}
