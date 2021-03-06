package pravda.node

import pravda.common.serialization._
import pravda.node.data.serialization.json._
import org.scalacheck.Prop._
import org.scalacheck.{Gen, Properties}
import pravda.common.vm.Data
import pravda.vm.asm.DataSpecification

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
