package pravda.vm.state

trait Storage {
  def get(key: Address): Option[Data]
  def put(key: Address, value: Data)
  def delete(key: Address): Unit
}
