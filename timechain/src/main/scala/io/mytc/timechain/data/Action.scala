package io.mytc.timechain.data

import io.mytc.timechain.data.common.Address
import io.mytc.timechain.data.serialization.json._

sealed trait Action

object Action {

  case class ActionsFile(address: Address, actions: Seq[ActionsFileRecord])
  case class ActionsFileRecord(timestamp: Long, timezone: String, action: Action)

  // Bullshit actions
  case object SmokeVape extends Action
  case object DrinkSmoothie extends Action
  case object DriveGyroscooter extends Action
  case object WorkInCoworking extends Action
  case object LiveInColiving extends Action
  case object SayMonadIsAMonoidInTheCategoryOfEndofunctors extends Action
}