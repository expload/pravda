package pravda.vm.state

import java.nio.ByteBuffer

// TODO @fomkin: looks like it should be case class
trait ProgramContext {
  def storage: Storage
  def code: ByteBuffer
}
