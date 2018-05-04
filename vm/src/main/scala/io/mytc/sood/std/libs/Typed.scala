package io.mytc.sood
package std.libs

import io.mytc.sood.std.Func
import io.mytc.sood.vm.serialization._
import io.mytc.sood.vm.state.{Data, Memory}

object Typed extends std.Lib {

  val Int32Tag: Byte = 1
  val Float64Tag: Byte = 2

  private def createFunc(name_ : String, f: Memory => Memory): std.Func =
    new Func {
      override val name: String = name_
      override def apply(m: Memory): Memory = f(m)
    }

  def dataToTyped(typeTag: Byte, data: Data): Data =
    typeTag +: data

  def typedTag(data: Data): Byte =
    data(0)

  val typedI32: std.Func = createFunc("typedI32", m => {
    m.copy(stack = m.stack.map(dataToTyped(Int32Tag, _)))
  })

  val typedR32: std.Func = createFunc("typedR64", m => {
    m.copy(stack = m.stack.map(dataToTyped(Float64Tag, _)))
  })

  private def createArithmeticFunc(name: String,
                                   ii2i: (Int, Int) => Int,
                                   ff2f: (Double, Double) => Double,
                                   fi2f: (Double, Int) => Double,
                                   if2f: (Int, Double) => Double): std.Func =
    createFunc(
      name,
      m => {
        val a = m.stack(0)
        val b = m.stack(1)
        m.stack.clear()

        val res = (typedTag(a), typedTag(b)) match {
          case (Int32Tag, Int32Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          ii2i(dataToInt32(a.drop(1)), dataToInt32(b.drop(1)))
                        ))
          case (Float64Tag, Float64Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          ff2f(dataToDouble(a.drop(1)), dataToDouble(b.drop(1)))
                        ))
          case (Int32Tag, Float64Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          if2f(dataToInt32(a.drop(1)), dataToDouble(b.drop(1)))
                        ))
          case (Float64Tag, Int32Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          fi2f(dataToDouble(a.drop(1)), dataToInt32(b.drop(1)))
                        ))
          case (_, _) => int32ToData(0) // FIXME
        }

        m.push(res)
        m
      }
    )

  val typedAdd: std.Func = createArithmeticFunc("typedAdd", _ + _, _ + _, _ + _, _ + _)
  val typedMul: std.Func = createArithmeticFunc("typedMul", _ * _, _ *_, _ * _, _ * _)
  val typedDiv: std.Func = createArithmeticFunc("typedDiv", _ / _, _ / _, _ / _, _ / _)
  val typedMod: std.Func = createArithmeticFunc("typedMod", _ % _, _ % _, _ % _, _ % _)

  override val address: String = "Typed"
  override val functions: Seq[std.Func] = Array(
    typedI32,
    typedR32,
    typedAdd,
    typedMul,
    typedDiv,
    typedMod
  )
}
