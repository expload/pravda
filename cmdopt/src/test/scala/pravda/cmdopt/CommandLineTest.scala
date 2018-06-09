package pravda.cmdopt

import utest._
import java.net.URI
import scala.concurrent.duration.Duration

object CommandLineTest extends TestSuite {

  val tests = Tests {

    "unit parser should parse ()" - {
      unitParser("--foo")
      unitParser("-f")
    }

    "grouped parser should parse ()" - {
      groupParser("--ab")
      groupParser("-abab")
    }

    "int parser should parse 1" - {
      intParser("--foo", "1")
      intParser("-f", "1")
      intParser("--foo", "0x01")
      intParser("-f", "0x1")
      intParserFail{"--foo"}
      intParserFail("--foo", "bar")
      intParserFail("--foo=bar")
    }

  }

  val unitParser1 = new CommandLine[Config] { def model = List(
    opt[Unit]('f', "foo").action( (x, c) => c.copy(flag = true) )
    , opt[Unit]("debug").action( (x, c) => c.copy(debug = true) )
  ) }
  def unitParser(args: String*): Unit = {
    val result = unitParser1.parse(args.toSeq, Config())
    assert(result == Right(Config(flag = true)))
  }
  def unitParserHidden(args: String*): Unit = {
    val result = unitParser1.parse(args.toSeq, Config())
    assert(result == Right(Config(debug = true)))
  }

  val groupParser1 = new CommandLine[Config] { def model = List(
    opt[Unit]('a', "alice").action{ (x, c) => c.copy(flag = true) },
    opt[Unit]('b', "bob").action{ (x, c) => c.copy(flag = true) },
    opt[Unit]("alicebob").abbr("ab").action{ (x, c) => c.copy(flag = true) }
  ) }
  def groupParser(args: String*): Unit = {
    val result = groupParser1.parse(args.toSeq, Config())
    assert(result == Right(Config(flag = true)))
  }

  val intParser1 = new CommandLine[Config] { def model = List(
    opt[Int]('f', "foo").action{ (x, c) => c.copy(intValue = x) }
  ) }
  def intParser(args: String*): Unit = {
    val result = intParser1.parse(args.toSeq, Config())
    assert(result == Right(Config(intValue = 1)))
  }
  def intParserFail(args: String*): Unit = {
    val result = intParser1.parse(args.toSeq, Config())
    assert(result.isInstanceOf[Left[String, Config]])
  }

  case class Config(
    flag: Boolean = false,
    intValue: Int = 0,
    longValue: Long = 0L,
    stringValue: String = "",
    doubleValue: Double = 0.0,
    boolValue: Boolean = false,
    debug: Boolean = false,
    bigDecimalValue: BigDecimal = BigDecimal("0.0"),
    uriValue: URI = new URI("http://localhost"),
    durationValue: Duration = Duration("0s"),
    key: String = "", a: String = "", b: String = "",
    seqInts: Seq[Int] = Seq(),
    mapStringToBool: Map[String,Boolean] = Map(),
    seqTupleStringString: Seq[(String, String)] = Nil,
    charValue: Char = 0
  )
}
