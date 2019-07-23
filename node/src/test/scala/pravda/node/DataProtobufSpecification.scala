package pravda.node

import org.scalacheck.Prop._
import org.scalacheck.Properties
import pravda.common.serialization._
import pravda.node.data.serialization._
import pravda.common.serialization.protobuf._
import pravda.common.vm.Data
import pravda.vm.DataSpecification

object DataProtobufSpecification extends Properties("DataProtobuf") {

  property("data write->read") = forAll(DataSpecification.data) { data =>
    val bytes = transcode(data).to[Protobuf]
    transcode(bytes).to[Data] == data
  }
}
