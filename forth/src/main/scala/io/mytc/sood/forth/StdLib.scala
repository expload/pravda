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
      mod,
      fadd,
      fmul,
      fdiv,
      fmod
    ).flatten

  val fadd: Seq[Op] = Seq(
    Op.Label("fadd"),
    Op.FAdd,
    Op.Ret
  )

  val fmul: Seq[Op] = Seq(
    Op.Label("fmul"),
    Op.FMul,
    Op.Ret
  )

  val fdiv: Seq[Op] = Seq(
    Op.Label("fdiv"),
    Op.FDiv,
    Op.Ret
  )

  val fmod: Seq[Op] = Seq(
    Op.Label("fmod"),
    Op.FMod,
    Op.Ret
  )

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
