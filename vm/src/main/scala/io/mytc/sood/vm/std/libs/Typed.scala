package io.mytc.sood.vm.std.libs

import com.google.protobuf.ByteString
import io.mytc.sood.vm.serialization._
import io.mytc.sood.vm.state.Data
import io.mytc.sood.vm.std.{Func, NativeLibrary}

object Typed extends NativeLibrary {

  val Int32Tag: Byte = 1
  val Float64Tag: Byte = 2

  // FIXME optimize
  def dataToTyped(typeTag: Byte, data: Data): Data =
    ByteString.copyFrom(Array(typeTag)).concat(data)

  def typedTag(data: Data): Byte =
    data.byteAt(0)

  val typedI32: Func = Func("typedI32", m => {
    m.copy(stack = m.stack.map(dataToTyped(Int32Tag, _)))
  })

  val typedR32: Func = Func("typedR64", m => {
    m.copy(stack = m.stack.map(dataToTyped(Float64Tag, _)))
  })

  private def createArithmeticFunc(name: String,
                                   ii2i: (Int, Int) => Int,
                                   ff2f: (Double, Double) => Double,
                                   fi2f: (Double, Int) => Double,
                                   if2f: (Int, Double) => Double): Func =
    Func(
      name,
      m => {
        val a = m.stack(0)
        val b = m.stack(1)
        m.stack.clear()

        val res = (typedTag(a), typedTag(b)) match {
          case (Int32Tag, Int32Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          ii2i(dataToInt32(a.substring(1)), dataToInt32(b.substring(1)))
                        ))
          case (Float64Tag, Float64Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          ff2f(dataToDouble(a.substring(1)), dataToDouble(b.substring(1)))
                        ))
          case (Int32Tag, Float64Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          if2f(dataToInt32(a.substring(1)), dataToDouble(b.substring(1)))
                        ))
          case (Float64Tag, Int32Tag) =>
            dataToTyped(Float64Tag,
                        doubleToData(
                          fi2f(dataToDouble(a.substring(1)), dataToInt32(b.substring(1)))
                        ))
          case (_, _) => int32ToData(0) // FIXME
        }

        m.push(res)
        m
      }
    )

  val typedAdd: Func = createArithmeticFunc("typedAdd", _ + _, _ + _, _ + _, _ + _)
  val typedMul: Func = createArithmeticFunc("typedMul", _ * _, _ * _, _ * _, _ * _)
  val typedDiv: Func = createArithmeticFunc("typedDiv", _ / _, _ / _, _ / _, _ / _)
  val typedMod: Func = createArithmeticFunc("typedMod", _ % _, _ % _, _ % _, _ % _)

  override val address: String = "Typed"
  override val functions: Seq[Func] = Array(
    typedI32,
    typedR32,
    typedAdd,
    typedMul,
    typedDiv,
    typedMod
  )
}
