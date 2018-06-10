package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.vm.state.VmError.{InvalidAddress, InvalidCoinAmount, WrongType}
import pravda.vm.state.{Data, VmErrorException}

import scala.collection.mutable

object DataOperations {
  
  import Data._
  import Primitive._
  import Array._

  def lt(a: Data, b: Data): Bool = gt(b, a)

  def gt(a: Data, b: Data): Bool = {
    val result = a match {
      case Int32(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > rhs
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)  
      }
      case Int16(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => rhs < lhs.toInt
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case Int8(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => rhs < lhs.toInt
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case Uint8(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > rhs
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case Uint16(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > rhs
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case Uint32(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > rhs
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case Number(lhs) => b match {
        case Int8(rhs) => lhs > rhs
        case Int16(rhs) => lhs > rhs
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > BigDecimal(rhs)
        case Number(rhs) => lhs > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case BigInt(lhs) => b match {
        case Int8(rhs) => lhs > rhs.toInt
        case Int16(rhs) => lhs > rhs.toInt
        case Int32(rhs) => lhs > rhs
        case Uint8(rhs) => lhs > rhs
        case Uint16(rhs) => lhs > rhs
        case Uint32(rhs) => lhs > rhs
        case BigInt(rhs) => lhs > rhs
        case Number(rhs) => BigDecimal(lhs) > rhs
        case _ => throw VmErrorException(WrongType)
      }
      case _ => throw VmErrorException(WrongType)
    }
    Bool(result)
  }

  def add(a: Data, b: Data): Data = a match {
    case Int32(lhs) => b match {
      case Int8(rhs) => Int32(lhs + rhs)
      case Int16(rhs) => Int32(lhs + rhs)
      case Int32(rhs) => Int32(lhs + rhs)
      case Uint8(rhs) => Int32(lhs + rhs)
      case Uint16(rhs) => Int32(lhs + rhs)
      case Uint32(rhs) => Int32((lhs + rhs).toInt)
      case BigInt(rhs) => Int32((lhs + rhs).toInt)
      case Number(rhs) => Int32((lhs + rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16(lhs) => b match {
      case Int8(rhs) => Int16((lhs + rhs).toShort)
      case Int16(rhs) => Int16((lhs + rhs).toShort)
      case Int32(rhs) => Int16((lhs + rhs).toShort)
      case Uint8(rhs) => Int16((lhs + rhs).toShort)
      case Uint16(rhs) => Int16((lhs + rhs).toShort)
      case Uint32(rhs) => Int16((lhs + rhs).toShort)
      case BigInt(rhs) => Int16((lhs.toInt + rhs).toShort)
      case Number(rhs) => Int16((lhs + rhs).toShort)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8(lhs) => b match {
      case Int8(rhs) => Int32(lhs + rhs)
      case Int16(rhs) => Int32(lhs + rhs)
      case Int32(rhs) => Int32(lhs + rhs)
      case Uint8(rhs) => Int32(lhs + rhs)
      case Uint16(rhs) => Int32(lhs + rhs)
      case Uint32(rhs) => BigInt(lhs + rhs)
      case BigInt(rhs) => BigInt(lhs.toInt + rhs)
      case Number(rhs) => Number(lhs + rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8(lhs) => b match {
      case Int8(rhs) => Uint8(lhs + rhs)
      case Int16(rhs) => Uint8(lhs + rhs)
      case Int32(rhs) => Uint8(lhs + rhs)
      case Uint8(rhs) => Uint8(lhs + rhs)
      case Uint16(rhs) => Uint8(lhs + rhs)
      case Uint32(rhs) => Uint8((lhs + rhs).toInt)
      case BigInt(rhs) => Uint8((lhs + rhs).toInt)
      case Number(rhs) => Uint8((lhs + rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16(lhs) => b match {
      case Int8(rhs) => Uint16(lhs + rhs)
      case Int16(rhs) => Uint16(lhs + rhs)
      case Int32(rhs) => Uint16(lhs + rhs)
      case Uint8(rhs) => Uint16(lhs + rhs)
      case Uint16(rhs) => Uint16(lhs + rhs)
      case Uint32(rhs) => Uint16((lhs + rhs).toInt)
      case BigInt(rhs) => Uint16((lhs + rhs).toInt)
      case Number(rhs) => Uint16((lhs + rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32(lhs) => b match {
      case Int8(rhs) => Uint32(lhs + rhs)
      case Int16(rhs) => Uint32(lhs + rhs)
      case Int32(rhs) => Uint32(lhs + rhs)
      case Uint8(rhs) => Uint32(lhs + rhs)
      case Uint16(rhs) => Uint32(lhs + rhs)
      case Uint32(rhs) => Uint32(lhs + rhs)
      case BigInt(rhs) => Uint32((lhs + rhs).toLong)
      case Number(rhs) => Uint32((lhs + rhs).toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case Number(lhs) => b match {
      case Int8(rhs) => Number(lhs + rhs)
      case Int16(rhs) => Number(lhs + rhs)
      case Int32(rhs) => Number(lhs + rhs)
      case Uint8(rhs) => Number(lhs + rhs)
      case Uint16(rhs) => Number(lhs + rhs)
      case Uint32(rhs) => Number(lhs + rhs)
      case BigInt(rhs) => Number((lhs + BigDecimal(rhs)).toDouble)
      case Number(rhs) => Number(lhs + rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BigInt(lhs) => b match {
      case Int8(rhs) => BigInt(lhs + scala.BigInt(rhs.toInt))
      case Int16(rhs) => BigInt(lhs + scala.BigInt(rhs.toInt))
      case Int32(rhs) => BigInt(lhs + rhs)
      case Uint8(rhs) => BigInt(lhs + rhs)
      case Uint16(rhs) => BigInt(lhs + rhs)
      case Uint32(rhs) => BigInt(lhs + rhs)
      case BigInt(rhs) => BigInt(lhs + rhs)
      case Number(rhs) => BigInt(lhs + rhs.toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def mul(a: Data, b: Data): Data = a match {
    case Int32(lhs) => b match {
      case Int8(rhs) => Int32(lhs * rhs)
      case Int16(rhs) => Int32(lhs * rhs)
      case Int32(rhs) => Int32(lhs * rhs)
      case Uint8(rhs) => Int32(lhs * rhs)
      case Uint16(rhs) => Int32(lhs * rhs)
      case Uint32(rhs) => Int32((lhs * rhs).toInt)
      case BigInt(rhs) => Int32((lhs * rhs).toInt)
      case Number(rhs) => Int32((lhs * rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16(lhs) => b match {
      case Int8(rhs) => Int16((lhs * rhs).toShort)
      case Int16(rhs) => Int16((lhs * rhs).toShort)
      case Int32(rhs) => Int16((lhs * rhs).toShort)
      case Uint8(rhs) => Int16((lhs * rhs).toShort)
      case Uint16(rhs) => Int16((lhs * rhs).toShort)
      case Uint32(rhs) => Int16((lhs * rhs).toShort)
      case BigInt(rhs) => Int16((lhs.toInt * rhs).toShort)
      case Number(rhs) => Int16((lhs * rhs).toShort)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8(lhs) => b match {
      case Int8(rhs) => Int32(lhs * rhs)
      case Int16(rhs) => Int32(lhs * rhs)
      case Int32(rhs) => Int32(lhs * rhs)
      case Uint8(rhs) => Int32(lhs * rhs)
      case Uint16(rhs) => Int32(lhs * rhs)
      case Uint32(rhs) => BigInt(lhs * rhs)
      case BigInt(rhs) => BigInt(lhs.toInt * rhs)
      case Number(rhs) => Number(lhs * rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8(lhs) => b match {
      case Int8(rhs) => Uint8(lhs * rhs)
      case Int16(rhs) => Uint8(lhs * rhs)
      case Int32(rhs) => Uint8(lhs * rhs)
      case Uint8(rhs) => Uint8(lhs * rhs)
      case Uint16(rhs) => Uint8(lhs * rhs)
      case Uint32(rhs) => Uint8((lhs * rhs).toInt)
      case BigInt(rhs) => Uint8((lhs * rhs).toInt)
      case Number(rhs) => Uint8((lhs * rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16(lhs) => b match {
      case Int8(rhs) => Uint16(lhs * rhs)
      case Int16(rhs) => Uint16(lhs * rhs)
      case Int32(rhs) => Uint16(lhs * rhs)
      case Uint8(rhs) => Uint16(lhs * rhs)
      case Uint16(rhs) => Uint16(lhs * rhs)
      case Uint32(rhs) => Uint16((lhs * rhs).toInt)
      case BigInt(rhs) => Uint16((lhs * rhs).toInt)
      case Number(rhs) => Uint16((lhs * rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32(lhs) => b match {
      case Int8(rhs) => Uint32(lhs * rhs)
      case Int16(rhs) => Uint32(lhs * rhs)
      case Int32(rhs) => Uint32(lhs * rhs)
      case Uint8(rhs) => Uint32(lhs * rhs)
      case Uint16(rhs) => Uint32(lhs * rhs)
      case Uint32(rhs) => Uint32(lhs * rhs)
      case BigInt(rhs) => Uint32((lhs * rhs).toLong)
      case Number(rhs) => Uint32((lhs * rhs).toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case Number(lhs) => b match {
      case Int8(rhs) => Number(lhs * rhs)
      case Int16(rhs) => Number(lhs * rhs)
      case Int32(rhs) => Number(lhs * rhs)
      case Uint8(rhs) => Number(lhs * rhs)
      case Uint16(rhs) => Number(lhs * rhs)
      case Uint32(rhs) => Number(lhs * rhs)
      case BigInt(rhs) => Number((lhs * BigDecimal(rhs)).toDouble)
      case Number(rhs) => Number(lhs * rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BigInt(lhs) => b match {
      case Int8(rhs) => BigInt(lhs * scala.BigInt(rhs.toInt))
      case Int16(rhs) => BigInt(lhs * scala.BigInt(rhs.toInt))
      case Int32(rhs) => BigInt(lhs * rhs)
      case Uint8(rhs) => BigInt(lhs * rhs)
      case Uint16(rhs) => BigInt(lhs * rhs)
      case Uint32(rhs) => BigInt(lhs * rhs)
      case BigInt(rhs) => BigInt(lhs * rhs)
      case Number(rhs) => BigInt(lhs * rhs.toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def div(a: Data, b: Data): Data = a match {
    case Int32(lhs) => b match {
      case Int8(rhs) => Int32(lhs / rhs)
      case Int16(rhs) => Int32(lhs / rhs)
      case Int32(rhs) => Int32(lhs / rhs)
      case Uint8(rhs) => Int32(lhs / rhs)
      case Uint16(rhs) => Int32(lhs / rhs)
      case Uint32(rhs) => Int32((lhs / rhs).toInt)
      case BigInt(rhs) => Int32((lhs / rhs).toInt)
      case Number(rhs) => Int32((lhs / rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16(lhs) => b match {
      case Int8(rhs) => Int16((lhs / rhs).toShort)
      case Int16(rhs) => Int16((lhs / rhs).toShort)
      case Int32(rhs) => Int16((lhs / rhs).toShort)
      case Uint8(rhs) => Int16((lhs / rhs).toShort)
      case Uint16(rhs) => Int16((lhs / rhs).toShort)
      case Uint32(rhs) => Int16((lhs / rhs).toShort)
      case BigInt(rhs) => Int16((lhs.toInt / rhs).toShort)
      case Number(rhs) => Int16((lhs / rhs).toShort)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8(lhs) => b match {
      case Int8(rhs) => Int32(lhs / rhs)
      case Int16(rhs) => Int32(lhs / rhs)
      case Int32(rhs) => Int32(lhs / rhs)
      case Uint8(rhs) => Int32(lhs / rhs)
      case Uint16(rhs) => Int32(lhs / rhs)
      case Uint32(rhs) => BigInt(lhs / rhs)
      case BigInt(rhs) => BigInt(lhs.toInt / rhs)
      case Number(rhs) => Number(lhs / rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8(lhs) => b match {
      case Int8(rhs) => Uint8(lhs / rhs)
      case Int16(rhs) => Uint8(lhs / rhs)
      case Int32(rhs) => Uint8(lhs / rhs)
      case Uint8(rhs) => Uint8(lhs / rhs)
      case Uint16(rhs) => Uint8(lhs / rhs)
      case Uint32(rhs) => Uint8((lhs / rhs).toInt)
      case BigInt(rhs) => Uint8((lhs / rhs).toInt)
      case Number(rhs) => Uint8((lhs / rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16(lhs) => b match {
      case Int8(rhs) => Uint16(lhs / rhs)
      case Int16(rhs) => Uint16(lhs / rhs)
      case Int32(rhs) => Uint16(lhs / rhs)
      case Uint8(rhs) => Uint16(lhs / rhs)
      case Uint16(rhs) => Uint16(lhs / rhs)
      case Uint32(rhs) => Uint16((lhs / rhs).toInt)
      case BigInt(rhs) => Uint16((lhs / rhs).toInt)
      case Number(rhs) => Uint16((lhs / rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32(lhs) => b match {
      case Int8(rhs) => Uint32(lhs / rhs)
      case Int16(rhs) => Uint32(lhs / rhs)
      case Int32(rhs) => Uint32(lhs / rhs)
      case Uint8(rhs) => Uint32(lhs / rhs)
      case Uint16(rhs) => Uint32(lhs / rhs)
      case Uint32(rhs) => Uint32(lhs / rhs)
      case BigInt(rhs) => Uint32((lhs / rhs).toLong)
      case Number(rhs) => Uint32((lhs / rhs).toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case Number(lhs) => b match {
      case Int8(rhs) => Number(lhs / rhs)
      case Int16(rhs) => Number(lhs / rhs)
      case Int32(rhs) => Number(lhs / rhs)
      case Uint8(rhs) => Number(lhs / rhs)
      case Uint16(rhs) => Number(lhs / rhs)
      case Uint32(rhs) => Number(lhs / rhs)
      case BigInt(rhs) => Number((lhs / BigDecimal(rhs)).toDouble)
      case Number(rhs) => Number(lhs / rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BigInt(lhs) => b match {
      case Int8(rhs) => BigInt(lhs / scala.BigInt(rhs.toInt))
      case Int16(rhs) => BigInt(lhs / scala.BigInt(rhs.toInt))
      case Int32(rhs) => BigInt(lhs / rhs)
      case Uint8(rhs) => BigInt(lhs / rhs)
      case Uint16(rhs) => BigInt(lhs / rhs)
      case Uint32(rhs) => BigInt(lhs / rhs)
      case BigInt(rhs) => BigInt(lhs / rhs)
      case Number(rhs) => BigInt(lhs / rhs.toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def mod(a: Data, b: Data): Data = a match {
    case Int32(lhs) => b match {
      case Int8(rhs) => Int32(lhs % rhs)
      case Int16(rhs) => Int32(lhs % rhs)
      case Int32(rhs) => Int32(lhs % rhs)
      case Uint8(rhs) => Int32(lhs % rhs)
      case Uint16(rhs) => Int32(lhs % rhs)
      case Uint32(rhs) => Int32((lhs % rhs).toInt)
      case BigInt(rhs) => Int32((lhs % rhs).toInt)
      case Number(rhs) => Int32((lhs % rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16(lhs) => b match {
      case Int8(rhs) => Int16((lhs % rhs).toShort)
      case Int16(rhs) => Int16((lhs % rhs).toShort)
      case Int32(rhs) => Int16((lhs % rhs).toShort)
      case Uint8(rhs) => Int16((lhs % rhs).toShort)
      case Uint16(rhs) => Int16((lhs % rhs).toShort)
      case Uint32(rhs) => Int16((lhs % rhs).toShort)
      case BigInt(rhs) => Int16((lhs.toInt % rhs).toShort)
      case Number(rhs) => Int16((lhs % rhs).toShort)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8(lhs) => b match {
      case Int8(rhs) => Int32(lhs % rhs)
      case Int16(rhs) => Int32(lhs % rhs)
      case Int32(rhs) => Int32(lhs % rhs)
      case Uint8(rhs) => Int32(lhs % rhs)
      case Uint16(rhs) => Int32(lhs % rhs)
      case Uint32(rhs) => BigInt(lhs % rhs)
      case BigInt(rhs) => BigInt(lhs.toInt % rhs)
      case Number(rhs) => Number(lhs % rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8(lhs) => b match {
      case Int8(rhs) => Uint8(lhs % rhs)
      case Int16(rhs) => Uint8(lhs % rhs)
      case Int32(rhs) => Uint8(lhs % rhs)
      case Uint8(rhs) => Uint8(lhs % rhs)
      case Uint16(rhs) => Uint8(lhs % rhs)
      case Uint32(rhs) => Uint8((lhs % rhs).toInt)
      case BigInt(rhs) => Uint8((lhs % rhs).toInt)
      case Number(rhs) => Uint8((lhs % rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16(lhs) => b match {
      case Int8(rhs) => Uint16(lhs % rhs)
      case Int16(rhs) => Uint16(lhs % rhs)
      case Int32(rhs) => Uint16(lhs % rhs)
      case Uint8(rhs) => Uint16(lhs % rhs)
      case Uint16(rhs) => Uint16(lhs % rhs)
      case Uint32(rhs) => Uint16((lhs % rhs).toInt)
      case BigInt(rhs) => Uint16((lhs % rhs).toInt)
      case Number(rhs) => Uint16((lhs % rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32(lhs) => b match {
      case Int8(rhs) => Uint32(lhs % rhs)
      case Int16(rhs) => Uint32(lhs % rhs)
      case Int32(rhs) => Uint32(lhs % rhs)
      case Uint8(rhs) => Uint32(lhs % rhs)
      case Uint16(rhs) => Uint32(lhs % rhs)
      case Uint32(rhs) => Uint32(lhs % rhs)
      case BigInt(rhs) => Uint32((lhs % rhs).toLong)
      case Number(rhs) => Uint32((lhs % rhs).toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case Number(lhs) => b match {
      case Int8(rhs) => Number(lhs % rhs)
      case Int16(rhs) => Number(lhs % rhs)
      case Int32(rhs) => Number(lhs % rhs)
      case Uint8(rhs) => Number(lhs % rhs)
      case Uint16(rhs) => Number(lhs % rhs)
      case Uint32(rhs) => Number(lhs % rhs)
      case BigInt(rhs) => Number((lhs % BigDecimal(rhs)).toDouble)
      case Number(rhs) => Number(lhs % rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BigInt(lhs) => b match {
      case Int8(rhs) => BigInt(lhs % scala.BigInt(rhs.toInt))
      case Int16(rhs) => BigInt(lhs % scala.BigInt(rhs.toInt))
      case Int32(rhs) => BigInt(lhs % rhs)
      case Uint8(rhs) => BigInt(lhs % rhs)
      case Uint16(rhs) => BigInt(lhs % rhs)
      case Uint32(rhs) => BigInt(lhs % rhs)
      case BigInt(rhs) => BigInt(lhs % rhs)
      case Number(rhs) => BigInt(lhs % rhs.toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def xor(a: Data, b: Data): Data = a match {
    case Int32(lhs) => b match {
      case Int8(rhs) => Int32(lhs ^ rhs)
      case Int16(rhs) => Int32(lhs ^ rhs)
      case Int32(rhs) => Int32(lhs ^ rhs)
      case Uint8(rhs) => Int32(lhs ^ rhs)
      case Uint16(rhs) => Int32(lhs ^ rhs)
      case Uint32(rhs) => Int32((lhs ^ rhs).toInt)
      case BigInt(rhs) => Int32((lhs ^ rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16(lhs) => b match {
      case Int8(rhs) => Int16((lhs ^ rhs).toShort)
      case Int16(rhs) => Int16((lhs ^ rhs).toShort)
      case Int32(rhs) => Int16((lhs ^ rhs).toShort)
      case Uint8(rhs) => Int16((lhs ^ rhs).toShort)
      case Uint16(rhs) => Int16((lhs ^ rhs).toShort)
      case Uint32(rhs) => Int16((lhs ^ rhs).toShort)
      case BigInt(rhs) => Int16((lhs.toInt ^ rhs).toShort)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8(lhs) => b match {
      case Int8(rhs) => Int32(lhs ^ rhs)
      case Int16(rhs) => Int32(lhs ^ rhs)
      case Int32(rhs) => Int32(lhs ^ rhs)
      case Uint8(rhs) => Int32(lhs ^ rhs)
      case Uint16(rhs) => Int32(lhs ^ rhs)
      case Uint32(rhs) => BigInt(lhs ^ rhs)
      case BigInt(rhs) => BigInt(lhs.toInt ^ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8(lhs) => b match {
      case Int8(rhs) => Uint8(lhs ^ rhs)
      case Int16(rhs) => Uint8(lhs ^ rhs)
      case Int32(rhs) => Uint8(lhs ^ rhs)
      case Uint8(rhs) => Uint8(lhs ^ rhs)
      case Uint16(rhs) => Uint8(lhs ^ rhs)
      case Uint32(rhs) => Uint8((lhs ^ rhs).toInt)
      case BigInt(rhs) => Uint8((lhs ^ rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16(lhs) => b match {
      case Int8(rhs) => Uint16(lhs ^ rhs)
      case Int16(rhs) => Uint16(lhs ^ rhs)
      case Int32(rhs) => Uint16(lhs ^ rhs)
      case Uint8(rhs) => Uint16(lhs ^ rhs)
      case Uint16(rhs) => Uint16(lhs ^ rhs)
      case Uint32(rhs) => Uint16((lhs ^ rhs).toInt)
      case BigInt(rhs) => Uint16((lhs ^ rhs).toInt)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32(lhs) => b match {
      case Int8(rhs) => Uint32(lhs ^ rhs)
      case Int16(rhs) => Uint32(lhs ^ rhs)
      case Int32(rhs) => Uint32(lhs ^ rhs)
      case Uint8(rhs) => Uint32(lhs ^ rhs)
      case Uint16(rhs) => Uint32(lhs ^ rhs)
      case Uint32(rhs) => Uint32(lhs ^ rhs)
      case BigInt(rhs) => Uint32((lhs ^ rhs).toLong)
      case _ => throw VmErrorException(WrongType)
    }
    case BigInt(lhs) => b match {
      case Int8(rhs) => BigInt(lhs ^ scala.BigInt(rhs.toInt))
      case Int16(rhs) => BigInt(lhs ^ scala.BigInt(rhs.toInt))
      case Int32(rhs) => BigInt(lhs ^ rhs)
      case Uint8(rhs) => BigInt(lhs ^ rhs)
      case Uint16(rhs) => BigInt(lhs ^ rhs)
      case Uint32(rhs) => BigInt(lhs ^ rhs)
      case BigInt(rhs) => BigInt(lhs ^ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def or(a: Data, b: Data): Bool = a match {
    case Bool.True => Bool.True
    case Bool.False => b match {
      case Bool.True => Bool.True
      case Bool.False => Bool.False
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def and(a: Data, b: Data): Bool = a match {
    case Bool.False => Bool.False
    case Bool.True => b match {
      case Bool.True => Bool.True
      case Bool.False => Bool.False
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }

  def not(a: Data): Bool = a match {
    case Bool.True => Bool.False
    case Bool.False => Bool.True
    case _ => throw VmErrorException(WrongType)
  }

  //----------------------------------------------------------------------------------
  // Converters
  //----------------------------------------------------------------------------------

  def int32(value: Data): Int = value match {
    case Int32(x) => x
    case _ => throw VmErrorException(WrongType)
  }

  def boolean(value: Data): Boolean = value match {
    case Bool.True => true
    case Bool.False => false
    case _ => throw VmErrorException(WrongType)
  }

  def bytes(a: Data): ByteString = {
    val bytes = a match {
      case BigInt(data) => data.toByteArray
      case Uint8Array(data) => data.toArray.map(_.toByte)
      case Int8Array(data) => data.toArray
      case _ => throw VmErrorException(WrongType)
    }
    ByteString.copyFrom(bytes)
  }

  def bytes(a: ByteString): Int8Array =
    Int8Array(a.toByteArray.to[mutable.Buffer])

  def coin(a: Data): NativeCoin = a match {
    case BigInt(data) if data < Long.MinValue || data > Long.MaxValue => throw VmErrorException(InvalidCoinAmount)
    case BigInt(data) => NativeCoin @@ data.toLong
    case _ => throw VmErrorException(WrongType)
  }

  def coin(a: NativeCoin): Data =
    BigInt(scala.BigInt(a))

  def address(a: Data): Address = {
    val bytes = a match {
      case BigInt(data) => data.toByteArray
      case Uint8Array(data) => data.toArray.map(_.toByte)
      case Int8Array(data) => data.toArray
      case _ => throw VmErrorException(WrongType)
    }
    if (bytes.length == 32) Address @@ ByteString.copyFrom(bytes)
    else throw VmErrorException(InvalidAddress)
  }

  def address(a: Address): Data = {
    val bytes = a.toByteArray
    if (bytes.length == 32) BigInt(scala.BigInt(bytes))
    else throw VmErrorException(InvalidAddress)
  }

  //----------------------------------------------------------------------------------
  // Misc
  //----------------------------------------------------------------------------------

  def slice(a: Data, from: Int, until: Int): Array = a match {
    case Utf8(data) => Utf8(data.substring(from, until))
    case Int8Array(data) => Int8Array(data.slice(from, until))
    case Int16Array(data) => Int16Array(data.slice(from, until))
    case Int32Array(data) => Int32Array(data.slice(from, until))
    case Uint8Array(data) => Uint8Array(data.slice(from, until))
    case Uint16Array(data) => Uint16Array(data.slice(from, until))
    case Uint32Array(data) => Uint32Array(data.slice(from, until))
    case NumberArray(data) => NumberArray(data.slice(from, until))
    case BigIntArray(data) => BigIntArray(data.slice(from, until))
    case RefArray(data) => RefArray(data.slice(from, until))
    case BoolArray(data) => BoolArray(data.slice(from, until))
    case _ => throw VmErrorException(WrongType)
  }
  
  def concat(a: Data, b: Data): Array = a match {
    case Utf8(lhs) => b match {
      case Utf8(rhs) => Utf8(lhs + rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Int8Array(lhs) => b match {
      case Int8Array(rhs) => Int8Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Int16Array(lhs) => b match {
      case Int16Array(rhs) => Int16Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Int32Array(lhs) => b match {
      case Int32Array(rhs) => Int32Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint8Array(lhs) => b match {
      case Uint8Array(rhs) => Uint8Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint16Array(lhs) => b match {
      case Uint16Array(rhs) => Uint16Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case Uint32Array(lhs) => b match {
      case Uint32Array(rhs) => Uint32Array(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case NumberArray(lhs) => b match {
      case NumberArray(rhs) => NumberArray(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BigIntArray(lhs) => b match {
      case BigIntArray(rhs) => BigIntArray(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case RefArray(lhs) => b match {
      case RefArray(rhs) => RefArray(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case BoolArray(lhs) => b match {
      case BoolArray(rhs) => BoolArray(lhs ++ rhs)
      case _ => throw VmErrorException(WrongType)
    }
    case _ => throw VmErrorException(WrongType)
  }
}
