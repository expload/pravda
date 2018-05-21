package pravda.node.utils

sealed abstract class OperationSystem(name: String) {
  override def toString: String = name
}

object OperationSystem {
  case object Windows     extends OperationSystem("windows")
  case object Linux       extends OperationSystem("linux")
  case object MacOS       extends OperationSystem("macos")
  case object Unsupported extends OperationSystem("?")
}
