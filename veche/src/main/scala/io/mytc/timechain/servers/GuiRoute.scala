package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.mytc.keyvalue.DB
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.data.common.{Address, TransactionId}
import io.mytc.timechain.persistence.FileStore
import io.mytc.timechain.servers.Abci.EnvironmentEffect
import io.mytc.timechain.utils
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._
import io.mytc.timechain.persistence.implicits._
import cats.data.OptionT
import cats.implicits._
import com.google.protobuf.ByteString
import io.mytc.sood.asm.{Assembler, Op}

import scala.concurrent.Future

class GuiRoute(abciClient: AbciClient, db: DB)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  private def effectsTable(table: List[Map[String, Node]]): Node = {
    'table('class /= "table",
      'thead('tr(table.head.keys.map(k => 'td(k)))),
      'tbody(
        table.map { el =>
          'tr(el.values.map(k => 'td(k)))
        }
      )
    )
  }

  private def showEffectName(effect: EnvironmentEffect) = effect match {
    case _: EnvironmentEffect.ProgramCreate => "Create program"
    case _: EnvironmentEffect.StorageRemove => "Remove from storage"
    case _: EnvironmentEffect.ProgramUpdate => "Update program"
    case _: EnvironmentEffect.StorageRead => "Read from storage"
    case _: EnvironmentEffect.StorageWrite => "Write to storage"
  }

  private def effectToTableElement(effect: EnvironmentEffect): Map[String, Node] = {
    def localKey(key: String) = key.substring(key.lastIndexOf(':') + 1)
    def showOption(value: Option[Array[Byte]]): Node = value match {
      case None => 'span('fontStyle @= "italic", "None")
      case Some(s) => utils.bytes2hex(s)
    }
    effect match {
      case EnvironmentEffect.ProgramCreate(address, program) =>
        Map(
          "Generated address" -> utils.bytes2hex(address),
          "Disassembled code" -> 'pre(programToAsm(program))
        )
      case EnvironmentEffect.ProgramUpdate(address, program) =>
        Map(
          "Program address" -> utils.bytes2hex(address),
          "Disassembled code" -> 'pre(programToAsm(program))
        )
      case EnvironmentEffect.StorageRemove(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Removed value" -> showOption(value)
        )
      case EnvironmentEffect.StorageWrite(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Written value" -> utils.bytes2hex(value)
        )
      case EnvironmentEffect.StorageRead(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Readen value" -> showOption(value)
        )
    }
  }

  private def groupEffects(effects: List[EnvironmentEffect]) = effects
    .reverse
    .foldLeft(List.empty[(String, List[EnvironmentEffect])]) {
      case (Nil, effect) => (showEffectName(effect), effect :: Nil) :: Nil
      case ((acc @ ((lastName, lastGroup) :: xs)), effect) =>
        val thisName = showEffectName(effect)
        if (thisName == lastName) (lastName, effect :: lastGroup) :: xs
        else (thisName, effect :: Nil) :: acc
    }

  private def asmAstToAsm(asmAst: Seq[(Int, Op)]) = {
    asmAst.map{ case (no, op) => "%06X:\t%s".format(no, op.toAsm) }.mkString("\n")
  }

  private def programToAsm(program: Array[Byte]) =
    asmAstToAsm(Assembler().decompile(program))

  private def programToAsm(program: ByteString) =
    asmAstToAsm(Assembler().decompile(program))

  private val service = akkaHttpService(
    KorolevServiceConfig[Future, GuiState, Any](
      serverRouter = ServerRouter
        .empty[Future, GuiState]
        .withRootPath("/ui/"),
      stateStorage = StateStorage.default(MainScreen),
//      stateStorage = StateStorage.default(BlockExplorer(
//        currentBlock = Block(
//          height = 42,
//          transactions = List(
//            Transaction(
//              id = TransactionId.forEncodedTransaction(ByteString.copyFromUtf8("dadasdasd")),
//              from = Address.fromHex("fab124939fe200de19"),
//              effects = List(),
//              disassembledProgram = "lol"
//            ),
//            Transaction(
//              id = TransactionId.forEncodedTransaction(ByteString.copyFromUtf8("vsdfds")),
//              from = Address.fromHex("fab124939fe200de1a"),
//              effects = List(
//                EnvironmentEffect.StorageRead("xx:vasja", Some(Array(0x10))),
//                EnvironmentEffect.StorageRead("xx:vova", Some(Array(0x10))),
//                EnvironmentEffect.StorageRead("xx:vanya", Some(Array(0x10))),
//                EnvironmentEffect.StorageWrite("xx:abram", Array(0x30)),
//                EnvironmentEffect.StorageWrite("xx:vasja", Array(0)),
//                EnvironmentEffect.StorageWrite("xx:vova", Array(0)),
//                EnvironmentEffect.StorageWrite("xx:vanya", Array(0)),
//                EnvironmentEffect.StorageRead("xx:izya", Some(Array(0x50))),
//              ),
//              disassembledProgram = "zog"
//            ),
//          )
//        ),
//        currentTransactionId = TransactionId.forEncodedTransaction(ByteString.copyFromUtf8("dadasdasd"))
//      )),
      head = Seq(
        'link('href /= "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css", 'rel /= "stylesheet")
      ),
      render = {
        case MainScreen =>
          'body(
            'button(
              "Show last block",
              event('click) { access =>
                val eventuallyTransactions =
                  for {
                    asi <- OptionT(FileStore.readApplicationStateInfoAsync())
                    height = asi.blockHeight - 1
                    key = s"effects:${utils.padLong(height, 10)}"
                    blockInfo <- OptionT(db.getAs[Map[TransactionId, Seq[EnvironmentEffect]]](key))
                    eventuallyTransaction = blockInfo.keys.map(tid => abciClient.readTransaction(tid).map(tx => tid -> tx))
                    transactions <- OptionT.liftF(Future.sequence(eventuallyTransaction))
                  } yield {
                    Block(
                      height, transactions.toList.map {
                        case (id, transaction) =>
                          val asm = programToAsm(transaction.program)
                          Transaction(id, transaction.from, blockInfo(id).toList, asm)
                      }
                    )
                  }
                val eventuallyNewScreen = eventuallyTransactions.value.map {
                  case None =>
                    ErrorScreen("Inconsistent state: Identifier of transaction " +
                      "mentioned in state effects is not found on blockchain.")
                  case Some(block) =>
                    BlockExplorer(
                      currentTransactionId = None,
                      currentEffectsGroup = None,
                      currentBlock = block
                    )
                }
                eventuallyNewScreen
                  .recover {
                    case e: Throwable => ErrorScreen(e)
                  }
                  .flatMap { newScreen =>
                    access.maybeTransition {
                      case MainScreen => newScreen
                    }
                  }
              }
            )
          )
        case state: BlockExplorer =>
          val block = state.currentBlock
          'body(
            'div('class /= "columns",
              // Block number
              'div('class /= "column is-2", block.height.toString),
              // Block content
              'div('class /= "column",
                'ul(
                  block.transactions.map { transaction =>
                    val groupedEffects = groupEffects(transaction.effects)
                    'li(
                      'span(
                        s"0x${utils.bytes2hex(transaction.id)}",
                        event('click) { access =>
                          access.maybeTransition {
                            case s: BlockExplorer =>
                              s.copy(currentTransactionId = Some(transaction.id), currentEffectsGroup = None)
                          }
                        }
                      ),
                      if (state.currentTransactionId.contains(transaction.id)) {
                        'div(
                          'pre(transaction.disassembledProgram),
                          'div(s"From: 0x${utils.bytes2hex(transaction.from)}"),
                          groupedEffects.zipWithIndex map {
                            case ((name, effects), i) =>
                              'li(
                                'b(
                                  s"$i. $name (${effects.length})",
                                  event('click) { access =>
                                    access.maybeTransition {
                                      case s: BlockExplorer =>
                                        s.copy(currentEffectsGroup = Some(i))
                                    }
                                  }
                                ),
                                if (state.currentEffectsGroup.contains(i)) {
                                  effectsTable(effects.map(effectToTableElement))
                                } else void
                              )
                          }
                        )
                      } else {
                        void
                      }
                    )
                  }
                )
              )
            )
          )
        case ErrorScreen(error) =>
          'body('pre('color @= "red", error))
      }
    )
  )

  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  final case class Transaction(id: TransactionId,
                               from: Address,
                               effects: List[EnvironmentEffect],
                               disassembledProgram: String)

  final case class Block(height: Long,
                         transactions: List[Transaction])

  sealed trait GuiState

  final case class BlockExplorer(currentBlock: Block,
                                 currentTransactionId: Option[TransactionId],
                                 currentEffectsGroup: Option[Int] = None) extends GuiState

  final case object MainScreen extends GuiState

  final case class ErrorScreen(error: String) extends GuiState

  object ErrorScreen {
    def apply(e: Throwable): ErrorScreen =
      ErrorScreen(s"${e.getMessage}:\n  ${e.getStackTrace.mkString("\n  ")}")
  }

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
