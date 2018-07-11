package pravda.dotnet.translation

import pravda.vm.{Data, Opcodes, asm}

package object opcode {

  def push[T](value: T, toData: T => Data): asm.Operation =
    asm.Operation.Push(toData(value))

  def pushInt(i: Int): asm.Operation =
    push(i, Data.Primitive.Int32)

  def pushFloat(d: Double): asm.Operation =
    push(d, Data.Primitive.Number)

  def pushString(s: String): asm.Operation =
    push(s, Data.Primitive.Utf8)

  def pushType(tpe: Data.Type): asm.Operation =
    push(tpe, Data.Primitive.Int8)

  def cast(tpe: Data.Type): List[asm.Operation] =
    List(pushType(tpe), asm.Operation(Opcodes.CAST))
}
