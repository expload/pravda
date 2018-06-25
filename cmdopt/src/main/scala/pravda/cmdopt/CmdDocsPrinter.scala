package pravda.cmdopt

import pravda.cmdopt.CommandLine.Verb

trait CmdDocsPrinter[Config, Ctx] {
  def printCL(cl: CommandLine[Config], ctx: Ctx): String
  def printVerbs(verbs: List[Verb[Config, _]], ctx: Ctx): String
  def printVerb(verb: Verb[Config, _], ctx: Ctx): String
}
