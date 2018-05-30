package pravda.dotnet

object DiffUtils {

  def printFirstDiff(s1: String, s2: String): Unit = {
    val firstDiff = (0 until math.min(s1.length, s2.length)).find(i => s1(i) != s2(i))

    for {
      idx <- firstDiff
    } yield {
      println(s1.slice(idx - 10, idx + 10))
      println(s2.slice(idx - 10, idx + 10))
    }
  }
}
