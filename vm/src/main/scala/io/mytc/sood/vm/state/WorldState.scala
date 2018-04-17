package io.mytc.sood.vm.state

trait WorldState {
  def get(address: Address): AccountState
}
