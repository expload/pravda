package pravda.node

import pravda.node.data.serialization._
import pravda.node.data.serialization.json._
import org.scalacheck.Prop._
import org.scalacheck.{Gen, Properties}
import pravda.vm.{Data, DataSpecification}

object DataJsonSpecification extends Properties("DataJson") {

  property("write->read") = forAll(DataSpecification.data) { data =>
    val json = transcode(data).to[Json]
    transcode(json).to[Data] == data
  }

  property("Seq[Data] write->read") = forAll(Gen.listOf(DataSpecification.data)) { data =>
    val json = transcode(data).to[Json]
    transcode(json).to[List[Data]] == data
  }

}
