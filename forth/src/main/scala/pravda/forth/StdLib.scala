package pravda.forth

object StdLib {

  import pravda.vm.asm.Operation
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

  def words: Seq[Operation] =
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
      transfer,
      pop
    ).flatten

  val fadd: Seq[Operation] = Seq(
    Operation.Label("fadd"),
    Operation.FAdd,
    Operation.Ret
  )

  val fmul: Seq[Operation] = Seq(
    Operation.Label("fmul"),
    Operation.FMul,
    Operation.Ret
  )

  val fdiv: Seq[Operation] = Seq(
    Operation.Label("fdiv"),
    Operation.FDiv,
    Operation.Ret
  )

  val fmod: Seq[Operation] = Seq(
    Operation.Label("fmod"),
    Operation.FMod,
    Operation.Ret
  )

  val add: Seq[Operation] = Seq(
    Operation.Label("add"),
    Operation.Add,
    Operation.Ret
  )

  val mul: Seq[Operation] = Seq(
    Operation.Label("mul"),
    Operation.Mul,
    Operation.Ret
  )

  val div: Seq[Operation] = Seq(
    Operation.Label("div"),
    Operation.Div,
    Operation.Ret
  )

  val mod: Seq[Operation] = Seq(
    Operation.Label("mod"),
    Operation.Mod,
    Operation.Ret
  )

  val sget: Seq[Operation] = Seq(
    Operation.Label("sget"),
    Operation.SGet,
    Operation.Ret
  )

  val sput: Seq[Operation] = Seq(
    Operation.Label("sput"),
    Operation.SPut,
    Operation.Ret
  )

  val sexst: Seq[Operation] = Seq(
    Operation.Label("sexst"),
    Operation.SExist,
    Operation.Ret
  )

  val eqls: Seq[Operation] = Seq(
    Operation.Label("eq"),
    Operation.Eq,
    Operation.Ret
  )

  val neq: Seq[Operation] = Seq(
    Operation.Label("neq"),
    Operation.Eq,
    Operation.Not,
    Operation.Ret
  )

  val lt: Seq[Operation] = Seq(
    Operation.Label("lt"),
    Operation.Lt,
    Operation.Ret
  )

  val gt: Seq[Operation] = Seq(
    Operation.Label("gt"),
    Operation.Gt,
    Operation.Ret
  )

  val not: Seq[Operation] = Seq(
    Operation.Label("not"),
    Operation.Not,
    Operation.Ret
  )

  val concat: Seq[Operation] = Seq(
    Operation.Label("concat"),
    Operation.Concat,
    Operation.Ret
  )

  val from: Seq[Operation] = Seq(
    Operation.Label("from"),
    Operation.From,
    Operation.Ret
  )

  val pcreate: Seq[Operation] = Seq(
    Operation.Label("pcreate"),
    Operation.PCreate,
    Operation.Ret
  )

  val pupdate: Seq[Operation] = Seq(
    Operation.Label("pupdate"),
    Operation.PUpdate,
    Operation.Ret
  )

  val pcall: Seq[Operation] = Seq(
    Operation.Label("pcall"),
    Operation.PCall,
    Operation.Ret
  )

  val swap: Seq[Operation] = Seq(
    Operation.Label("swap"),
    Operation.Swap,
    Operation.Ret
  )

  def dup(n: Int): Seq[Operation] = Seq(
    Operation.Label(s"dup${n}"),
    Operation.Push(Datum.Integral(n)),
    Operation.Dupn,
    Operation.Ret
  )

  val transfer: Seq[Operation] = Seq(
    Operation.Label("transfer"),
    Operation.Transfer,
    Operation.Ret
  )

  def pop: Seq[Operation] = Seq(
    Operation.Label("pop"),
    Operation.Pop,
    Operation.Ret
  )
}
