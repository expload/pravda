package pravda.vm

sealed abstract class VmError(val code: Int)

object VmError {

  case object StackOverflow     extends VmError(100)
  case object StackUnderflow    extends VmError(101)
  case object WrongStackIndex   extends VmError(102)
  case object WrongHeapIndex    extends VmError(103)
  case object WrongType         extends VmError(104)
  case object InvalidCoinAmount extends VmError(104)
  case object InvalidAddress    extends VmError(104)

  case object OperationDenied      extends VmError(200)
  case object NoSuchProgram        extends VmError(300)
  case object NoSuchLibrary        extends VmError(301)
  case object NoSuchMethod         extends VmError(302)
  case object NoSuchElement        extends VmError(400)
  case object OutOfWatts           extends VmError(500)
  case object CallStackOverflow    extends VmError(600)
  case object CallStackUnderflow   extends VmError(601)
  case object ExtCallStackOverflow extends VmError(602)

  final case class SomethingWrong(ex: Throwable) extends VmError(999)
}
