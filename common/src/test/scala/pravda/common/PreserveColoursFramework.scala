package pravda.common
import utest.ufansi.Attrs

class PreserveColoursFramework extends utest.runner.Framework {
  override def exceptionMsgColor: Attrs = Attrs.Empty
}
