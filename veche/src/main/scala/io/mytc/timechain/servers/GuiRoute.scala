package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.mytc.keyvalue.DB
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

import scala.concurrent.Future

class GuiRoute(db: DB)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  def effectsTable(table: List[Map[String, Node]]): Node = {
    'table('class /= "table",
      'thead('tr(table.head.keys.map(k => 'td(k)))),
      'tbody(
        table.map { el =>
          'tr(el.values.map(k => 'td(k)))
        }
      )
    )
  }

  def showEffectName(effect: EnvironmentEffect) = effect match {
    case _: EnvironmentEffect.ProgramCreate => "Create program"
    case _: EnvironmentEffect.StorageRemove => "Remove from storage"
    case _: EnvironmentEffect.ProgramUpdate => "Update program"
    case _: EnvironmentEffect.StorageRead => "Read from storage"
    case _: EnvironmentEffect.StorageWrite => "Write to storage"
  }

  def effectToTableElement(effect: EnvironmentEffect): Map[String, Node] = {
    def localKey(key: String) = key.substring(key.lastIndexOf(':') + 1)
    def showOption(value: Option[Array[Byte]]): Node = value match {
      case None => 'span('fontStyle @= "italic", "None")
      case Some(s) => utils.bytes2hex(s)
    }
    effect match {
      case EnvironmentEffect.ProgramCreate(address, program) =>
        Map(
          "Generated address" -> utils.bytes2hex(address),
          "Code" -> utils.bytes2hex(program)
        )
      case EnvironmentEffect.ProgramUpdate(address, program) =>
        Map(
          "Program address" -> utils.bytes2hex(address),
          "Code" -> utils.bytes2hex(program)
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

  def groupEffects(effects: List[EnvironmentEffect]) = effects
    .reverse
    .foldLeft(List.empty[(String, List[EnvironmentEffect])]) {
      case (Nil, effect) => (showEffectName(effect), effect :: Nil) :: Nil
      case ((acc @ ((lastName, lastGroup) :: xs)), effect) =>
        val thisName = showEffectName(effect)
        if (thisName == lastName) (lastName, effect :: lastGroup) :: xs
        else (thisName, effect :: Nil) :: acc
    }

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
                for {
                  maybeHeight <- FileStore.readApplicationStateInfoAsync()
                  height = maybeHeight.map(_.blockHeight - 1).getOrElse(0L)
                  maybeBlockInfo <- db.getAs[Map[TransactionId, Seq[EnvironmentEffect]]](s"effects:${utils.padLong(height, 10)}")
                } yield {
                  access.maybeTransition {
                    case MainScreen =>
                      BlockExplorer(
                        currentTransactionId = None,
                        currentEffectsGroup = None,
                        currentBlock = Block(
                          height, maybeBlockInfo.get.toList map {
                            case (txId, effects) =>
                              Transaction(txId, Address.fromHex("0000000000"), effects.toList, "not ready")
                          }
                        )
                      )
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

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
