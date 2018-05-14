package io.mytc.timechain.data

import io.mytc.timechain.data.blockchain.TransactionData.{MultiplierUpdating, TariffMatrixUpdating}
import io.mytc.timechain.data.common.{Matrix, TariffMatrix}
import io.mytc.timechain.data.processing.ProcessingEffect.UpdateTariffMatrix
import io.mytc.timechain.data.processing.{ProcessingError, ProcessingState}
import utest._
import testutils._
import testutils.transactions._


object PricesTransactionTest extends TestSuite {

  val stEmpty = ProcessingState()
  val bob = User.create(free = 100)
  val state = stEmpty.copy(accounts = Map(bob.account.address -> bob.account))

  val tests = Tests {
    "Valid update multiplier transaction should be successfull" - {
      bob.propose(MultiplierUpdating(3), state) shouldBeSuccess

      bob.propose(MultiplierUpdating(0), state) shouldBeSuccess
    }

    "Negative multiplier will not pass" - {
      bob.propose(MultiplierUpdating(-3), state) shouldBe ProcessingError.ShouldNotBeNegative
    }

    def col(values: BigDecimal*) = values.toVector

    def matrix(cols: Vector[BigDecimal]*): Matrix[BigDecimal] = {
      val rows = cols(0).length
      assert(cols.forall(_.length == rows))
      Matrix(rows, cols.length, cols.flatten.toVector)
    }

    def tariff(cols: Vector[BigDecimal]*): TariffMatrix = {
      TariffMatrix(bob.account.address, matrix(cols: _*))
    }

    val validTariffMatrix = tariff(col(3, 2, 1), col(3, 2, 1))
    "Valid tariff matrix updating should be successful" - {
      bob.propose(TariffMatrixUpdating(validTariffMatrix), state) shouldBeSuccess
    }

    "Tariff matrix with wrong order shoud be invalid" - {
      val inorderMatrix = tariff(col(2, 3, 1), col(3, 2, 1))
      bob.propose(TariffMatrixUpdating(inorderMatrix), state) shouldBe ProcessingError.InvalidMatrix

      val validMatrix = tariff(col(2))
      bob.propose(TariffMatrixUpdating(validMatrix), state) shouldBeSuccess
    }

    "Tariff matrix with negative values shoul be invalid" - {
      val notPositiveMatrix = tariff(col(2, 1, -1), col(3, 2, 1))
      bob.propose(TariffMatrixUpdating(notPositiveMatrix), state) shouldBe ProcessingError.ShouldNotBeNegative

      val zeroOkMatrix = tariff(col(2, 1, 0), col(3, 2, 1))
      bob.propose(TariffMatrixUpdating(zeroOkMatrix), state) shouldBeSuccess
    }

    "Tariff matrix should not be empty" - {
      val emptyMatrix = TariffMatrix(bob.account.address, Matrix.empty[BigDecimal])
      bob.propose(TariffMatrixUpdating(emptyMatrix), state) shouldBe ProcessingError.InvalidMatrix
    }

    "Tariff matrix should replace previous matrix if it has more or equal number of tariffs" - {
      val oldMatrix = tariff(col(3, 1), (col(2, 1)))
      val newMatrix = tariff(col(5, 4), (col(7, 3)))

      val stateWithTariff = state.copy(tariffMatrices = Map(bob.account.address -> oldMatrix))
      val result = bob.propose(TariffMatrixUpdating(newMatrix), stateWithTariff)
      result shouldBeSuccess

      val updEff = result.right.get.collect { case x: UpdateTariffMatrix => x }.map(_.tariffMatrix)
      assert(!updEff.isEmpty)
      assert(updEff.head == newMatrix)
      assert(updEff.head != oldMatrix)
    }

    "Tariff matrix should be merged with previous one if it has less number of tariffs" - {
      val oldMatrix = tariff(col(3, 1), (col(2, 1)))
      val newMatrix = tariff(col(5, 4))
      val mergedMatrix = tariff(col(5, 4), col(2, 1))
      val stateWithTariff = state.copy(tariffMatrices = Map(bob.account.address -> oldMatrix))
      val result = bob.propose(TariffMatrixUpdating(newMatrix), stateWithTariff)
      result shouldBeSuccess

      val updEff = result.right.get.collect { case x: UpdateTariffMatrix => x }.map(_.tariffMatrix)
      assert(!updEff.isEmpty)
      assert(updEff.head != newMatrix)
      assert(updEff.head != oldMatrix)
      assert(updEff.head == mergedMatrix)
    }

  }
}
