package pravda.vm.state

trait Storage {
  def get(key: Data): Option[Data]
  def put(key: Data, value: Data)
  def delete(key: Data): Unit
}
