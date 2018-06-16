package pravda.vm.state

import java.nio.ByteBuffer

final case class ProgramContext(storage: Storage, code: ByteBuffer)
