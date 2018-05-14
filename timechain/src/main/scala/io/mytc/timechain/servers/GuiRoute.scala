package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.mytc.timechain.clients.AbciClient
import io.mytc.timechain.data.blockchain.TransactionData
import io.mytc.timechain.data.common._
import io.mytc.timechain.data.domain.{Account, Deposit, PurchaseIntentionEvidence, TariffDescription}
import io.mytc.timechain.data.offchain.PurchaseIntention.SignedPurchaseIntention
import io.mytc.timechain.persistence._
import IntentionDTO.{Active, Confirmed, Expired, Punished}
import io.mytc.timechain.data.processing
import io.mytc.timechain.servers.Abci.ConsensusState
import io.mytc.timechain.utils
import io.mytc.timechain.utils.Var
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._

import scala.concurrent.Future


object IntentionDTO {
  sealed trait Status
  case object Confirmed extends Status
  case object Punished extends Status
  case object Active extends Status
  case object Expired extends Status
}
case class IntentionDTO (
  intention: SignedPurchaseIntention,
  status: IntentionDTO.Status,
  deposit: Deposit
)


class GuiRoute(consensusState: Var.VarReader[ConsensusState], abciClient: AbciClient)
              (implicit system: ActorSystem,
               materializer: ActorMaterializer,
               blockChainStore: BlockChainStore,
               nodeStore: NodeStore
              ) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  private def panel(caption: String, content: Node) = {
    'div(
      'border       @= "1px solid black",
      'marginBottom @= 10,
      'borderRadius @= 3,
      'padding      @= 10,
      'div(
        'fontWeight @= "600",
        'fontSize   @= 18,
        caption
      ),
      content
    )
  }

  private def verticalTable(xs: (String, Node)*) = {
    'table(
      'marginTop    @= 10,
      'marginBottom @= 10,
      'cellspacing  /= "5",
      'tbody(
        xs map {
          case (k, v) =>
            'tr('td('fontStyle @= "italic", k), 'td(v))
        }
      )
    )
  }

  private def refreshAccount() = {
    for {
      wallet <- FileStore.readPaymentWalletAsync()
      maybeAccount <- blockChainStore.getAccount(wallet.address)
    } yield {
      maybeAccount match {
        case None => Account(wallet.address, Mytc.zero, Mytc.zero)
        case Some(account) => account
      }
    }
  }

  private def refreshMultiplier() = {
    for {
      wallet <- FileStore.readPaymentWalletAsync()
      multiplier <- blockChainStore.getMultiplier(wallet.address)
    } yield {
      multiplier.getOrElse(BigDecimal(1.0))
    }
  }
  private def refreshOrganization() = {
    for {
      wallet <- FileStore.readPaymentWalletAsync()
      info <- FileStore.readMyOrganizationInfoAsync()
      kyc <- blockChainStore.getOrganization(wallet.address)
    } yield {
      GuiOrganization(
        info = info,
        kycPassed = kyc
          .flatMap(org => info.map(_.domain == org.domain))
          .getOrElse(false)
      )
    }
  }

  private def refreshTariffs() = {
    for {
      wallet <- FileStore.readPaymentWalletAsync()
      matrix <- blockChainStore.getMatrix(wallet.address).map(_.getOrElse(TariffMatrix(wallet.address)))
      descritption <- nodeStore.getTariffDescriptions
    } yield matrixToTariffs(matrix, descritption)
  }

  def getIntentionDto(intention: PurchaseIntentionEvidence, address: Address, currentBlock: Long, expiratoinTime: Long): Future[IntentionDTO] = {
    for {
      punished <- blockChainStore.isPunished(intention.address, intention.dataRef)
      confirmed <- blockChainStore.isConfirmed(intention.address, intention.dataRef)
      optDeposit <- blockChainStore.getDeposit(DepositId(vendor = address, owner = intention.address))
      deposit = optDeposit.get
      status = {
        if(punished) IntentionDTO.Punished
        else if (confirmed) IntentionDTO.Confirmed
        else if (deposit.block + expiratoinTime >= currentBlock) IntentionDTO.Expired
        else IntentionDTO.Active
      }
    } yield IntentionDTO(
      intention.intention,
      status,
      deposit
    )
  }

  private def refreshNotConfirmed(after: Long): Future[List[IntentionDTO]] = {
    for {
      wallet <- FileStore.readPaymentWalletAsync()
      consensus <- consensusState.get()
      intentions <- nodeStore.getIntentions
      currentBlock = consensus.processing.lastBlockHeight
      intentionDTO <- Future.sequence {
        intentions.map(i => getIntentionDto(i, wallet.address, currentBlock, processing.DepositWithdrawTimeout))
      }
    } yield intentionDTO.filter(_.deposit.block + after < currentBlock).filterNot(_.status == Confirmed)
  }

  private def makeKyc() = {
    FileStore.readPaymentWalletAsync() flatMap { wallet =>
      FileStore.readMyOrganizationInfoAsync() flatMap {
        case Some(info) =>
          abciClient.broadcastTransaction(
            from = wallet.address,
            privateKey = wallet.privateKey,
            data = TransactionData.ThisIsMe(info.domain),
            fee = Mytc(BigDecimal(0.1))
          )
        case None =>
          Future.unit
      }
    }
  }

  private def makeDeposit(address: Address, amount: Mytc) = {
    FileStore.readPaymentWalletAsync() flatMap { wallet =>
      abciClient.broadcastTransaction(
        from = wallet.address,
        privateKey = wallet.privateKey,
        data = TransactionData.DataPurchasingDeposit(address, amount),
        fee = Mytc(BigDecimal(0.1))
      )
    }
  }

  private def punish(intention: SignedPurchaseIntention) = {
    FileStore.readPaymentWalletAsync() flatMap { wallet =>
      abciClient.broadcastTransaction(
        from = wallet.address,
        privateKey = wallet.privateKey,
        data = TransactionData.CheatingCustomerPunishment(intention),
        fee = Mytc(BigDecimal(0.1))
      )
    }
  }

  private def updateTariffs(tariffs: List[Tariff]): Future[Unit] = {
    FileStore.readPaymentWalletAsync() flatMap { wallet =>
      val matrix = tariffsToMatrix(wallet.address, tariffs)
      abciClient.broadcastTransaction(
        from = wallet.address,
        privateKey = wallet.privateKey,
        data = TransactionData.TariffMatrixUpdating(matrix),
        fee = Mytc(BigDecimal(0.1))
      )
    }
  }

  private def setMultiplier(value: BigDecimal): Future[Unit] = {
    FileStore.readPaymentWalletAsync() flatMap { wallet =>
      abciClient.broadcastTransaction(
        from = wallet.address,
        privateKey = wallet.privateKey,
        data = TransactionData.MultiplierUpdating(value),
        fee = Mytc(BigDecimal(0.1))
      )
    }
  }

  private def refreshSettings(): Future[NodeSettings] = {
    FileStore.readNodeSettingsAsync().map(
      _.getOrElse(NodeSettings.default)
    )
  }
  private def saveSettings(settings: NodeSettings) = {
    FileStore.updateNodeSettingsAsync(settings)
  }

  private val inputDomain = elementId()

  private val inputLabel = elementId()

  private val inputPathToData = elementId()

  private val inputDepositAddress = elementId()

  private val inputDepositAmount = elementId()

  private val inputPunishmentTimeout = elementId()

  private val newTariffName = elementId()

  private val editedTariffName = elementId()

  private val multiplierValue = elementId()


  private val service = akkaHttpService(
    KorolevServiceConfig[Future, GuiState, Any](
      serverRouter = ServerRouter
        .empty[Future, GuiState]
        .withRootPath("/ui/"),
      stateStorage = StateStorage.forDeviceId { _ =>
        for {
          account <- refreshAccount()
          organization <- refreshOrganization()
          settings <- refreshSettings()
          intentions <- refreshNotConfirmed(settings.punishmentTimeout)
          tariffs <- refreshTariffs()
          multiplier <- refreshMultiplier()
          matrices <- blockChainStore.getMatrices
        } yield {
          GuiState(
            account, organization,
            editOrganization = false,
            tariffs = tariffs,
            editTariff = None,
            intentions = intentions,
            nodeSettings = settings,
            multiplier = multiplier,
            matrices = matrices.map(m => utils.bytes2hex(m.vendor) -> m).toMap
          )
        }
      },
      render = {
        case state => 'body(
          'fontFamily @= "sans-serif",
          'fontSize   @= 14,
          panel(
            caption = "Payment wallet",
            content = 'div(
              verticalTable(
                "Address" -> 'input(
                  'type /= "text",
                  'disabled /= "",
                  'value /= utils.bytes2hex(state.account.address)
                ),
                "Free"    -> state.account.free.toString,
                "Frozen"  -> state.account.frozen.toString
              ),
              'button(
                "Refresh",
                event('click) { access =>
                  refreshAccount() flatMap { account =>
                    access.transition(s => s.copy(account = account))
                  }
                }
              )
            )
          ),
          panel(
            caption = "Organization",
            content = state.organization.info match {
              case maybeInfo if state.editOrganization =>
                'form(
                  verticalTable(
                    "Domain"       -> 'input('type /= "text", inputDomain, maybeInfo.map(info => 'value /= info.domain)),
                    "Label"        -> 'input('type /= "text", inputLabel, maybeInfo.map(info => 'value /= info.label)),
                    "Path to data" -> 'input('type /= "text", inputPathToData, maybeInfo.flatMap(_.path.map(x => 'value /= x)))
                  ),
                  'button("Submit"),
                  event('submit) { access =>
                    for {
                      wallet <- FileStore.readPaymentWalletAsync()
                      domain <- access.valueOf(inputDomain)
                      label <- access.valueOf(inputLabel)
                      path <- access.valueOf(inputPathToData).map(_.trim).map(s => if (s.nonEmpty) Some(s) else None)
                      _ <- FileStore.updateMyOrganizationInfoAsync(OrganizationInfo(wallet.address, domain, label, path))
                      organization <- refreshOrganization()
                      _ <- access.transition(_.copy(editOrganization = false, organization = organization))
                    } yield ()
                  }
                )
              case Some(info) =>
                'div(
                  verticalTable(
                    "Domain"       -> info.domain,
                    "Label"        -> info.label,
                    "Path to data" -> (info.path.getOrElse("[empty]"): String)
                  ),
                  if (state.organization.kycPassed) {
                    'div(
                      'fontWeight    @= "600",
                      'color         @= "green",
                      'marginBottom  @= 5,
                      "KYC passed"
                    )
                  } else {
                    'button(
                      'marginBottom @= 5,
                      "Pass KYC",
                      event('click) { access =>
                        makeKyc() flatMap { _ =>
                          refreshOrganization() flatMap { org =>
                            access.transition(_.copy(organization = org))
                          }
                        }
                      }
                    )
                  },
                  'button(
                    'display @= "block",
                    "Edit organization",
                    event('click) { access =>
                      access.transition(_.copy(editOrganization = true))
                    }
                  )
                )
              case None =>
                'button(
                  'marginTop @= 10,
                  "Setup organization",
                  event('click) { access =>
                    access.transition(_.copy(editOrganization = true))
                  }
                )
            }
          ),
          state.editTariff match {
            case Some(tariff) => {
              panel(
                caption = s"${tariff.id}. ${tariff.name}",
                'div (
                  'div (
                    'margin @= 10,
                    'padding @= 5,
                    'span("Name: "),
                    'input ('type /= "text", 'value /= tariff.name, editedTariffName)
                  ),
                  'div (
                    'margin @= 10,
                    'table (
                      'thead(
                        'tr (
                          'th ("block"),
                          'th ("amount"),
                          'th ("")
                        )
                      ),
                      'tbody(
                        tariff.prices.zipWithIndex.map {
                          case (price, idx) =>
                            'tr (
                              'td (
                                math.pow(2, idx).toInt.toString
                              ),
                              'td (
                                'input (
                                  'type /= "number",
                                  'step /= "0.01",
                                  'value /= price.value.bigDecimal.toString,
                                  price.elId
                                )
                              ),
                              'td (
                                'button ("-"),
                                event('click) { access =>
                                  access.transition(_.copy( editTariff =
                                    state.editTariff.map {
                                      t => t.copy (prices = t.prices.filterNot(_ == price))
                                    }
                                  ))
                                }
                              )
                            )
                        }
                      )
                    ),
                    'button (
                      "+",
                      event('click) {
                        access =>
                          access.transition { s =>
                            s.copy(
                              editTariff = s.editTariff.map( t => t.copy(prices = t.prices :+ Price(Mytc.zero)))
                            )
                          }
                      }
                    )
                  ),
                  'div(
                    'button(
                      "Save",
                      event('click) {
                        access =>
                          for {
                            newName <- access.valueOf(editedTariffName)
                            prices <- Future.sequence(tariff.prices.map {
                              case price => access.valueOf(price.elId).map {
                                p => price.copy(value = BigDecimal(p))
                              }
                            })
                            editedTariff = tariff.copy(name = newName, prices = prices)
                            _ <- nodeStore.putTariffDescription(TariffDescription(editedTariff.id, editedTariff.name))
                            _ <- access.transition(s => s.copy(editTariff = None,
                              tariffs = s.tariffs.map {
                                case t if t.id == tariff.id => editedTariff
                                case t => t
                              }
                            ))
                          } yield ()
                      }
                    ),
                    'button(
                      "Cancel",
                      event('click) {
                        _.transition(_.copy(editTariff = None))
                      }
                    )
                  )
                )
              )
            }
            case None => {
              panel(
                caption = "Tariffs",
                content = 'div (
                  'div (
                    'margin @= 10,
                    'table (
                      'tbody(
                        state.tariffs.map {
                          tariff =>
                            'tr (
                              'td (
                                'margin @= 10,
                                s"${tariff.id}. ${tariff.name}"
                              ),
                              'td (
                                'margin @= 10,
                                'padding @= 5,
                                'button (
                                  "edit",
                                  event('click) { access =>
                                    access.transition(_.copy(editTariff = Some(tariff)))
                                  }
                                )
                              ),
                              'td (
                                if(tariff.inChain) {
                                  'span("")
                                } else {
                                  'button(
                                    "-",
                                    event('click) {
                                      access => access.transition(_.copy(
                                        tariffs = state.tariffs
                                          .filterNot(_.id == tariff.id)
                                            .map { t =>
                                              if(t.id > tariff.id) t.copy(id = t.id - 1)
                                              else t
                                            }
                                      ))
                                    }
                                  )
                                }
                              )
                            )
                        }
                      )
                    )
                  ),
                  'div (
                    'margin @= 10,
                    'input ('type /= "text", newTariffName),
                    'button (
                      "+",
                      event('click) { access =>
                        for {
                          tariffName <- access.valueOf(newTariffName)
                          _ <- access.property(newTariffName).set('value, "")
                          _ <- access.transition(s =>
                            s.copy(tariffs = s.tariffs :+ Tariff(s.tariffs.length + 1, tariffName))
                          )
                        } yield ()
                      }
                    )
                  ),
                  'div (
                    'button (
                      "Submit",
                      event('click) { access =>
                        updateTariffs(state.tariffs)
                      }
                    )
                  )
                )
              )
            }
          },
          panel(
            caption = "Multiplier",
            content = 'form(
              'margin @= 10,
              'padding @= 5,
              'input(
                'type /= "number",
                multiplierValue,
                'value /= state.multiplier.toString,
                'step /= "0.01"
              ),
              'button("Submit"),
              event('submit) { access =>
                for {
                  multiplier <- access.valueOf(multiplierValue).map(BigDecimal(_))
                  _ <- setMultiplier(multiplier)
                  _ <- access.transition(s => s.copy(multiplier = multiplier))
                }
                  yield ()
              }
            )
          ),
          panel(
            caption = "Deposit",
            content = 'form(
              verticalTable(
                "Address" -> 'input('type /= "text", inputDepositAddress),
                "Amount"  -> 'input('type /= "number", inputDepositAmount)
              ),
              'button("Submit"),
              event('submit) { access =>
                for {
                  address <- access.valueOf(inputDepositAddress)
                  amount  <- access.valueOf(inputDepositAmount)
                  _       <- makeDeposit(Address.fromHex(address), Mytc.fromString(amount))
                  account <- refreshAccount()
                  _       <- access.transition(s => s.copy(account = account))
                }
                yield ()
              }
            )
          ),
          panel(
            caption = "Node settings",
            content = 'div(
              'form(
                verticalTable(
                  "Punishment timeout" -> 'div(
                      'input('type /= "number", inputPunishmentTimeout, 'value /= state.nodeSettings.punishmentTimeout.toString),
                      if(state.nodeSettings.punishmentTimeout >= processing.DepositWithdrawTimeout) {
                        'div(
                          'color @= "red",
                          s"It should be less then deposit withdraw timeout (${processing.DepositWithdrawTimeout})"
                        )
                      } else {
                        'none ()
                      }
                  )
                ),
                'button("Save"),
                event('submit) {
                  access =>
                    for {
                      punishmentTimeout <- access.valueOf(inputPunishmentTimeout).map(_.toLong)
                      settings = NodeSettings(punishmentTimeout)
                      _ <- saveSettings(settings)
                      notConfirmedIntentions <- refreshNotConfirmed(punishmentTimeout)
                      _ <- access.transition(_.copy(nodeSettings=settings, intentions=notConfirmedIntentions))
                    }
                    yield ()
                }
              )
            )
          ),
          panel(
            caption = "Not confirmed intentions",
            content = 'table(
              'thead(
                'tr(
                  'th(
                    "Data"
                  ),
                  'th(
                    "Address"
                  ),
                  'th(
                    ""
                  )
                )
              ),
              'tbody(
                state.intentions.map(
                  intention =>
                  'tr(
                    'td(
                      utils.bytes2hex(intention.intention.data.dataRef)
                    ),
                    'td(
                      utils.bytes2hex(intention.intention.data.address)
                    ),
                    'td {
                      intention.status match {
                        case Punished =>
                          'span ("Punished", 'color @= "red")
                        case Active =>
                          'button (
                            "Punish",
                            event('click) { access =>
                              punish(intention.intention)
                            }
                          )
                        case Expired =>
                          'button (
                            "Punish",
                            'disabled /= "true"
                          )
                        case _ =>
                          'none ()
                      }
                    }
                  )
                )
              )
            )
          ),
          panel(
            caption = "Tariff matrices",
            content = 'div(
              'div(
                'select(
                  tariffVendor,
                  'padding @= 5, 'margin @= 10,
                  'option("Choose vendor address"),
                  state.matrices.keySet.map {
                    address =>
                    'option(address, 'value /= address)
                  },
                  event('change) { access =>
                    access.valueOf(tariffVendor).flatMap {
                      address => access.transition(_.copy(vendorMatrix = state.matrices.get(address)))
                    }
                  }
                )
              ),
              'div(
                state.vendorMatrix.map(
                  tariffMatrix => {
                    val matrix = tariffMatrix.matrix
                    'table(
                      'thead(
                        'tr(
                          'th(),
                          (1 to matrix.cols).map { id =>
                          'th(id.toString)
                          }
                        )
                      ),
                      'tbody(
                        (0 until matrix.rows) map { r =>
                          'tr(
                            'td('b(math.pow(2, r).toInt.toString)),
                            (0 until matrix.cols) map { c =>
                              'td(
                                'padding @= 15,
                                'margin @= 20,
                                matrix(r, c).toString
                              )
                            }
                          )
                        }
                      )
                    )
                  }
                ).getOrElse('span())
              )
            )
          )
        )
      }
    )
  )

  val tariffVendor = elementId()
  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  case class Price(value: BigDecimal = BigDecimal(0), elId: globalContext.ElementId = globalContext.elementId())
  case class Tariff(id: Int, name: String, prices: List[Price] = List(Price()), inChain: Boolean = false)
  def tariffsToMatrix(address: Address, tariffs: List[Tariff]): TariffMatrix = {
    val maxLen = tariffs.map(_.prices.length).max
    val data = tariffs.sortBy(_.id).flatMap { t =>
      t.prices ::: List.fill(maxLen - t.prices.length)(t.prices.last)
    }.map(_.value).toVector
    TariffMatrix(address, tariffs.length, maxLen, data)
  }
  def matrixToTariffs(matrix: TariffMatrix, descriptions: Map[Int, TariffDescription]): List[Tariff] = {
    (1 to matrix.tariffs).map { tid =>
      val tariff = matrix.tariff(tid)
      val last = tariff(matrix.records - 1)
      val tariffWithoutTail = tariff.takeWhile(_ > last) :+ last
      Tariff(tid, descriptions.get(tid).map(_.name).getOrElse(""),
        tariffWithoutTail.toList.map(Price(_)),
        true
      )
    }.toList
  }

  case class GuiState(
    account: Account,
    organization: GuiOrganization,
    editOrganization: Boolean,
    tariffs: List[Tariff],
    multiplier: BigDecimal,
    editTariff: Option[Tariff],
    intentions: List[IntentionDTO],
    nodeSettings: NodeSettings,
    matrices: Map[String, TariffMatrix],
    vendorMatrix: Option[TariffMatrix] = None
 )

  case class GuiOrganization(
    info: Option[OrganizationInfo],
    kycPassed: Boolean
  )

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
