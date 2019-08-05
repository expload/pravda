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

import pravda.common.cryptography
import pravda.vm._
import pravda.common.vm._

object ValidateEd25519Signature extends FunctionDefinition {

  val id: Long = 0x01L

  val description: String = "Validates message signed with Ed25519 algorithm. See https://ed25519.cr.yp.to"

  val returns = Seq(Data.Type.Boolean)

  val args = Seq(
    "pubKey" -> Seq(Data.Type.Bytes),
    "message" -> Seq(Data.Type.Bytes, Data.Type.Utf8),
    "signature" -> Seq(Data.Type.Bytes),
  )

  def apply(memory: Memory, wattCounter: WattCounter): Unit = {
    val signature = operations.bytes(memory.pop())
    val message = memory.pop() match {
      case Data.Primitive.Bytes(data) => data.toByteArray
      case Data.Primitive.Utf8(data)  => data.getBytes(StandardCharsets.UTF_8)
      case _                          => throw ThrowableVmError(Error.WrongType)
    }
    val pubKey = operations.bytes(memory.pop())
    val result = cryptography.verify(pubKey.toByteArray, message, signature.toByteArray)
    wattCounter.cpuUsage((signature.size() + message.length + pubKey.size) * WattCounter.CpuArithmetic)
    memory.push(Data.Primitive.Bool(result))
  }
}
