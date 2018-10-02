package pravda.yaml4s

trait YamlMethods extends org.json4s.JsonMethods[JValue] {

  protected[this] def yaml: Yaml

  override def pretty(node: JValue): String = {
    val javaObj = jvalue2java(node)
    yaml.dump(javaObj)
  }

  private[yaml] def jvalue2java(node: JValue): AnyRef = {
    import scala.collection.JavaConversions._
    node match {
      case JNull => null
      case JArray(l) => seqAsJavaList(l.map(jvalue2java))
      case JInt(i) =>
        if (i.isValidInt) Int.box(i.toInt)
        else i.bigInteger
      case JBool(b) => Boolean.box(b)
      case JDecimal(d) => d
      case JDouble(d) => Double.box(d)
      case JNothing => null
      case JString(s) => s
      case JObject(l) => mapAsJavaMap(l.toMap.mapValues(jvalue2java))
    }
  }

  override def render(value: JValue)(implicit formats: Formats = DefaultFormats): JValue =
    formats.emptyValueStrategy.replaceEmpty(value)

  override def compact(d: JValue): String = {
    import org.json4s.native.{ renderJValue, compactJson }
    compactJson(renderJValue(d))
  }

  override def parseOpt(in: JsonInput, useBigDecimalForDouble: Boolean): Option[JValue] = allCatch opt {
    parse(in, useBigDecimalForDouble)
  }

  def parse(in: JsonInput, useBigDecimalForDouble: Boolean = false): JValue = {
    import scala.collection.JavaConversions._

    // WARNING: Yaml.load() accepts a String or an InputStream object. Yaml.load(InputStream stream) detects the encoding by checking the BOM (byte order mark) sequence at the beginning of the stream. If no BOM is present, the utf-8 encoding is assumed.
    // WARNING: Right now I cannot distinguish between null and nothing.

    val tree = in match {
      case StringInput(s) =>  yaml.load(new StringReader(s))
      case ReaderInput(rdr) => yaml.load(rdr)
      case StreamInput(stream) => yaml.load(new InputStreamReader(stream))
      case FileInput(file) => yaml.load(new FileReader(file))
    }

    getJValue(tree)(useBigDecimalForDouble)
  }

  private def getJValue(node: Object)(implicit useBigDecimalForDouble: Boolean): JValue = {
    import scala.collection.JavaConversions._
    node match {
      case null => JNull

      case s: String => JString(s)

      case l: java.util.List[_] =>
        JArray(l.toList.map { v =>
          getJValue(v.asInstanceOf[AnyRef])
        })

      case m: java.util.Map[_, _] =>
        val pairs = m.map { case (k, v) =>
          (k.asInstanceOf[String], getJValue(v.asInstanceOf[AnyRef]))
        }.toList
        JObject(pairs)

      case i: Integer =>
        JInt(BigInt(i))

      case f: java.lang.Double =>
        if (useBigDecimalForDouble) JDecimal(BigDecimal(f))
        else JDouble(f)

      case f: java.lang.Float =>
        if (useBigDecimalForDouble) JDecimal(BigDecimal(f.toDouble))
        else JDouble(f.toDouble)

      case b: java.lang.Boolean =>
        JBool(b)

      case i: java.math.BigInteger =>
        JInt(BigInt(i))
    }
  }
}

object YamlMethods extends YamlMethods {
  // This is because yaml is not thread safe
  private val threadYaml: java.lang.ThreadLocal[Yaml] = new java.lang.ThreadLocal[Yaml] {
    override protected def initialValue() = new Yaml()
  }

  override protected def yaml = threadYaml.get
}