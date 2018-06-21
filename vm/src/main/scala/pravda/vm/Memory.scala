package pravda.vm

trait Memory {
  def stack: Seq[Data.Primitive]
  def heap: Seq[Data]
  def limit(index: Int): Unit
  def dropLimit(): Unit
  def pop(): Data.Primitive
  def push(x: Data.Primitive): Unit
  def get(i: Int): Data.Primitive
  def clear(): Unit
  def all: Seq[Data.Primitive]
  def swap(i: Int, j: Int): Unit
  def length: Int
  // TODO should return Data.Ref
  def heapPut(x: Data): Int
  // TODO should take Data.Ref
  def heapGet(idx: Int): Data
  def heapLength: Int
  def top(): Data.Primitive
  def top(n: Int): Seq[Data.Primitive]
}
