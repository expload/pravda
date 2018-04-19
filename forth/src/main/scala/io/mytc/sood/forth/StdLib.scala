package io.mytc.sood.forth

object StdLib {

  import io.mytc.sood.asm.Op

  def defs: String = { """
  : + add ;
  : * mul ;
  : / div ;
  : % mod ;
  """ }

  def words: Seq[Op] =
    Seq(
      add,
      mul,
      div,
      mod
    ).flatten

  val add: Seq[Op] = Seq(
    Op.Label("add"),
    Op.I32Add,
    Op.Ret
  )

  val mul: Seq[Op] = Seq(
    Op.Label("mul"),
    Op.I32Mul,
    Op.Ret
  )

  val div: Seq[Op] = Seq(
    Op.Label("div"),
    Op.I32Div,
    Op.Ret
  )

  val mod: Seq[Op] = Seq(
    Op.Label("mod"),
    Op.I32Mod,
    Op.Ret
  )

}
