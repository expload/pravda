package pravda.cli

import pravda.cli.languages.impl._
import pravda.cli.programs._
import sys.process.stderr

/**
  * Pravda CLI entry point.
  */
object Application extends App {

  lazy val io = new IoLanguageImpl()
  lazy val compilers = new CompilersLanguageImpl()
  lazy val random = new RandomLanguageImpl()
  lazy val vm = new VmLanguageImpl()

  lazy val compile = new Compile(io, compilers)
  lazy val genAddress = new GenAddress(io, random)
  lazy val runner = new RunBytecode(io, vm)

  Parser.parse(args, Config.Nope) match {
    case Some(config: Config.Compile)     => compile(config)
    case Some(config: Config.RunBytecode) => runner(config)
    case Some(config: Config.GenAddress)  => genAddress(config)
    case _ =>
      stderr.println(Parser.renderTwoColumnsUsage)
      sys.exit(1)
  }

}
