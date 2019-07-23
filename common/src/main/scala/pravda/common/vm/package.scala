package pravda.common

package object vm {
  // Result of program execution
  type ExecutionResult = Either[RuntimeException, FinalState]
}
