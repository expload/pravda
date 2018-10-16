package pravda.vm.asm.json

import pravda.vm.asm.{Operation, PravdaAssembler}
import tethys.{JsonReader, JsonWriter}

trait TethysInstances {
  implicit val asmOpReader: JsonReader[Operation] =
    JsonReader.stringReader.map(s => PravdaAssembler.operationParser.parse(s).get.value)

  implicit val asmOpWriter: JsonWriter[Operation] =
    JsonWriter.stringWriter.contramap(op => PravdaAssembler.render(op, false))
}
