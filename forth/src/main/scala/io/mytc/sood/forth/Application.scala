package io.mytc.sood.forth


object Application {

  def main(args: Array[String]): Unit = {

    def test(code: String): Unit = Parser().parse(code) match {
      case Right(res)     ⇒ println(res)
      case Left(errorMsg) ⇒ println("failed: " + errorMsg)
    }

    test(":booka boo 3 *;")
    test("2   5 + a234@#$ 0< if boo goo doo then:booka print print 2 *;")
    test("abc")
    test("<0")
    test(">0<")
    test(">0")

  }

}
