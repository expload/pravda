package pravda.vm

import java.nio.ByteBuffer

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{Properties, _}

import scala.annotation.strictfp

@strictfp object MetaSpecification extends Properties("Meta") {

  val genMeta: Gen[Meta] = Gen.oneOf(
    arbitrary[String].map(Meta.LabelUse),
    arbitrary[String].map(Meta.LabelDef),
    arbitrary[String].map(Meta.Custom)
  )

  property("writeToByteBuffer -> readFromByteBuffer") = forAll(genMeta) { meta =>
    val buffer = ByteBuffer.allocate(64 * 1024)
    meta.writeToByteBuffer(buffer)
    buffer.flip()
    Meta.readFromByteBuffer(buffer) == meta
  }
}