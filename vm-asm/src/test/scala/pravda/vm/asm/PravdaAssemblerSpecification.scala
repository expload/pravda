package pravda.vm.asm

import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalacheck._
import pravda.vm.{Data, DataSpecification}

import scala.util.Random

object PravdaAssemblerSpecification extends Properties("PravdaAssembler") {

  import Operation._

  class Shuffler(seed: Int) { def apply[T](xs: List[T]): List[T] = new Random(seed).shuffle(xs) }

  def genShuffler: Gen[Shuffler] =
    arbitrary[Int].map(s => new Shuffler(s))

  val genDataOps: Gen[List[Operation]] =
    Gen.listOf {
      DataSpecification.data.map {
        case x: Data.Primitive => Push(x)
        case x => New(x)
      }
    }

  val genSimpleOps: Gen[List[Operation]] =
    Gen.listOf(Gen.oneOf(SimpleOperations.toSeq))

  val genControlOps: Gen[List[Operation]] = for {
    shuffle <- genShuffler
    labels <- Gen.listOf(Gen.alphaStr.suchThat(s => s.length > 2 && s.length < 6))
    calls = labels.map(s => Call(Some(s)))
    jumps = labels.map(s => Jump(Some(s)))
    jumpis = labels.map(s => JumpI(Some(s)))
  } yield {
    labels.map(Label) ++ shuffle(calls ++ jumps ++ jumpis)
  }

  val genCommentOps: Gen[List[Comment]] =
    Gen.listOf(Gen.asciiPrintableStr.map(x => Operation.Comment(x)))

  def operationsGenerator(addComments: Boolean): Gen[List[Operation]] = {
    val components = {
      val all = List(genDataOps, genSimpleOps, genControlOps)
      if (!addComments) all
      else genCommentOps :: all
    }
    genShuffler flatMap { shuffler =>
      import collection.JavaConverters._ // WAT??
      // Randomize and select only two generators.
      // Otherwise generator is too complex and it leads to 'give up' error.
      val selectedComponents = shuffler(components).take(2)
      Gen.sequence(selectedComponents)
        .map(x => x.asScala.toList.flatten)
    }
  }

  property("assemble(saveLabels = true) -> disassemble") = forAll(operationsGenerator(false)) { ops =>
    val assembled = PravdaAssembler.assemble(ops, saveLabels = true)
    val disassembled = PravdaAssembler.disassemble(assembled).toList
    disassembled == ops
  }

  property("render -> parser") = forAll(operationsGenerator(true)) { ops =>
    val text = PravdaAssembler.render(ops)
    val parsed = PravdaAssembler.parser.parse(text).get.value.toList
    parsed == ops
  }
}
