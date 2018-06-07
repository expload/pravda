package pravda.vm.state

trait Memory {
  def stack: Seq[Data]
  def heap: Seq[Data]
  def limit(index: Int): Unit
  def dropLimit(): Unit
  def pop(): Data
  def push(x: Seq[Data]): Unit
  def push(x: Data): Unit
  def get(i: Int): Data
  def clear(): Unit
  def all: Seq[Data]
  def swap(i: Int, j: Int): Unit
  def length: Int
  def heapPut(x: Data): Int
  def heapGet(idx: Int): Data
  def heapLength: Int
  def top(): Data
  def top(n: Int): Seq[Data]
}
