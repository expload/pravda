package pravda.yopt

import utest._
import java.net.URI

import scala.concurrent.duration
import scala.concurrent.duration.Duration

object CommandLineTest extends TestSuite {

  import CommandLine.Ok
  import CommandLine.ParseError

  val tests = Tests {

    "unit parser should parse ()" - {
      unitParser("--foo")
      unitParser("-f")
    }

    "grouped parser should parse ()" - {
      groupParser("-abab")
    }

    "int parser should parse" - {
      primTypesParser(1, "--integer", "1")(_.intValue)
    }

    "int parser should fail" - {
      primTypesParserFail("--integer", "FF")
      primTypesParserFail("--integer")
      primTypesParserFail("--integer", "bar")
      primTypesParserFail("--integer", "-90")
    }

    "long parser should parse" - {
      primTypesParser(999L, "--long", "999")(_.longValue)
    }

    "hex parser should parse" - {
      primTypesParser(1L, "--hex", "0x01")(_.hex)
      primTypesParser(1L, "--hex", "0x1")(_.hex)
      primTypesParser(255L, "--hex", "0xFF")(_.hex)
    }

    "bool parser should parse" - {
      primTypesParser(true, "--boolean", "true")(_.boolValue)
      primTypesParser(false, "--boolean", "false")(_.boolValue)
    }

    "YesOrNo parser should parse" - {
      primTypesParser(true, "--yesOrNo", "yes")(_.yesOrNoValue)
      primTypesParser(false, "--yesOrNo", "no")(_.yesOrNoValue)
    }

    "bigdecimal parser should parse" - {
      primTypesParser(BigDecimal(1.0), "--bigdecimal", "1.0")(_.bigDecimalValue)
      primTypesParser(BigDecimal(200), "--bigdecimal", "200")(_.bigDecimalValue)
      primTypesParser(BigDecimal(100), "--bigdecimal", "100")(_.bigDecimalValue)
    }

    "double parser should parse" - {
      primTypesParser(1.0, "--double", "1.0")(_.doubleValue)
      primTypesParser(200.0, "--double", "200")(_.doubleValue)
      primTypesParser(100.0, "--double", "100")(_.doubleValue)
    }

    "uri parser should parse" - {
      otherTypesParser(new URI("http://localhost"), "--uri", "http://localhost")(_.uriValue)
      otherTypesParser(new URI("https://localhost"), "--uri", "https://localhost")(_.uriValue)
      otherTypesParser(new URI("http://localhost:8080/api/public/broadcast"),
                       "--uri",
                       "http://localhost:8080/api/public/broadcast")(_.uriValue)
    }

    "duration parser should parse" - {
      otherTypesParser[Duration](Duration(30, duration.SECONDS), "--duration", "30s")(_.durationValue)
      otherTypesParser[Duration](Duration(2, duration.DAYS), "--duration", "2 day")(_.durationValue)
    }

    "sequence parser should parse" - {
      otherTypesParser(Seq(1), "--seq", "1")(_.seqInts)
      otherTypesParser(Seq(1, 2, 3), "--seq", "1,2,3")(_.seqInts)
    }

    "tuple parser should parse" - {
      otherTypesParser("a" -> 1, "--tuple2", "a->1")(_.tupleOfStringInt)
      otherTypesParser("abc" -> 999, "--tuple2", "abc->999")(_.tupleOfStringInt)
    }

    "map parser should parse" - {
      otherTypesParser(Map("a" -> true), "--map", "a->true")(_.mapStringToBool)
      otherTypesParser(Map("a" -> true, "b" -> false), "--map", "a->true,b->false")(_.mapStringToBool)
    }

    "seq of tuple should parse" - {
      otherTypesParser(Seq(("a", "b")), "--seqtuple", "a->b")(_.seqTupleStringString)
      otherTypesParser(Seq(("a", "b"), ("c", "d")), "--seqtuple", "a->b,c->d")(_.seqTupleStringString)
    }

    "char parser should parse" - {
      primTypesParser('a', "--char", "a")(_.charValue)
    }

    "char parser should fail" - {
      primTypesParserFail("--char", "ab")
    }
  }

  val unitParser1 = new CommandLine[Config] {

    def model =
      head("unit")
        .children(opt[Unit]('f', "foo").action((x, c) => c.copy(flag = true)),
                  opt[Unit]("debug").action((x, c) => c.copy(debug = true)))
  }

  def unitParser(args: String*): Unit = {
    val result = unitParser1.parse(args.toList, Config())
    assert(result == Ok(Config(flag = true)))
  }

  def unitParserHidden(args: String*): Unit = {
    val result = unitParser1.parse(args.toList, Config())
    assert(result == Ok(Config(debug = true)))
  }

  val groupParser1 = new CommandLine[Config] {

    def model = head("group").children(
      opt[Unit]('a', "alice").action { (x, c) =>
        c.copy(flag = true)
      },
      opt[Unit]('b', "bob").action { (x, c) =>
        c.copy(flag = true)
      },
      opt[Unit]("alicebob").action { (x, c) =>
        c.copy(flag = true)
      }
    )
  }

  def groupParser(args: String*): Unit = {
    val result = groupParser1.parse(args.toList, Config())
    assert(result == Ok(Config(flag = true)))
  }

  val primitiveTypesParser = new CommandLine[Config] {

    def model = head("primitive_types").children(
      opt[Int]("integer").action { (x, c) =>
        c.copy(intValue = x)
      },
      opt[Hex]("hex").action { (x, c) =>
        c.copy(hex = x)
      },
      opt[Long]("long").action { (x, c) =>
        c.copy(longValue = x)
      },
      opt[Boolean]("boolean").action { (x, c) =>
        c.copy(boolValue = x)
      },
      opt[YesOrNo]("yesOrNo").action { (x, c) =>
        c.copy(yesOrNoValue = x)
      },
      opt[BigDecimal]("bigdecimal").action { (x, c) =>
        c.copy(bigDecimalValue = x)
      },
      opt[Double]("double").action { (x, c) =>
        c.copy(doubleValue = x)
      },
      opt[Char]("char").action { (x, c) =>
        c.copy(charValue = x)
      },
    )
  }

  val otherTypesParser = new CommandLine[Config] {
    override def model = head("other_types").children(
      opt[URI]("uri").action { (x, c) =>
        c.copy(uriValue = x)
      },
      opt[Duration]("duration").action { (x, c) =>
        c.copy(durationValue = x)
      },
      opt[Seq[Int]]("seq").action { (x, c) =>
        c.copy(seqInts = x)
      },
      opt[(String, Int)]("tuple2").action { (x, c) =>
        c.copy(tupleOfStringInt = x)
      },
      opt[Map[String, Boolean]]("map").action { (x, c) =>
        c.copy(mapStringToBool = x)
      },
      opt[Seq[(String, String)]]("seqtuple").action { (x, c) =>
        c.copy(seqTupleStringString = x)
      }
    )
  }

  def primTypesParser[T](expectedValue: T, args: String*)(slice: Config => T): Unit = {
    primitiveTypesParser.parse(args.toList, Config()) match {
      case Ok(r) => slice(r) ==> expectedValue
      case _     => assert(false)
    }
  }

  def otherTypesParser[T](expectedValue: T, args: String*)(slice: Config => T): Unit = {
    otherTypesParser.parse(args.toList, Config()) match {
      case Ok(r) => slice(r) ==> expectedValue
      case _     => assert(false)
    }
  }

  def primTypesParserFail(args: String*): Unit = {
    val result = primitiveTypesParser.parse(args.toList, Config())
    assert(result.isInstanceOf[ParseError])
  }

  def otherTypesParserFail(args: String*): Unit = {
    val result = otherTypesParser.parse(args.toList, Config())
    assert(result.isInstanceOf[ParseError])
  }

  final case class Config(
      flag: Boolean = false,
      intValue: Int = 0,
      hex: Long = 0L,
      longValue: Long = 0L,
      stringValue: String = "",
      doubleValue: Double = 0.0,
      boolValue: Boolean = false,
      yesOrNoValue: Boolean = false,
      debug: Boolean = false,
      bigDecimalValue: BigDecimal = BigDecimal("0.0"),
      uriValue: URI = new URI("http://localhost"),
      durationValue: Duration = Duration("0s"),
      key: String = "",
      a: String = "",
      b: String = "",
      seqInts: Seq[Int] = Seq(),
      tupleOfStringInt: (String, Int) = ("", 0),
      mapStringToBool: Map[String, Boolean] = Map(),
      seqTupleStringString: Seq[(String, String)] = Nil,
      charValue: Char = 0
  )
}
