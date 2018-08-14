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

sealed trait CmdDecoderError {
  val message: String
  override def toString = message
}

final case class IntegerDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as integer value"
}
final case class LongDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as long value"
}
case object NegativeNumberDecodeError extends CmdDecoderError {
  override val message = "For now, only positive numbers are allowed"
}
final case class BooleanDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as boolean value"
}
final case class BigDecimalDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as bigdecimal value"
}
final case class DoubleDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as double value"
}
final case class CharDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as char value"
}
final case class UriDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as URI"
}
final case class DurationDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as duration"
}
final case class SequenceDecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as sequence"
}
final case class Tuple2DecodeError(toDecode: String) extends CmdDecoderError {
  override val message = s"$toDecode cannot be decoded as tuple"
}
