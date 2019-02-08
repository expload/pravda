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

package pravda.cli

import cats._
import cats.data.EitherT
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.node.client.{IpfsLanguage, MetadataLanguage}
import pravda.vm.Meta

import scala.collection.mutable
import scala.language.higherKinds

package object programs {

  def useOption[F[_]: Monad, A, B](maybe: Option[A])(none: => F[B],
                                                     some: A => F[Either[String, B]]): EitherT[F, String, B] = {
    EitherT[F, String, B] {
      maybe.fold[F[Either[String, B]]](none.map(Right.apply))(some)
    }
  }

  def loadIncludes[F[_]: Monad](source: ByteString, ipfsNode: String)(
      ipfs: IpfsLanguage[F],
      metadata: MetadataLanguage[F]): F[(ByteString, Map[Int, Seq[Meta]])] = {
    for {
      extracted <- metadata.extractPrefixIncludes(source)
      (bytecode, includes) = extracted
      externalFiles <- includes
        .map {
          case Meta.IpfsFile(hash) => ipfs.loadFromIpfs(ipfsNode, hash)
        }
        .toList
        .sequence
        .map(_.flatten)
      externalMetas = externalFiles.map(bs => Meta.externalReadFromByteBuffer(bs.asReadOnlyByteBuffer()))
      mergedMeta = {
        val m = mutable.Map[Int, mutable.Buffer[Meta]]()
        externalMetas.foreach[Unit](_.foreach[Unit] {
          case (k, v) =>
            m.get(k) match {
              case Some(buff) => buff ++= v
              case None       => m += k -> mutable.Buffer(v: _*)
            }
        })
        m.toMap
      }
    } yield (bytecode, mergedMeta)
  }
}
