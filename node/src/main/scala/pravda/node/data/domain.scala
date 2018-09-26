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

package pravda.node.data

import pravda.common.domain.Address
import pravda.node.data.cryptography.EncryptedPrivateKey

object domain {

  final case class Wallet(
      address: Address,
      name: String,
      privateKey: EncryptedPrivateKey
  )

  final case class Account(
      address: Address,
      free: BigDecimal, // do not use mytc cause getquill bug :(
      frozen: BigDecimal
  )

}
