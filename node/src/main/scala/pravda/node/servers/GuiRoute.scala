/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.node.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import cats.data.OptionT
import cats.implicits._
import com.google.protobuf.ByteString
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._
import pravda.common.domain.{Address, NativeCoin}
import pravda.common.{bytes => byteUtils}
import pravda.node.clients.AbciClient
import pravda.node.data.blockchain.Transaction.UnsignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.TransactionId
import pravda.node.data.cryptography
import pravda.node.data.cryptography.PrivateKey
import pravda.node.data.serialization._
import pravda.node.data.serialization.bson._
import pravda.node.db.DB
import pravda.node.persistence.FileStore
import pravda.node.servers.Abci.EnvironmentEffect
import pravda.node.utils
import pravda.vm.asm.{Operation, PravdaAssembler}

import scala.concurrent.Future
import scala.util.Random

class GuiRoute(abciClient: AbciClient, db: DB)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  private def effectsTable(table: List[Map[String, Node]]): Node = {
    'table (
      'class /= "table is-striped is-hoverable is-fullwidth is-narrow",
      'thead ('tr (table.head.keys.map(k => 'td (k)))),
      'tbody (
        table.map { el =>
          'tr (el.values.map(k => 'td (k)))
        }
      )
    )
  }

  private def showEffectName(effect: EnvironmentEffect) = effect match {
    case _: EnvironmentEffect.ProgramCreate => "Create program"
    case _: EnvironmentEffect.StorageRemove => "Remove from storage"
    case _: EnvironmentEffect.ProgramUpdate => "Update program"
    case _: EnvironmentEffect.StorageRead   => "Read from storage"
    case _: EnvironmentEffect.StorageWrite  => "Write to storage"
    case _: EnvironmentEffect.Withdraw      => "Withdraw NC"
    case _: EnvironmentEffect.Accrue        => "Put NC"
    case _: EnvironmentEffect.ShowBalance   => "Read NC balance"
  }

  private def mono(s: ByteString): Node =
    'span ('class /= "hash", byteUtils.byteString2hex(s))

  private def mono(s: Array[Byte]): Node =
    'span ('class /= "hash", byteUtils.bytes2hex(s))

  private def effectToTableElement(effect: EnvironmentEffect): Map[String, Node] = {
    def localKey(key: String) = key.substring(key.lastIndexOf(':') + 1)
    def showOption(value: Option[Array[Byte]]): Node = value match {
      case None    => 'span ('fontStyle @= "italic", "None")
      case Some(s) => mono(s)
    }
    effect match {
      case EnvironmentEffect.ProgramCreate(address, program) =>
        Map(
          "Generated address" -> mono(address),
          "Disassembled code" -> 'pre ('class /= "code", programToAsm(program))
        )
      case EnvironmentEffect.ProgramUpdate(address, program) =>
        Map(
          "Program address" -> mono(address),
          "Disassembled code" -> 'pre ('class /= "code", programToAsm(program))
        )
      case EnvironmentEffect.StorageRemove(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Removed value" -> showOption(value)
        )
      case EnvironmentEffect.StorageWrite(key, prev, value) =>
        Map(
          "Key" -> localKey(key),
          "Written value" -> mono(value),
          "Previous value" -> showOption(prev)
        )
      case EnvironmentEffect.StorageRead(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Readen value" -> showOption(value)
        )
      case EnvironmentEffect.Withdraw(from, amount) =>
        Map(
          "From" -> mono(from),
          "Amount" -> 'span (amount.toString)
        )
      case EnvironmentEffect.Accrue(to, amount) =>
        Map(
          "To" -> mono(to),
          "Amount" -> 'span (amount.toString)
        )
      case EnvironmentEffect.ShowBalance(address, amount) =>
        Map(
          "Address" -> mono(address),
          "Amount" -> 'span (amount.toString)
        )

    }
  }

  private def groupEffects(effects: List[EnvironmentEffect]) =
    effects.reverse
      .foldLeft(List.empty[(String, List[EnvironmentEffect])]) {
        case (Nil, effect) => (showEffectName(effect), effect :: Nil) :: Nil
        case ((acc @ ((lastName, lastGroup) :: xs)), effect) =>
          val thisName = showEffectName(effect)
          if (thisName == lastName) (lastName, effect :: lastGroup) :: xs
          else (thisName, effect :: Nil) :: acc
      }

  private def asmAstToAsm(operations: Seq[Operation]) = {
    PravdaAssembler.render(operations)
  }

  private def programToAsm(program: Array[Byte]): String =
    programToAsm(ByteString.copyFrom(program))

  private def programToAsm(program: ByteString): String =
    asmAstToAsm(PravdaAssembler.disassemble(program))

  private val codeArea = elementId()
  private val wattLimitField = elementId()
  private val wattPriceField = elementId()
  private val addressField = elementId()
  private val pkField = elementId()

  private val service = akkaHttpService(
    KorolevServiceConfig[Future, GuiState, Any](
      serverRouter = ServerRouter
        .empty[Future, GuiState]
        .withRootPath("/ui/"),
      stateStorage = StateStorage.default(SendTransactionScreen(false, None)),
      head = Seq(
        'link ('href /= "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css", 'rel /= "stylesheet"),
        'link (
          'href        /= "https://use.fontawesome.com/releases/v5.0.13/css/all.css",
          'rel         /= "stylesheet",
          'crossorigin /= "anonymous",
          'integrity   /= "sha384-DNOHZ68U8hZfKXOrtjWvjxusGo9WQnrNx2sqG0tfsghAvtVlRW3tvkXWZh58N9jp"
        ),
        'link ('href /= "main.css", 'rel /= "stylesheet")
      ),
      render = {
        case state: BlockExplorer =>
          val block = state.currentBlock
          mainLayout(
            state,
            'section (
              'class /= "section",
              'div (
                'class /= "columns",
                // Block number
                'div (
                  'class /= "column is-narrow has-text-centered",
                  'div ('class /= "is-size-3", block.height.toString)
                ),
                // Block content
                'div (
                  'class /= "column",
                  block.transactions.map {
                    transaction =>
                      val groupedEffects = groupEffects(transaction.effects)
                      'div (
                        'class /= "card",
                        'header (
                          'class /= "card-header",
                          'p (
                            'class /= "card-header-title",
                            'style /= "overflow-x:auto;",
                            'span ('class /= "icon has-text-black", 'i ('class /= "fas fa-stream")),
                            mono(transaction.id),
                            event('click) { access =>
                              access.maybeTransition {
                                case s: BlockExplorer =>
                                  s.copy(currentTransactionId = Some(transaction.id), currentEffectsGroup = None)
                              }
                            }
                          )
                        ),
                        if (state.currentTransactionId.contains(transaction.id)) {
                          'div (
                            'class /= "card-content",
                            'div (
                              'class /= "columns",
                              'div ('class /= "column is-2", 'div ('class /= "title is-5", "From")),
                              'div ('class /= "column", 'style /= "overflow-x:auto;", mono(transaction.from))
                            ),
                            'div (
                              'class /= "columns",
                              'div ('class /= "column is-2", 'div ('class /= "title is-5", "Disassembled code")),
                              'div ('class /= "column", 'pre ('class      /= "code", transaction.disassembledProgram))
                            ),
                            if (groupedEffects.nonEmpty) 'div ('class /= "title is-5", "World state effects") else void,
                            'ul (
                              'class /= "columns is-multiline",
                              groupedEffects.zipWithIndex map {
                                case ((name, effects), i) =>
                                  'li (
                                    'class /= "column is-12",
                                    'div (
                                      'class /= "title is-6",
                                      s"${i + 1}. $name",
                                      'span ('marginLeft @= 5, 'class /= "tag is-blue", effects.length.toString),
                                      event('click) { access =>
                                        access.maybeTransition {
                                          case s: BlockExplorer =>
                                            s.copy(currentEffectsGroup = Some(i))
                                        }
                                      }
                                    ),
                                    'div (
                                      'style /= "overflow-x:auto;",
                                      if (state.currentEffectsGroup.contains(i)) {
                                        effectsTable(effects.map(effectToTableElement))
                                      } else void
                                    )
                                  )
                              }
                            )
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
        case state @ ErrorScreen(error) =>
          mainLayout(state, 'pre ('color @= "red", error))
        case state @ SendTransactionScreen(inProgress, maybeResult) =>
          mainLayout(
            state,
            'form (
              'class /= "section",
              'div (
                'class /= "field",
                'label ('class /= "label", "Pravda-asm code"),
                'div (
                  'class /= "control",
                  'textarea ('class  /= "textarea",
                             'height @= 400,
                             codeArea,
                             'placeholder /= "Place your pravda-asm code here")
                )
              ),
              'div (
                'class /= "field",
                'label ('class /= "label", "Watt limit"),
                'div (
                  'class /= "control",
                  'input (
                    'class /= "input",
                    wattLimitField,
                    'placeholder /= "Watt limit",
                    'type        /= "number",
                    'min         /= "0",
                    'value       /= "300"
                  )
                )
              ),
              'div (
                'class /= "field",
                'label ('class /= "label", "Watt price"),
                'div (
                  'class /= "control",
                  'input (
                    'class /= "input",
                    wattPriceField,
                    'placeholder /= "Watt price",
                    'type        /= "number",
                    'min         /= "0",
                    'value       /= "1"
                  )
                )
              ),
              'div (
                'class /= "field",
                'label ('class /= "label", "Address"),
                'div (
                  'class /= "control",
                  'input ('class /= "input", addressField, 'placeholder /= "Address", 'value /= "") //byteUtils.byteString2hex(Config.pravdaConfig.validator.address)),
                )
              ),
              'div (
                'class /= "field",
                'label ('class /= "label", "Private key"),
                'div (
                  'class /= "control",
                  'input (
                    'class /= "input",
                    pkField,
                    'placeholder /= "Private key",
                    'type        /= "password",
                    'value       /= "" //byteUtils.byteString2hex(Config.pravdaConfig.validator.privateKey)
                  )
                )
              ),
              'div (
                'class /= "field buttons",
                'button (
                  'class /= "button is-link",
                  "Send transaction",
                  if (inProgress) 'disabled /= "" else void,
                  event('click) {
                    access =>
                      val eventuallyErrorOrTransaction =
                        for {
                          code <- access.valueOf(codeArea)
                          wattLimit <- access.valueOf(wattLimitField)
                          wattPrice <- access.valueOf(wattPriceField)
                          address <- access.valueOf(addressField)
                          pk <- access.valueOf(pkField)
                        } yield {
                          val hackCode = code
                            .replace("\\t", "\t")
                            .replace("\\r", "\r")
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\") // FIXME HACK CODE
                          PravdaAssembler.assemble(hackCode, saveLabels = true) map { data =>
                            val unsignedTx = UnsignedTransaction(Address.fromHex(address),
                                                                 TransactionData @@ data,
                                                                 wattLimit.toLong,
                                                                 NativeCoin.amount(wattPrice),
                                                                 Random.nextInt())
                            cryptography.signTransaction(PrivateKey.fromHex(pk), unsignedTx)
                          }
                        }
                      eventuallyErrorOrTransaction flatMap {
                        case Left(error) =>
                          access.transition(_ =>
                            SendTransactionScreen(inProgress = false, maybeResult = Some(error.mkString)))
                        case Right(tx) =>
                          for {
                            _ <- access.transition(_ => SendTransactionScreen(inProgress = true, maybeResult = None))
                            result <- abciClient.broadcastTransaction(tx)
                            _ <- access.transition { _ =>
                              SendTransactionScreen(
                                inProgress = false,
                                maybeResult = Some(result.fold(_.toString, utils.showExecInfo))
                              )
                            }
                          } yield {
                            ()
                          }
                      }
                  }
                ),
                'button (
                  'class /= "button is-link is-danger",
                  "Create program",
                  if (inProgress) 'disabled /= "" else void,
                  event('click) {
                    access =>
                      val eventuallyErrorOrTransaction =
                        for {
                          code <- access.valueOf(codeArea)
                          wattLimit <- access.valueOf(wattLimitField)
                          wattPrice <- access.valueOf(wattPriceField)
                          address <- access.valueOf(addressField)
                          pk <- access.valueOf(pkField)
                        } yield {
                          val hackCode = code
                            .replace("\\t", "\t")
                            .replace("\\r", "\r")
                            .replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\") // FIXME HACK CODE
                          PravdaAssembler.assemble(hackCode, saveLabels = true) flatMap { data =>
                            PravdaAssembler.assemble(s"push x${byteUtils.byteString2hex(data)} pcreate",
                                                     saveLabels = false) map { data =>
                              val unsignedTx = UnsignedTransaction(Address.fromHex(address),
                                                                   TransactionData @@ data,
                                                                   wattLimit.toLong,
                                                                   NativeCoin.amount(wattPrice),
                                                                   Random.nextInt())
                              cryptography.signTransaction(PrivateKey.fromHex(pk), unsignedTx)
                            }
                          }
                        }
                      eventuallyErrorOrTransaction flatMap {
                        case Left(error) =>
                          access.transition(_ =>
                            SendTransactionScreen(inProgress = false, maybeResult = Some(error.mkString)))
                        case Right(tx) =>
                          for {
                            _ <- access.transition(_ => SendTransactionScreen(inProgress = true, maybeResult = None))
                            execInfo <- abciClient.broadcastTransaction(tx)
                            _ <- access.transition { _ =>
                              SendTransactionScreen(
                                inProgress = false,
                                maybeResult = Some(execInfo.fold(_.toString, utils.showExecInfo))
                              )
                            }
                          } yield {
                            ()
                          }
                      }
                  }
                )
              ),
              maybeResult.map(x => 'pre (x))
            )
          )
      }
    )
  )

  private def loadBlock(height: Long) = {
    val key = s"effects:${byteUtils.bytes2hex(byteUtils.longToBytes(height))}"
    for {
      blockInfo <- OptionT(db.get(byteUtils.stringToBytes(key))).map(r =>
        transcode(Bson @@ r.bytes).to[Map[TransactionId, Seq[EnvironmentEffect]]])
      eventuallyTransaction = blockInfo.keys.map(tid => abciClient.readTransaction(tid).map(tx => tid -> tx))
      transactions <- OptionT.liftF(Future.sequence(eventuallyTransaction))
    } yield {
      Block(
        height,
        transactions.toList.map {
          case (id, transaction) =>
            val asm = programToAsm(transaction.program)
            Transaction(id, transaction.from, blockInfo(id).toList, asm)
        }
      )
    }
  }

  private def mainLayout(state: GuiState, content: Node*) = {
    'body (
      'div (
        'class /= "container is-fullhd",
        'div (
          'class /= "tabs is-medium",
          'ul (
            'li (
              if (state.isInstanceOf[BlockExplorer]) 'class /= "is-active" else void,
              'a ("Block Explorer"),
              event('click) { access =>
                val eventuallyTransactions =
                  OptionT(FileStore.readApplicationStateInfoAsync()).flatMap { asi =>
                    val height = asi.blockHeight - 1
                    loadBlock(height)
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
                      case _ => newScreen
                    }
                  }
              }
            ),
            'li (
              if (state.isInstanceOf[SendTransactionScreen]) 'class /= "is-active" else void,
              'a ("Transacton Composer"),
              event('click) { access =>
                access.transition(_ => SendTransactionScreen(inProgress = false, maybeResult = None))
              }
            )
          )
        )
      ),
      content
    )
  }

  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  final case class Transaction(id: TransactionId,
                               from: Address,
                               effects: List[EnvironmentEffect],
                               disassembledProgram: String)

  final case class Block(height: Long, transactions: List[Transaction])

  sealed trait GuiState

  final case class BlockExplorer(currentBlock: Block,
                                 currentTransactionId: Option[TransactionId],
                                 currentEffectsGroup: Option[Int] = None)
      extends GuiState

  final case class SendTransactionScreen(inProgress: Boolean, maybeResult: Option[String]) extends GuiState

  final case class ErrorScreen(error: String) extends GuiState

  object ErrorScreen {

    def apply(e: Throwable): ErrorScreen =
      ErrorScreen(s"${e.getMessage}:\n  ${e.getStackTrace.mkString("\n  ")}")
  }

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
