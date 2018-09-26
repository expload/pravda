package pravda.evm.parse

import fastparse.byte.all._
import pravda.evm.EVM
import pravda.evm.EVM._

object Parser {

  def apply(bytes: Bytes):Either[String,List[EVM.Op]] = {
    ops.parse(bytes).get.value
  }

  private def push(cnt: Int): P[Push] = AnyByte.rep(exactly = cnt).!.map(Push)
  // FIXME the bytes default to zero if they extend past the limits

  val op: P[Either[String, Op]] = {

    def checkPush(i: Int) =
      if ((0x60 to 0x7f).contains(i)) {
        Some(push(i - 0x60 + 1))
      } else {
        None
      }

    def checkRanges(i: Int) = rangeOps.find(_._1.contains(i)).map(r => r._2(i))
    def checkSingleOps(i: Int) = singleOps.get(i)

    Int8.flatMap(b => {
      val i = b & 0xff
      checkSingleOps(i)
        .orElse(checkRanges(i))
        .map(PassWith)
        .orElse(checkPush(i))
        .map(_.map(Right(_)))
        .getOrElse(PassWith(Left(s"Unknown opcode: 0x${i.toHexString}")))
    })
  }

  private def sequence[T](l: List[Either[String, T]]): Either[String, List[T]] =
    l.foldRight(Right(Nil): Either[String, List[T]]) { (e, acc) =>
      for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  private val ops: P[Either[String, List[Op]]] = P(Start ~ op.rep ~ End).map(ops => sequence(ops.toList))

  //private val opsWithIndices = P(Start  ~ (Index ~ op).rep ~ End)

}
