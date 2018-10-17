package pravda.node

import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._

object EitherJsonSpecification extends Properties("EitherJson") {

  val left: Gen[Either[String, String]] = Gen.alphaStr.map(Left(_))
  val right: Gen[Either[String, String]] = Gen.alphaStr.map(Right(_))
  val either: Gen[Either[String, String]] = Gen.oneOf(left, right)

  property("write->read") = forAll(either) { either =>
    val json = transcode(either).to[Json]
    transcode(json).to[Either[String, String]] == either
  }

}
