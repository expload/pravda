package pravda.cmdopt


object Application {
  def main(args: Array[String]): Unit = {
    val templates = Renderer.read("./doc-templates")
    templates.map { content =>
      println( Renderer.write("./docs", content) )
    }
  }
}
