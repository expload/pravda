package pravda.node

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}
import pravda.common.domain
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.common.TransactionId
import pravda.node.data.serialization._
import pravda.node.data.serialization.json._
import pravda.node.servers.Abci.TransactionResult
import pravda.vm._

object TransactionResultJsonSpecification extends Properties("TransactionResultJson") {

  import pravda.vm.DataSpecification
  import DataSpecification.{byteString, bytes, data, ref, primitive}

  val address: Gen[domain.Address] =
    byteString.map(Address @@ _)

  val amount: Gen[domain.NativeCoin] =
    arbitrary[Long].map(NativeCoin @@ _)

  val marshalledData: Gen[MarshalledData] = {
    val hitem = for (d <- data; r <- ref) yield (r, d)
    val simple = DataSpecification.data.map(MarshalledData.Simple)
    val complex = for (d <- data; h <- Gen.listOf(hitem).map(_.toMap)) yield MarshalledData.Complex(d, h)
    Gen.oneOf(simple, complex)
  }

  val watts: Gen[Long] = arbitrary[Long]

  val effect: Gen[Effect] = {
    val storageRemove =
      for (p <- address; k <- data; v <- Gen.option(data))
        yield Effect.StorageRemove(p, k, v)
    val storageWrite =
      for (p <- address; k <- data; v <- Gen.option(data); v2 <- data)
        yield Effect.StorageWrite(p, k, v, v2)
    val storageRead =
      for (p <- address; k <- data; v <- Gen.option(data))
        yield Effect.StorageRead(p, k, v)
    val programCreate =
      for (p <- address; b <- bytes)
        yield Effect.ProgramCreate(p, b)
    val programSeal =
      for (p <- address)
        yield Effect.ProgramSeal(p)
    val programUpdate =
      for (p <- address; b <- bytes)
        yield Effect.ProgramUpdate(p, b)
    val transfer =
      for (f <- address; t <- address; a <- amount)
        yield Effect.Transfer(f, t, a)
    val showBalance =
      for (f <- address; a <- amount)
        yield Effect.ShowBalance(f, a)
    val event =
      for (f <- address; n <- Gen.alphaStr; d <- marshalledData)
        yield Effect.Event(f, n, d)

    Gen.oneOf(
      storageRemove,
      storageWrite,
      storageRead,
      programCreate,
      programSeal,
      programUpdate,
      transfer,
      showBalance,
      event
    )
  }

  val finalState: Gen[FinalState] =
    for (sw <- watts; tw <- watts; rw <- watts; stack <- Gen.listOf(primitive); heap <- Gen.listOf(data))
      yield FinalState(sw, rw, tw, stack, heap)

  val error: Gen[Error] = Gen.oneOf(
    Error.StackOverflow,
    Error.StackUnderflow,
    Error.WrongStackIndex,
    Error.WrongHeapIndex,
    Error.WrongType,
    Error.InvalidCoinAmount,
    Error.InvalidAddress,
    Error.OperationDenied,
    Error.PcallDenied,
    Error.NotEnoughMoney,
    Error.AmountShouldNotBeNegative,
    Error.ProgramIsSealed,
    Error.NoSuchProgram,
    Error.NoSuchMethod,
    Error.NoSuchElement,
    Error.OutOfWatts,
    Error.CallStackOverflow,
    Error.CallStackUnderflow,
    Error.ExtCallStackOverflow,
    Error.ExtCallStackUnderflow,
  )

  val runtimeException: Gen[RuntimeException] = for {
    e <- error
    fs <- finalState
    cs <- Gen.listOf {
      Gen.option(address).flatMap { address =>
        Gen.listOf(arbitrary[Int]).map(xs => (address, xs))
      }
    }
    lp <- arbitrary[Int]
  } yield RuntimeException(e, fs, cs, lp)

  val executionResult: Gen[ExecutionResult] =
    Gen.oneOf(runtimeException.map(Left(_)), finalState.map(Right(_)))

  val transactionId: Gen[TransactionId] =
    byteString.map(TransactionId @@ _)

  val transactionResult: Gen[TransactionResult] =
    for {
      id <- transactionId
      er <- executionResult
      es <- Gen.listOf(effect)
    } yield TransactionResult(id, er, es)

  property("TransactionResult/write->read") = forAll(transactionResult) { txr =>
    val json = transcode(txr).to[Json]
    transcode(json).to[TransactionResult] == txr
  }

  property("Error/write->read") = forAll(error) { error =>
    val json = transcode(error).to[Json]
    transcode(json).to[Error] == error
  }

}
