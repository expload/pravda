package pravda.yaml4s

import org.json4s._
import org.json4s.native.JsonMethods._
import utest._

object YamlDiffSuite extends TestSuite {
  def tests = Tests {
    "simple diff" - {
      val json1 = parse(
          """
           {
             "first body": 1,
             "second body": 2,
             "third body": 3
           }
          """)
      val json2 = parse(
        """
           {
             "first body": "one",
             "second body": "two",
             "third body": "three"
           }
          """)
      println(YamlMethods.renderDiff(json1, json2))
    }
  }
}
