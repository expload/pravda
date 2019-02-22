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
import pravda.node.client.{IoLanguage, IpfsLanguage, MetadataLanguage}
import pravda.vm.Meta
import pravda.vm.asm.{Operation, PravdaAssembler}

import scala.collection.mutable
import scala.language.higherKinds

package object programs {

  def useOption[F[_]: Monad, A, B](maybe: Option[A])(none: => F[B],
                                                     some: A => F[Either[String, B]]): EitherT[F, String, B] = {
    EitherT[F, String, B] {
      maybe.fold[F[Either[String, B]]](none.map(Right.apply))(some)
    }
  }

  def loadMetaFromSource(source: ByteString): Map[Int, Seq[Meta]] = {
    val metas = mutable.Map[Int, mutable.Buffer[Meta]]()

    PravdaAssembler.disassemble(source).foreach {
      case (i, Operation.Meta(m)) =>
        if (metas.contains(i)) {
          metas(i) += m
        } else {
          metas(i) = mutable.Buffer[Meta](m)
        }
      case _ =>
    }

    metas.toMap
  }

  def loadAllMetaWithoutIpfs[F[_]: Monad](source: ByteString)(metadata: MetadataLanguage[F]): F[Map[Int, Seq[Meta]]] = {
    for {
      extracted <- metadata.extractPrefixIncludes(source)
      (bytecode, _) = extracted
    } yield loadMetaFromSource(bytecode)
  }

  def loadAllMeta[F[_]: Monad](source: ByteString, ipfsNode: String)(
      io: IoLanguage[F],
      ipfs: IpfsLanguage[F],
      metadata: MetadataLanguage[F]): F[Map[Int, Seq[Meta]]] = {
    for {
      includes <- loadIncludes(source, ipfsNode)(io, ipfs, metadata)
      (bytecode, metas) = includes
    } yield {
      val extendedMetas = mutable.Map[Int, mutable.Buffer[Meta]](metas.mapValues(_.toBuffer).toSeq: _*)

      loadMetaFromSource(source).foreach {
        case (i, sourceMetas) =>
          if (extendedMetas.contains(i)) {
            extendedMetas(i) ++= sourceMetas
          } else {
            extendedMetas(i) = mutable.Buffer(sourceMetas: _*)
          }
      }

      extendedMetas.toMap
    }
  }

  def loadIncludes[F[_]: Monad](source: ByteString, ipfsNode: String)(
      io: IoLanguage[F],
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
