package pravda.vm.asm

import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalacheck._
import pravda.vm.{Data, DataSpecification}

import scala.util.Random

object PravdaAssemblerSpecification extends Properties("PravdaAssembler") {

  import Operation._

  class Shuffler(seed: Int) { def apply[T](xs: List[T]): List[T] = new Random(seed).shuffle(xs) }

  val genShuffler: Gen[Shuffler] =
    arbitrary[Int].map(s => new Shuffler(s))

  val genDataOps: Gen[List[Operation]] =
    Gen.listOf {
      DataSpecification.data.map {
        case x: Data.Primitive => Push(x)
        case x => New(x)
      }
    }

  val genOrthanOps: Gen[List[Operation]] =
    Gen.listOf(Gen.oneOf(Orphans))

  val genStructOp: Gen[List[Operation]] = Gen.listOf {
    DataSpecification.primitive.flatMap { key =>
      Gen.oneOf(StaticGet(key), StaticMut(key))
    }
  }

  val genControlOps: Gen[List[Operation]] = for {
    size <- Gen.chooseNum(1, 32)
    genArrayChar = Gen.containerOfN[Array, Char](size, Gen.alphaChar)
    labels <- Gen.listOf(genArrayChar.map(xs => new String(xs)))
    calls = labels.map(s => Call(Some(s)))
    jumps = labels.map(s => Jump(Some(s)))
    jumpis = labels.map(s => JumpI(Some(s)))
  } yield {
    labels.map(Label) ++ calls ++ jumps ++ jumpis
  }

  val genCommentOps: Gen[List[Comment]] =
    Gen.listOf(Gen.alphaNumStr.map(x => Operation.Comment(x)))

  def operationsGenerator(addComments: Boolean): Gen[List[Operation]] =
    for {
      shuffle <- genShuffler
      data <- genDataOps
      orphan <- genOrthanOps
      control <- genControlOps
      struct <- genStructOp
      comments <- if (addComments) genCommentOps else Gen.const(Nil)
    } yield {
      // Randomize operations
      shuffle(data ++ orphan ++ control ++ struct ++ comments)
    }

  property("assemble(saveLabels = true) -> disassemble") = forAll(operationsGenerator(false)) { ops =>
    val assembled = PravdaAssembler.assemble(ops, saveLabels = true)
    val disassembled = PravdaAssembler.disassemble(assembled).toList
    disassembled == ops
  }

  property("render -> parser") = forAll(operationsGenerator(true)) { ops =>
    val text = PravdaAssembler.render(ops)
    //println(s"$text\n-------")
    val parsed = PravdaAssembler.parser.parse(text).get.value.toList
    parsed == ops
  }
}
