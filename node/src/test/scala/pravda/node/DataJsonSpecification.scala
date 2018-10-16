package pravda.node

import pravda.node.data.serialization._
import pravda.node.data.serialization.json._

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import pravda.vm.{Data, DataSpecification}

object DataJsonSpecification extends Properties("DataJson") {

  property("write->read") = forAll(DataSpecification.data) { data =>
    val json = transcode(data).to[Json]
    transcode(json).to[Data] == data
  }

}