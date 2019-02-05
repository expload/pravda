package pravda.vm.asm
import pravda.vm.Meta

import scala.concurrent.Future

trait MetaLoader {
  def load(includeMeta: Meta.MetaInclude): Future[Map[Int, Seq[Meta]]]
}
