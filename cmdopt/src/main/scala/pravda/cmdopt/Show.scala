package pravda.cmdopt

trait Show[A, O] {
  def show(a: A): String
  def show(a: A, o: O): String
}

object Show {
}
