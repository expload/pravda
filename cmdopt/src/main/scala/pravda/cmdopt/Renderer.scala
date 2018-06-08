package pravda.cmdopt

object Renderer {

  import java.io.File
  import java.nio.file.Files
  import java.nio.charset.StandardCharsets


  private object Resource {
    class AutoCloseableWrapper[A <: AutoCloseable](protected val c: A) {
      def map[B](f: (A) => B): B = {
        try {
          f(c)
        } finally {
          c.close()
        }
      }
      def flatMap[B](f: (A) => B): B = map(f)
    }
    def apply[A <: AutoCloseable](c: A) = new AutoCloseableWrapper(c)
  }

  def render[C](cl: CommandLine[C], template: Map[String, String]): Either[String, Map[String, String]] = {
    import scala.collection.JavaConverters._
    import com.github.mustachejava.MustacheFactory
    import com.github.mustachejava.DefaultMustacheFactory
    def render(muf: MustacheFactory, name: String, template: String, scope: java.util.Map[String, AnyRef]): Either[String, String] = {
      for {
        in <- Resource(new java.io.StringReader(template))
        out <- Resource(new java.io.StringWriter())
      } yield {
        val mus = muf.compile(in, name)
        mus.execute(out, scope)
        Right(out.toString())
      }
    }
    def walk(m: List[cl.Verb[_]]): List[(String, Map[String, AnyRef])] = {
      m.collect{ case cmd: cl.Cmd => cmd }.flatMap{ cmd =>
        List(
          cmd.docref -> Map("name" -> cmd.name)
        ) ++ walk(cmd.verbs)
      }
    }
    try {
      val muf = new DefaultMustacheFactory
      val items = walk(cl.model)
      val templates = items.map{ case (docref, cmd) => (docref, cmd, template.get(docref)) }
      val missing = templates.collect{ case (k, v, None) => k }
      if (!missing.isEmpty) {
        Left("Doc templates are missing for: " + missing.mkString(","))
      } else {
        val result = templates.map{ case (docref, cmd, template) =>
          render(muf, docref, template.get, cmd.asJava).map(docref -> _)
        }
        val succ = result.collect{ case Right((docref, doc)) => (docref, doc) }
        val fail = result.collect{ case Left(err) => err }
        if (fail.isEmpty) {
          Right(succ.toMap)
        } else {
          Left(fail.mkString("\n"))
        }
      }
    } catch {
      case ex: Exception => Left(s"Failed to render: ${ex.getClass}:${ex.getMessage}")
    }
  }

  def write(rootPath: String, content: Map[String, String]): Either[String, Unit] = {
    import java.nio.file.StandardOpenOption.{CREATE, WRITE, TRUNCATE_EXISTING}
    val opts = Array(WRITE, CREATE, TRUNCATE_EXISTING)
    try {
      content.map{ case (k, v) =>
        val path = new File(new File(rootPath), k)
        path.getParentFile().mkdirs()
        Files.write(path.toPath, v.getBytes(StandardCharsets.UTF_8), opts:_*)
      }
      Right(())
    } catch {
        case ex: Exception => Left(s"Failed to write content to $rootPath: ${ex.getClass}:${ex.getMessage}")
    }
  }

  def read(rootPath: String): Either[String, Map[String, String]] = {
    def read(file: File, root: File): (String, String) = {
      val name = root.toURI.relativize(file.toURI).toString
      val content = new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8)
      (name, content)
    }
    def walk(dir: File, root: File): Either[String, Map[String, String]] = {
      try {
        val dirs = dir.listFiles().filter(_.isDirectory()).toSeq
        val files = dir.listFiles().filter(!_.isDirectory()).toSeq
        val acc: Either[String, Map[String, String]] = Right(files.map(read(_, root)).toMap)
        dirs.map(walk(_, root)).foldLeft(acc){ (ea, en) =>
          for (
            a <- ea;
            n <- en
          ) yield (a ++ n)
        }
      } catch {
        case ex: Exception => Left(s"Failed to read templates in $dir: ${ex.getClass}:${ex.getMessage}")
      }
    }
    val root = new File(rootPath)
    if (!root.exists()) {
      Left(s"Directory not found: $rootPath")
    } else {
      walk(root, root)
    }
  }

}
