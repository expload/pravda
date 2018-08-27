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
import java.net.URI

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

import scala.concurrent.duration.Duration
import scala.util.Try

trait CmdDecoder[T] {
  def decode(line: Line): Either[String, (T, Line)]
  def optInfo: Option[String]
}

object CmdDecoder {
  private val noValueError = Left("Option must have value. No value provided")

  implicit val intDecoder: CmdDecoder[Int] = new CmdDecoder[Int] {
    override def decode(line: Line): Either[String, (Int, Line)] =
      line.headOption
        .map { item =>
          Try(item.toInt).fold(
            _ => Left(IntegerDecodeError(item).toString),
            number => Right((number, line.tail))
          )
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<int>")
  }

  implicit val hexDecoder: CmdDecoder[Hex] = new CmdDecoder[Hex] {
    override def decode(line: Line): Either[String, (Hex, Line)] = {
      line.headOption
        .map { item =>
          Try(java.lang.Long.parseLong(item.toLowerCase.replace("0x", ""), 16)).fold(
            _ => Left(HexDecodeError(item).toString),
            number => Right((Hex @@ number, line.tail))
          )
        }
        .getOrElse(noValueError)
    }

    override def optInfo: Option[String] = Some("hex")
  }

  implicit val longReader: CmdDecoder[Long] = new CmdDecoder[Long] {
    override def decode(line: Line): Either[String, (Long, Line)] =
      line.headOption
        .map { item =>
          Try(item.toLong).fold(
            _ => Left(LongDecodeError(item).toString),
            number => Right((number, line.tail))
          )
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<long>")
  }

  implicit val booleanReader: CmdDecoder[Boolean] = new CmdDecoder[Boolean] {
    override def decode(line: Line): Either[String, (Boolean, Line)] =
      line.headOption
        .map { item =>
          item.toLowerCase match {
            case "true"  => Right((true, line.tail))
            case "false" => Right((false, line.tail))
            case _       => Left(BooleanDecodeError(item).toString)
          }
        }
        .getOrElse(noValueError)

    override def optInfo: Option[String] = Some("<boolean>")
  }

  implicit val yesOrNoReader: CmdDecoder[YesOrNo] = new CmdDecoder[YesOrNo] {
    override def decode(line: Line): Either[String, (YesOrNo, Line)] =
      line.headOption
        .map { item =>
          item.toLowerCase match {
            case "yes" => Right((YesOrNo @@ true, line.tail))
            case "no"  => Right((YesOrNo @@ false, line.tail))
            case _     => Left(YesOrNoDecodeError(item).toString)
          }
        }
        .getOrElse(noValueError)

    override def optInfo: Option[String] = Some("<yesOrNo>")
  }

  implicit val bigDecimalReader: CmdDecoder[BigDecimal] = new CmdDecoder[BigDecimal] {
    override def decode(line: Line): Either[String, (BigDecimal, Line)] =
      line.headOption
        .map(
          item =>
            Try(BigDecimal(item)).fold(
              _ => Left(BigDecimalDecodeError(item).toString),
              v => Right((v, line.tail))
          ))
        .getOrElse(noValueError)
    override def optInfo = Some("<bigdecimal>")
  }

  implicit val doubleReader: CmdDecoder[Double] = new CmdDecoder[Double] {
    override def decode(line: Line): Either[String, (Double, Line)] =
      line.headOption
        .map(
          item =>
            Try(item.toDouble).fold(
              _ => Left(DoubleDecodeError(item).toString),
              v => Right((v, line.tail))
          ))
        .getOrElse(noValueError)
    override def optInfo = Some("<double>")
  }

  implicit val uriReader: CmdDecoder[URI] = new CmdDecoder[URI] {
    override def decode(line: Line): Either[String, (URI, Line)] =
      line.headOption
        .map(
          item =>
            Try(new URI(item)).fold(
              _ => Left(UriDecodeError(item).toString),
              v => Right((v, line.tail))
          ))
        .getOrElse(noValueError)
    override def optInfo = Some("<uri>")
  }

  implicit val durationReader: CmdDecoder[Duration] = new CmdDecoder[Duration] {
    override def decode(line: Line): Either[String, (Duration, Line)] =
      line.headOption
        .map(
          item =>
            Try(Duration(item)).fold(
              _ => Left(DurationDecodeError(item).toString),
              v => Right((v, line.tail))
          ))
        .getOrElse(noValueError)
    override def optInfo = Some("<duration>")
  }

  implicit val stringReader: CmdDecoder[String] = new CmdDecoder[String] {
    override def decode(line: Line): Either[String, (String, Line)] =
      line.headOption
        .map(item => Right((item, line.tail)))
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<string>")
  }

  implicit val charReader: CmdDecoder[Char] = new CmdDecoder[Char] {
    override def decode(line: Line): Either[String, (Char, Line)] =
      line.headOption
        .map {
          case item if item.length != 1 => Left(CharDecodeError(item).toString)
          case item                     => Right((item.head, line.tail))
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<char>")
  }

  // Sample values: 1 OR 1,2,3
  implicit def seqReader[A: CmdDecoder]: CmdDecoder[Seq[A]] = new CmdDecoder[Seq[A]] {
    override def decode(line: Line): Either[String, (Seq[A], Line)] =
      line.headOption
        .map { item =>
          val list: List[Either[String, A]] = item
            .split(',')
            .map(a => implicitly[CmdDecoder[A]].decode(List(a)).map(_._1))
            .toList

          list.sequence.map((_, line.tail))
        }
        .getOrElse(noValueError)

    override val optInfo: Option[String] = Some("<sequence>")
  }

  // Sample values: "a" -> 1 OR 2 -> "b" OR true -> false
  implicit def tuple2Reader[A, B](implicit aDecoder: CmdDecoder[A], bDecoder: CmdDecoder[B]): CmdDecoder[(A, B)] =
    new CmdDecoder[(A, B)] {
      override def decode(line: Line): Either[String, ((A, B), Line)] =
        line.headOption
          .map {
            case item if !item.contains("->") => Left(Tuple2DecodeError(item).toString)
            case item =>
              val parts = item.split("->")
              if (parts.length != 2) Left(Tuple2DecodeError(item).toString)
              else {
                for {
                  a <- aDecoder.decode(List(parts(0))).map(_._1)
                  b <- bDecoder.decode(List(parts(1))).map(_._1)
                } yield ((a, b), line.tail)
              }

          }
          .getOrElse(noValueError)

      override val optInfo: Option[String] = Some("<tuple2>")
    }

  // Sample values: a->true OR a->true,b->false
  implicit def mapReader[K: CmdDecoder, V: CmdDecoder] =
    new CmdDecoder[Map[K, V]] {
      override def decode(line: Line): Either[String, (Map[K, V], Line)] = {
        line.headOption
          .map { item =>
            implicitly[CmdDecoder[Seq[(K, V)]]].decode(List(item)).map(x => (x._1.toMap, line.tail))
          }
          .getOrElse(noValueError)
      }

      override def optInfo = Some("<map>")
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
