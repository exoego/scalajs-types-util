package net.exoego.scalajs.types.util

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.StdNames._
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.transform.Staging

class FactoryPlugin extends StandardPlugin {
  val name: String = "generateFactory"

  override val description: String = "generate factory method in companion object"

  def init(options: List[String]): List[PluginPhase] = List(new GenerateFactoryPhase)
}

class GenerateFactoryPhase extends PluginPhase {
  val phaseName ="generateFactory"
  
  override val runsAfter: Set[String]  = Set.empty
  override val runsBefore: Set[String] = Set(Staging.name)

  override def transformApply(tree: tpd.Apply)(implicit context: Context): tpd.Tree = ???

}
