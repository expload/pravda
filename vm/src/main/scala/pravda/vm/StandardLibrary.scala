package pravda.vm

object StandardLibrary {

  val All = Seq(
    standard.Ripemd160,
    standard.ValidateEd25519Signature
  )

  val Index: Map[Long, (Memory, WattCounter) => Unit] =
    All.map(x => (x.id, x.apply _)).toMap
}
