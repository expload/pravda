package io.mytc.sood.forth

object StdLib {

  import pravda.vm.asm.Op
  import pravda.vm.asm.Datum

  def defs: String = { """
    : + add ;
    : - -1 mul add ;
    : * mul ;
    : / div ;
    : % mod ;
    : == eq ;
    : != neq ;
    : < lt ;
    : > gt ;
    : <= gt not ;
    : >= lt not ;
    : dup dup1 ;
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
      lt,
      gt,
      not,
      dup(1),
      dup(2),
      dup(3),
      dup(4),
      dup(5),
      sget,
      sput,
      sexst,
      concat,
      from,
      pcall,
      pcreate,
      pupdate,
      swap,
      pcall,
      pop
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

  val sexst: Seq[Op] = Seq(
    Op.Label("sexst"),
    Op.SExst,
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

  val lt: Seq[Op] = Seq(
    Op.Label("lt"),
    Op.I32LT,
    Op.Ret
  )

  val gt: Seq[Op] = Seq(
    Op.Label("gt"),
    Op.I32GT,
    Op.Ret
  )

  val not: Seq[Op] = Seq(
    Op.Label("not"),
    Op.Not,
    Op.Ret
  )

  val concat: Seq[Op] = Seq(
    Op.Label("concat"),
    Op.Concat,
    Op.Ret
  )

  val from: Seq[Op] = Seq(
    Op.Label("from"),
    Op.From,
    Op.Ret
  )

  val pcreate: Seq[Op] = Seq(
    Op.Label("pcreate"),
    Op.PCreate,
    Op.Ret
  )

  val pupdate: Seq[Op] = Seq(
    Op.Label("pupdate"),
    Op.PUpdate,
    Op.Ret
  )

  val pcall: Seq[Op] = Seq(
    Op.Label("pcall"),
    Op.PCall,
    Op.Ret
  )

  val swap: Seq[Op] = Seq(
    Op.Label("swap"),
    Op.Swap,
    Op.Ret
  )

  def dup(n: Int): Seq[Op] = Seq(
    Op.Label(s"dup${n}"),
    Op.Push(Datum.Integral(n)),
    Op.Dupn,
    Op.Ret
  )

  def pop: Seq[Op] = Seq(
    Op.Label("pop"),
    Op.Pop,
    Op.Ret
  )
}
