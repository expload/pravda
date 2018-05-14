package io.mytc.timechain.data

import io.mytc.timechain.data.{blockchain => blch}
import io.mytc.timechain.data.{cryptography => crypto}
import utest.assert

object testutils {

  import common.Mytc
  import domain.Account

  import blch.TransactionData
  import blch.Transaction.UnsignedTransaction

  import .ProcessingState
  import .ProcessingResult
  import .processTransaction

  import crypto.PrivateKey
  import crypto.generateKeyPair
  import crypto.signTransaction
  import crypto.checkTransactionSignature

  // Simple utils to manually indicate failure
  // assert(fail"oops")
  // assert(msg"oops" || dump(v))
  def dump(v: Any) = false
  implicit class FailureHelper(val sc: StringContext) extends AnyVal {
    def msg(args: Any*): Boolean = true
    def fail(args: Any*): Boolean = false
  }

  case class User(val account: Account, val pkey: PrivateKey) {
    // For the sake of simplicity of tests
    // let's do all the intermediate steps in here
    // and return ProcessingResult
    def propose(txdata: TransactionData, state: ProcessingState, fee: Mytc = Mytc.amount(0.5)): ProcessingResult = {
      val utx = UnsignedTransaction(account.address, txdata, fee)
      val stx = signTransaction(pkey, utx)
      checkTransactionSignature(stx).map(processTransaction(_, state)) match {
        case Some(pr) ⇒ pr
        case None ⇒ throw new RuntimeException("Transaction sign check failed. Something went very wrong.")
      }
    }
  }

  object User {
    def create(free: BigDecimal = 0, frozen: BigDecimal = 0) = {
      val (addr, pkey) = generateKeyPair()
      User(Account(address = addr, free = free, frozen = frozen), pkey)
    }
  }

  object transactions {

    implicit class TransactionValidator(result: Either[ProcessingError, List[ProcessingEffect]]) {
      def shouldBe(expected: ProcessingError): Unit = result match {
        case Left(was) => assert(was == expected)
        case Right(_) => assert(fail"It should be error here")
      }

      def shouldBe(expected: List[ProcessingEffect]): Unit = result match {
        case Left(error) => assert(fail"Should be success but found error $error")
        case Right(was) => assert(was == expected)
      }

      def shouldBeError = result match {
        case Left(_) => assert(true)
        case Right(_) => assert(fail"It should be error here")
      }

      def shouldBeSuccess = result match {
        case Left(error) => assert(fail"Should be success but found error $error")
        case Right(_) => assert(true)
      }

    }

  }

}
