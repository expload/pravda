package io.mytc.timechain.data

import io.mytc.timechain.data.common.{Address, Matrix, Mytc, TariffMatrix}
import utest._

object PriceCalcTest  extends TestSuite {

  def col(values: BigDecimal*) = values.toVector

  def matrix(cols: Vector[BigDecimal]*): Matrix[BigDecimal] = {
    val rows = cols(0).length
    assert(cols.forall(_.length == rows))
    Matrix(rows, cols.length, cols.flatten.toVector)
  }

  def tariff(cols: Vector[BigDecimal]*): TariffMatrix = {
    TariffMatrix(Address.fromHex("aaff"), matrix(cols: _*))
  }

  val tests = Tests {
    "price should be work at blocks which are powers of two" - {
      val matrix = tariff(col(6, 5, 4, 3, 2, 1))
      assert(matrix.price(1, 0, 1, 1) == 6)
      assert(matrix.price(1, 1, 1, 1) == 6)
      assert(matrix.price(1, 2, 1, 1) == 5)
      assert(matrix.price(1, 4,  1, 1) == 4)
      assert(matrix.price(1, 8, 1, 1) == 3)
      assert(matrix.price(1, 16, 1, 1) == 2)
      assert(matrix.price(1, 32, 1, 1) == 1)
    }

    "it should be work at block 0 with different multipliers" - {
      val matrix = tariff(col(4, 3, 2, 1), col(31, 21, 11, 1))
      assert(matrix.price(2, 0, 1, 1) == 31)
      assert(matrix.price(1, 0, 1, 1) == 4)
      assert(matrix.price(1, 0, 3, 1) == 12)
      assert(matrix.price(1, 0, 1, 11) == 44)
      assert(matrix.price(1, 0, 3, 5) == 60)
    }

    "it should be work at block 1" - {
      val matrix = tariff(col(4, 3, 2, 1), col(7, 6, 2, 2))
      assert(matrix.price(1, 1, 1, 1) == 4)
      assert(matrix.price(2, 1, 1, 1) == 7)
    }

    "it should be work at block 2" - {
      val matrix = tariff(col(4, 3, 2, 1), col(7, 6, 2, 2))
      assert(matrix.price(1, 2, 1, 1) == 3)
      assert(matrix.price(2, 2, 1, 1) == 6)
    }

    "it should take the last element if block is more than matrix size" - {
      val matrix = tariff(col(345, 33, 21, 17))
      assert(matrix.price(1, 10000, 1, 1) == 17)
    }

    "it should calculate fraction between tariff records" - {
      val matrix = tariff(col(6, 5, 4, 3, 2, 1))
      assert(matrix.price(1, 4, 1, 1) == 4)
      assert(matrix.price(1, 8, 1, 1) == 3)
      assert(matrix.price(1, 6, 1, 1) < 4)
      assert(matrix.price(1, 6, 1, 1) > 3)
    }

  }
}
