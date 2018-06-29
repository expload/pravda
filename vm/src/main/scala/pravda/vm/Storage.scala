package pravda.vm

trait Storage {
  def get(key: Data): Option[Data]
  def put(key: Data, value: Data): Option[Data]
  def delete(key: Data): Option[Data]
}
