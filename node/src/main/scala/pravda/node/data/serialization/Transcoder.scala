package pravda.node.data.serialization

trait Transcoder[From, To] extends (From => To)
