package pravda.evm.translate

import pravda.evm.EVM
import pravda.evm.translate.opcode.SimpleTranslation
import pravda.vm.asm

object Translator {


  private def listToEither[L,R](eithers: List[Either[L,List[R]]]): Either[List[L],List[R]] = {
     eithers.partition(_.isLeft) match {
       case (Nil,rights) => Right(
         for {Right(list) <- rights
              el <- list
         } yield el
       )
       case (lefts,_) => Left(for(Left(message) <- lefts)yield message)

     }
  }

  def apply(ops: List[EVM.Op]):Either[List[String], List[asm.Operation]]  = {
    listToEither(
      ops.map(SimpleTranslation.evmOpToOps)
    )
  }

}
