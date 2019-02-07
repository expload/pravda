package pravda.yaml4s

import org.json4s._
import org.json4s.native.JsonMethods._
import utest._

import scala.io.AnsiColor

object YamlDiffSuite extends TestSuite {

  private val coloursLabels = Map(
    AnsiColor.BLACK -> "black",
    AnsiColor.RED -> "red",
    AnsiColor.GREEN -> "green",
    AnsiColor.YELLOW -> "yellow",
    AnsiColor.BLUE -> "blue",
    AnsiColor.MAGENTA -> "magenta",
    AnsiColor.CYAN -> "cyan",
    AnsiColor.WHITE -> "white",
    AnsiColor.BLACK_B -> "black_b",
    AnsiColor.RED_B -> "red_b",
    AnsiColor.GREEN_B -> "green_b",
    AnsiColor.YELLOW_B -> "yellow_b",
    AnsiColor.BLUE_B -> "blue_b",
    AnsiColor.MAGENTA_B -> "magenta_b",
    AnsiColor.CYAN_B -> "cyan_b",
    AnsiColor.WHITE_B -> "white_b",
    AnsiColor.RESET -> "reset",
    AnsiColor.BOLD -> "bold",
    AnsiColor.UNDERLINED -> "underlined",
    AnsiColor.BLINK -> "blink",
    AnsiColor.REVERSED -> "reversed",
    AnsiColor.INVISIBLE -> "invisible"
  )

  private def escapeColours(s: String): String = {
    var res: String = s
    for {
      (colour, label) <- coloursLabels
    } {
      res = res.replace(colour, s"[$label]")
    }

    res
  }

  def tests = Tests {
    "simple object diff" - {
      val json1 = parse("""
           {
             "first body": 1,
             "second body": 2,
             "third body": 3
           }
          """)
      val json2 = parse("""
           {
             "first body": "one",
             "second body": "two",
             "third body": "three"
           }
          """)

      escapeColours(YamlMethods.renderDiff(json1, json2)) ==>
        """first body: [yellow]one[reset]
          |second body: [yellow]two[reset]
          |third body: [yellow]three[reset]""".stripMargin
    }

    "simple object deletion diff" - {
      val json1 = parse("""
           {
             "first body": 1,
             "second body": "two",
             "third body": "three",
             "forth body": "four"
           }
          """)
      val json2 = parse("""
           {
             "first body": "one",
             "second body": "two",
             "third body": "three"
           }
          """)

      escapeColours(YamlMethods.renderDiff(json1, json2)) ==>
        """first body: [yellow]one[reset]
          |second body: two
          |third body: three
          |[red]forth body: four[reset]""".stripMargin
    }

    "simple object addition diff" - {
      val json1 = parse("""
           {
             "first body": 1,
             "second body": "two",
             "third body": "three",
           }
          """)
      val json2 = parse("""
           {
             "first body": "one",
             "second body": "two",
             "third body": "three",
             "forth body": "four"
           }
          """)

      escapeColours(YamlMethods.renderDiff(json1, json2)) ==>
        """first body: [yellow]one[reset]
          |second body: two
          |third body: three
          |[green]forth body: four[reset]""".stripMargin
    }

    "simple object permutation diff" - {
      val json1 = parse("""
           {
             "second body": 2,
             "first body": 1,
             "third body": 3
           }
          """)
      val json2 = parse("""
           {
             "first body": "one",
             "second body": "two",
             "third body": "three"
           }
          """)

      escapeColours(YamlMethods.renderDiff(json1, json2)) ==>
        """second body: [yellow]two[reset]
          |first body: [yellow]one[reset]
          |third body: [yellow]three[reset]""".stripMargin
    }

    "simple array diff" - {
      val json1 = parse("[1, 2, 3]")
      val json2 = parse("[\"one\", \"two\", \"three\"]")

      escapeColours(YamlMethods.renderDiff(json1, json2)) ==>
        """- [yellow]one[reset]
          |- [yellow]two[reset]
          |- [yellow]three[reset]""".stripMargin
    }
  }
}
