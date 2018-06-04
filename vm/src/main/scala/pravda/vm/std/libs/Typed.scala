package pravda.vm

package std

package libs

import com.google.protobuf.ByteString
import pravda.vm.serialization._
import pravda.vm.state.Data

object Typed extends NativeLibrary {

  val Int32Tag: Byte = 1
  val Float64Tag: Byte = 2

  // FIXME optimize
  def dataToTyped(typeTag: Byte, data: Data): Data =
    ByteString.copyFrom(Array(typeTag)).concat(data)

  def typedTag(data: Data): Byte =
    data.byteAt(0)

  val typedI32: Func = Func("typedI32", m => {
    val typed = m.all.map(dataToTyped(Int32Tag, _))
    m.clear()
    m.push(typed)
  })

  val typedR32: Func = Func("typedR64", m => {
    val typed = m.all.map(dataToTyped(Float64Tag, _))
    m.clear()
    m.push(typed)
  })

  val typedBool: Func = Func("typedBool", m => {
    val b = if (m.stack(0).byteAt(0) == 0) 0 else 1
    m.clear()
    m.push(dataToTyped(Int32Tag, int32ToData(b)))
  })

  val typedNot: Func = Func("typedNot", m => {
    val a = dataToInt32(m.stack(0).substring(1))
    val res = if (a == 0) 1 else 0
    m.clear()
    m.push(dataToTyped(Int32Tag, int32ToData(res)))
  })

  private def createCmpFunc(name: String,
                            ii2b: (Int, Int) => Boolean,
                            ff2b: (Double, Double) => Boolean,
                            fi2b: (Double, Int) => Boolean,
                            if2b: (Int, Double) => Boolean): Func = {

    def b2i(b: Boolean): Int = if (b) 1 else 0

    Func(
      name,
      m => {
        val a = m.all(0)
        val b = m.all(1)
        m.clear()

        val res = (typedTag(a), typedTag(b)) match {
          case (Int32Tag, Int32Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          b2i(ii2b(dataToInt32(a.substring(1)), dataToInt32(b.substring(1))))
                        ))
          case (Float64Tag, Float64Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          b2i(ff2b(dataToDouble(a.substring(1)), dataToDouble(b.substring(1))))
                        ))
          case (Int32Tag, Float64Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          b2i(if2b(dataToInt32(a.substring(1)), dataToDouble(b.substring(1))))
                        ))
          case (Float64Tag, Int32Tag) =>
            dataToTyped(Int32Tag,
                        int32ToData(
                          b2i(fi2b(dataToDouble(a.substring(1)), dataToInt32(b.substring(1))))
                        ))
          case (_, _) => int32ToData(0) // FIXME
        }

        m.push(res)
      }
    )
  }

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
        m.clear()

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
      }
    )

  val typedAdd: Func = createArithmeticFunc("typedAdd", _ + _, _ + _, _ + _, _ + _)
  val typedMul: Func = createArithmeticFunc("typedMul", _ * _, _ * _, _ * _, _ * _)
  val typedDiv: Func = createArithmeticFunc("typedDiv", _ / _, _ / _, _ / _, _ / _)
  val typedMod: Func = createArithmeticFunc("typedMod", _ % _, _ % _, _ % _, _ % _)
  val typedClt: Func = createCmpFunc("typedClt", _ < _, _ < _, _ < _, _ < _)
  val typedCgt: Func = createCmpFunc("typedCgt", _ > _, _ > _, _ > _, _ > _)

  override val address: String = "Typed"
  override val functions: Seq[Func] = Seq(
    typedI32,
    typedR32,
    typedBool,
    typedAdd,
    typedMul,
    typedDiv,
    typedMod,
    typedClt,
    typedCgt,
    typedNot
  )
}
