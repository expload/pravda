package pravda.vm.asm

import java.nio.ByteBuffer

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{Properties, _}
import pravda.common.vm.Meta
import pravda.common.vm.Meta.{MethodSignature, TypeSignature}

import scala.annotation.strictfp

@strictfp object MetaSpecification extends Properties("Meta") {

  val genPrimitiveSignature: Gen[TypeSignature.PrimitiveType] = Gen.oneOf(
    TypeSignature.Null,
    TypeSignature.Int8,
    TypeSignature.Int16,
    TypeSignature.Int32,
    TypeSignature.Int64,
    TypeSignature.BigInt,
    TypeSignature.Number,
    TypeSignature.Boolean,
    TypeSignature.Ref,
    TypeSignature.Utf8,
    TypeSignature.Bytes
  )

  val genArraySignature: Gen[TypeSignature.Array] = genPrimitiveSignature.map(TypeSignature.Array)

  val genTypeSignature: Gen[TypeSignature] = Gen.frequency((1, genArraySignature), (10, genPrimitiveSignature))

  val genMethod: Gen[MethodSignature] = for {
    name <- Gen.alphaStr
    returnTpe <- genTypeSignature
    args <- Gen.listOf(genTypeSignature)
  } yield MethodSignature(name, returnTpe, args)

  val genMeta: Gen[Meta] = Gen.oneOf(
    arbitrary[String].map(Meta.LabelUse),
    arbitrary[String].map(Meta.LabelDef),
    arbitrary[String].map(Meta.Custom),
    genMethod
  )

  property("writeToByteBuffer -> readFromByteBuffer") = forAll(genMeta) { meta =>
    val buffer = ByteBuffer.allocate(64 * 1024)
    meta.writeToByteBuffer(buffer)
    buffer.flip()
    Meta.readFromByteBuffer(buffer) == meta
  }

  property("mkString -> parse") = forAll(genMeta) { meta =>
    Meta.parser.meta.parse(meta.mkString).get.value == meta
  }
}
