package pravda

package vm

import java.nio.ByteBuffer

import pravda.common.domain

trait Vm {

  def spawn(program: ByteBuffer,
            environment: Environment,
            memory: Memory,
            wattCounter: WattCounter,
            maybeStorage: Option[Storage],
            programAddress: Option[domain.Address],
            pcallAllowed: Boolean): ExecutionResult
}
