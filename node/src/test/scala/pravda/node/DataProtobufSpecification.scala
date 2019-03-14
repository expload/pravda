package pravda.node

import org.scalacheck.Prop._
import org.scalacheck.Properties
import pravda.node.data.serialization._
import pravda.node.data.serialization.protobuf._
import pravda.vm.{Data, DataSpecification}

object DataProtobufSpecification extends Properties("DataProtobuf") {

  property("data write->read") = forAll(DataSpecification.data) { data =>
    val bytes = transcode(data).to[Protobuf]
    transcode(bytes).to[Data] == data
  }
}
