package io.mytc.timechain.utils

sealed trait CpuArchitecture

object CpuArchitecture {
  case object x86         extends CpuArchitecture
  case object x86_64      extends CpuArchitecture
  case object Unsupported extends CpuArchitecture
}
