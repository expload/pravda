package io.mytc.timechain.data

import com.google.protobuf.ByteString
import supertagged.TaggedType
import io.mytc.timechain.utils._

import scala.util.Try

object common {

  object Mytc extends TaggedType[BigDecimal] {
    val zero = apply(BigDecimal(0))
    def amount(v: Int) = Mytc(BigDecimal(v))
    def amount(v: Double) = Mytc(BigDecimal(v))
    def amount(v: String) = Mytc(BigDecimal(v))
    def fromString(s: String) = Mytc(BigDecimal(s))
  }

  type Mytc = Mytc.Type


  implicit def matrixToVector[T](matrix: Matrix[T]): Vector[T] = {
    matrix.data
  }

  case class TariffMatrix(vendor: Address, matrix: Matrix[BigDecimal]) {
    val tariffs = matrix.cols
    val records = matrix.rows

    /**
      * Calculating the final price according to the following formula:
      *<p>
      * \phi(a,r) * m * i
      *</p>
      * <p>
      * Here:
      * <ul>
      *   <li>a = log(b_2 - b_1)</li>
      *   <li>\phi(a,r) = k_{\lfloor a \rfloor,r} - (k_{\lfloor a \rfloor,r} - k_{\lfloor a \rfloor + 1,r}) * \{a\}</li>
      *   <li>b_2 — the height of the current block, as of the time a confirmation is placed</li>
      *   <li>b_1  — the height of a block, containing a reward transaction</li>
      *   <li>r — the tariff index</li>
      *   <li>m — the multiplier</li>
      *   <li>i — the current MYTC market rate</li>
      *   <li>k — tariff matrix</li>
      * </ul>
      *
      *</p>
      *
      * @param tariff tariff ($r$)
      * @param blockDiff block difference ($b_2 - b_1$)
      * @param marketRate $i$
      * @param multiplier vendor's multiplier ($m$)
      * @return final price in Mytc
      */
    def price(tariff: Int, blockDiff: Long, marketRate: BigDecimal, multiplier: BigDecimal): Mytc = {
      assert(tariff <= tariffs)
      assert(tariff > 0)

      val r = tariff - 1
      val diff = if (blockDiff == 0) 1 else blockDiff
      val a = log2(diff)
      val i = marketRate
      val m = multiplier
      val aFloor = math.floor(a).toInt
      val aFrac = BigDecimal(a - math.floor(a))

      val phi = k(aFloor, r) - ( k(aFloor, r) - k(aFloor + 1, r) ) * aFrac

      Mytc(phi * i * m)
    }
    def tariff(id: Int): Vector[BigDecimal] = {
      assert(id <= tariffs)
      assert(id > 0)

      val start = (id - 1) * records
      matrix.data.slice(start, start + records)
    }

    private def k(row: Int, col: Int) = {
      val r = if(row >= records) records - 1 else row
      matrix(r, col)
    }
    private def log2(num: Long) = {
      math.log(num) / math.log(2)
    }

  }
  object TariffMatrix {
    def apply(vendor: Address): TariffMatrix = new TariffMatrix(vendor, Matrix.empty[BigDecimal])
    def apply(vendor: Address, tariffs: Int, records: Int, data: Vector[BigDecimal]): TariffMatrix = {
      TariffMatrix(vendor, Matrix(records, tariffs, data))
    }
  }

  object Matrix {
    def empty[T] = Matrix(0, 0, Vector.empty[T])
  }

  case class Matrix[T](val rows: Int, val cols: Int, data: Vector[T]) {
    assert(cols >= 0)
    assert(rows >= 0)
    assert(data.length == rows * cols)
    def apply(row: Int, col: Int): T = {
      assert(row < rows)
      assert(col < cols)
      data(col * rows + row)
    }
  }

  object Address extends TaggedType[ByteString] {

    final val Void = {
      val bytes = ByteString.copyFrom(Array.fill(32)(0.toByte))
      Address(bytes)
    }

    def tryFromHex(hex: String): Try[Address] =
      Try(Address(hex2byteString(hex)))

    def fromHex(hex: String): Address =
      Address(hex2byteString(hex))
  }
  type Address = Address.Type

  object DataRef extends TaggedType[ByteString] {
    def fromHex(hex: String): DataRef =
      DataRef(hex2byteString(hex))
  }
  type DataRef = DataRef.Type

  case class DepositId(owner: Address, vendor: Address)

  case class OrganizationInfo(
    address: Address,
    domain: String,
    label: String,
    path: Option[String]
  )

  object NodeSettings {
    val default = NodeSettings(
      punishmentTimeout = 0L
    )
  }

  case class NodeSettings(punishmentTimeout: Long)

}
