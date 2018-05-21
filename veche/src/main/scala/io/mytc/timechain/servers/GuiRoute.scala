package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.mytc.keyvalue.DB
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.data.common.{Address, Mytc, TransactionId}
import io.mytc.timechain.persistence.FileStore
import io.mytc.timechain.servers.Abci.EnvironmentEffect
import io.mytc.timechain.{Config, utils}
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
import io.mytc.timechain.data.blockchain.Transaction.UnsignedTransaction
import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.cryptography
import io.mytc.timechain.data.cryptography.PrivateKey

import scala.concurrent.Future
import scala.util.Random

class GuiRoute(abciClient: AbciClient, db: DB)(implicit system: ActorSystem, materializer: ActorMaterializer) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  private def effectsTable(table: List[Map[String, Node]]): Node = {
    'table ('class /= "table is-striped is-hoverable is-fullwidth",
            'thead ('tr (table.head.keys.map(k => 'td (k)))),
            'tbody (
              table.map { el =>
                'tr (el.values.map(k => 'td (k)))
              }
            ))
  }

  private def showEffectName(effect: EnvironmentEffect) = effect match {
    case _: EnvironmentEffect.ProgramCreate => "Create program"
    case _: EnvironmentEffect.StorageRemove => "Remove from storage"
    case _: EnvironmentEffect.ProgramUpdate => "Update program"
    case _: EnvironmentEffect.StorageRead   => "Read from storage"
    case _: EnvironmentEffect.StorageWrite  => "Write to storage"
  }

  private def mono(s: ByteString): Node =
    'span ('class /= "hash", utils.bytes2hex(s))

  private def mono(s: Array[Byte]): Node =
    'span ('class /= "hash", utils.bytes2hex(s))

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
      case EnvironmentEffect.StorageWrite(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Written value" -> mono(value)
        )
      case EnvironmentEffect.StorageRead(key, value) =>
        Map(
          "Key" -> localKey(key),
          "Readen value" -> showOption(value)
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

  private def asmAstToAsm(asmAst: Seq[(Int, Op)]) = {
    asmAst.map { case (no, op) => "%06X:\t%s".format(no, op.toAsm) }.mkString("\n")
  }

  private def programToAsm(program: Array[Byte]) =
    asmAstToAsm(Assembler().decompile(program))

  private def programToAsm(program: ByteString) =
    asmAstToAsm(Assembler().decompile(program))

  private val codeArea = elementId()
  private val feeField = elementId()
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
            'div (
              'class  /= "columns",
              'margin @= "0",
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
                          'div ('class /= "columns",
                                'div ('class /= "column is-2", 'div ('class /= "title is-5", "From")),
                                'div ('class /= "column", mono(transaction.from))),
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
                                  if (state.currentEffectsGroup.contains(i)) {
                                    effectsTable(effects.map(effectToTableElement))
                                  } else void
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
        case state @ ErrorScreen(error) =>
          mainLayout(state, 'pre ('color @= "red", error))
        case state @ SendTransactionScreen(inProgress, maybeResult) =>
          mainLayout(
            state,
            'form (
              'display       @= "flex",
              'flexDirection @= "column",
              'textarea ('class  /= "textarea",
                         'margin @= 10,
                         'height @= 400,
                         codeArea,
                         'placeholder /= "Place your p-forth code here"),
              'input ('class          /= "input", 'margin @= 10, feeField, 'placeholder /= "Fee", 'value /= "0.00"),
              'input ('class          /= "input",
                      'margin         @= 10,
                      addressField,
                      'placeholder /= "Address",
                      'value       /= utils.bytes2hex(Config.timeChainConfig.paymentWallet.address)),
              'input (
                'class  /= "input",
                'margin @= 10,
                pkField,
                'placeholder /= "Private key",
                'type        /= "password",
                'value       /= utils.bytes2hex(Config.timeChainConfig.paymentWallet.privateKey)
              ),
              'div (
                'button (
                  'class  /= "button is-link",
                  'margin @= 10,
                  'width  @= 200,
                  "Send transaction",
                  if (inProgress) 'disabled /= "" else void,
                  event('click) {
                    access =>
                      val eventuallyErrorOrTransaction =
                        for {
                          code <- access.valueOf(codeArea)
                          fee <- access.valueOf(feeField)
                          address <- access.valueOf(addressField)
                          pk <- access.valueOf(pkField)
                        } yield {
                          val hackCode = code.replace("\\n", " ").replace("\\", "") // FIXME HACK CODE
                          io.mytc.sood.forth.Compiler().compile(hackCode) map { data =>
                            val unsignedTx = UnsignedTransaction(Address.fromHex(address),
                                                                 TransactionData @@ ByteString.copyFrom(data),
                                                                 Mytc.fromString(fee),
                                                                 Random.nextInt())
                            cryptography.signTransaction(PrivateKey.fromHex(pk), unsignedTx)
                          }
                        }
                      eventuallyErrorOrTransaction flatMap {
                        case Left(error) =>
                          access.transition(_ => SendTransactionScreen(inProgress = false, maybeResult = Some(error)))
                        case Right(tx) =>
                          for {
                            _ <- access.transition(_ => SendTransactionScreen(inProgress = true, maybeResult = None))
                            stack <- abciClient.broadcastTransaction(tx)
                            _ <- access.transition { _ =>
                              SendTransactionScreen(
                                inProgress = false,
                                maybeResult = Some(utils.showStack(stack))
                              )
                            }
                          } yield {
                            ()
                          }
                      }
                  }
                ),
                'button (
                  'class  /= "button is-link is-danger",
                  'margin @= 10,
                  'width  @= 200,
                  "Create program",
                  if (inProgress) 'disabled /= "" else void,
                  event('click) {
                    access =>
                      val eventuallyErrorOrTransaction =
                        for {
                          code <- access.valueOf(codeArea)
                          fee <- access.valueOf(feeField)
                          address <- access.valueOf(addressField)
                          pk <- access.valueOf(pkField)
                        } yield {
                          val compiler = io.mytc.sood.forth.Compiler()
                          val hackCode = code.replace("\\n", " ").replace("\\", "") // FIXME HACK CODE
                          compiler.compile(hackCode) flatMap { data =>
                            compiler.compile(s"$$x${utils.bytes2hex(data)} pcreate") map { data =>
                              val unsignedTx = UnsignedTransaction(Address.fromHex(address),
                                                                   TransactionData @@ ByteString.copyFrom(data),
                                                                   Mytc.fromString(fee),
                                                                   Random.nextInt())
                              cryptography.signTransaction(PrivateKey.fromHex(pk), unsignedTx)
                            }
                          }
                        }
                      eventuallyErrorOrTransaction flatMap {
                        case Left(error) =>
                          access.transition(_ => SendTransactionScreen(inProgress = false, maybeResult = Some(error)))
                        case Right(tx) =>
                          for {
                            _ <- access.transition(_ => SendTransactionScreen(inProgress = true, maybeResult = None))
                            stack <- abciClient.broadcastTransaction(tx)
                            _ <- access.transition { _ =>
                              SendTransactionScreen(
                                inProgress = false,
                                maybeResult = Some(utils.showStack(stack))
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
    val key = s"effects:${utils.padLong(height, 10)}"
    for {
      blockInfo <- OptionT(db.getAs[Map[TransactionId, Seq[EnvironmentEffect]]](key))
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
        'class /= "column",
        'div (
          'class /= "tabs",
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
