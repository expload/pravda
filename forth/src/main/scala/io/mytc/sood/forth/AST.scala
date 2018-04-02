package io.mytc.sood.forth

sealed trait Statement

object Statement {
  case class Integ(v: Long)             extends Statement
  case class Float(v: Double)           extends Statement
  case class Ident(v: String)           extends Statement
  case class Delim(v: String)           extends Statement
}
