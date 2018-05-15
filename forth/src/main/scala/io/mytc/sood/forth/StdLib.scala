package io.mytc.sood.forth

object StdLib {

  import io.mytc.sood.asm.Op
  import io.mytc.sood.asm.Datum

  def defs: String = { """
  : + add ;
  : * mul ;
  : / div ;
  : % mod ;
  : == eq ;
  : != neq ;
  : dup dup1 ;
  : dup_2 dup2 ;
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
      fmod,
      eqls,
      neq,
      dup(1), dup(2), dup(3), dup(4), dup(5)
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

  val sget: Seq[Op] = Seq(
    Op.Label("sget"),
    Op.SGet,
    Op.Ret
  )

  val sput: Seq[Op] = Seq(
    Op.Label("sput"),
    Op.SPut,
    Op.Ret
  )

  val eqls: Seq[Op] = Seq(
    Op.Label("eq"),
    Op.Eq,
    Op.Ret
  )

  val neq: Seq[Op] = Seq(
    Op.Label("neq"),
    Op.Eq,
    Op.Not,
    Op.Ret
  )

  def dup(n: Int): Seq[Op] = Seq(
    Op.Label(s"dup${n}"),
    Op.Push(Datum.Integral(n)),
    Op.Dupn,
    Op.Ret
  )

}
