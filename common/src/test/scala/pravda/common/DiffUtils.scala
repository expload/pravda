package pravda.common

object DiffUtils {

  def printFirstDiff(s1: String, s2: String): String = {
    val firstDiff = (0 until math.min(s1.length, s2.length)).find(i => s1(i) != s2(i))

    (for {
      idx <- firstDiff
    } yield {
      s"""
        |${s1.slice(idx - 40, idx + 40)}
        |
        |${s2.slice(idx - 40, idx + 40)}
      """.stripMargin
    }).getOrElse("<end of file>")
  }

  def assertEqual(a1: Any, a2: Any): Unit = {
    Predef.assert(a1 == a2, s"$a1 != $a2\n\nFirst diff: ${printFirstDiff(a1.toString, a2.toString)}")
  }
}
