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

package pravda.node.client.impl

import java.io.IOException

import com.google.protobuf.ByteString
import io.ipfs.api.{IPFS, NamedStreamable}
import io.ipfs.multihash.Multihash
import pravda.node.client.IpfsLanguage

import scala.concurrent.{ExecutionContext, Future}

final class IpfsLanguageImpl(implicit executionContext: ExecutionContext) extends IpfsLanguage[Future] {

  def loadFromIpfs(ipfsHost: String, base58: String): Future[Option[ByteString]] = {
    Future {
      val ipfs = new IPFS(ipfsHost)
      val hash = Multihash.fromBase58(base58)
      Option(ByteString.copyFrom(ipfs.cat(hash)))
    }.recover {
      case e: RuntimeException => None
      case e: IOException      => None
    }
  }

  def writeToIpfs(ipfsHost: String, bytes: ByteString): Future[Option[String]] = {
    Future {
      val ipfs = new IPFS(ipfsHost)
      val file = new NamedStreamable.ByteArrayWrapper(bytes.toByteArray)
      Option(ipfs.add(file).get(0).hash.toBase58)
    }.recover {
      case e: RuntimeException => None
      case e: IOException      => None
    }
  }
}
