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

package pravda.vm.standard

import java.nio.charset.StandardCharsets

import com.google.protobuf.ByteString
import org.bouncycastle.jcajce.provider.digest._
import pravda.vm._
import pravda.common.vm._

object Sha3 extends FunctionDefinition {

  val id: Long = 0x09L

  val description: String =
    "Calculate Keccak-256 hash for message."

  val returns = Seq(Data.Type.Bytes)

  val args = Seq(
    "message" -> Seq(Data.Type.Bytes, Data.Type.Utf8)
  )

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val message = memory.pop() match {
      case Data.Primitive.Bytes(data) => data.toByteArray
      case Data.Primitive.Utf8(data)  => data.getBytes(StandardCharsets.UTF_8)
      case _                          => throw ThrowableVmError(Error.WrongType)
    }
    val digest = new Keccak.Digest256
    val result = digest.digest(message)
    wattCounter.cpuUsage(message.length * WattCounter.CpuArithmetic)
    memory.push(Data.Primitive.Bytes(ByteString.copyFrom(result)))
  }
}
