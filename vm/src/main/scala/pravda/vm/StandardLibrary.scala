package pravda.vm

object StandardLibrary {

  final val ValidateEd25519Signature = 0x01L

  val implementation: Map[Long, (Memory, WattCounter) => Unit] = Map(
    ValidateEd25519Signature -> { (memory, wattcounter) =>
      
    }
  )
}
