package pravda.node.data.serialization

final class TranscodingDsl[From](val value: From) extends AnyVal {

  def to[To](implicit transcoder: Transcoder[From, To]): To =
    transcoder(value)
}
