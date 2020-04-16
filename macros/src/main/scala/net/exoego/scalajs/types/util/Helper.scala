package net.exoego.scalajs.types.util

import scala.reflect.internal.Trees
import scala.reflect.macros.blackbox
import scala.scalajs.js

private[util] object Helper {
  def bail(message: String)(implicit c: blackbox.Context): Nothing = {
    c.abort(c.enclosingPosition, message)
  }

  def annotteeShouldBeTrait(c: blackbox.Context)(annottees: Seq[c.Expr[Any]]): Unit = {
    import c.universe._
    val inputs = annottees.map(_.tree).toList
    if (!inputs.headOption.exists(_.isInstanceOf[ClassDef])) {
      bail("Can annotate only trait")(c)
    }
  }

  def getInheritedMembers(c: blackbox.Context)(argumentType: c.universe.Type): List[c.universe.Symbol] = {
    import c.universe._
    // maybe decls instead of members?
    (argumentType.members.toSet -- c.typeOf[js.Object].members.toSet).toList
      .filterNot(_.isConstructor)
      .sortBy(_.name.decodedName.toString)
  }

  def getArgumentType[T]()(implicit c: blackbox.Context): T = {
    import c.universe._
    val argumentType: c.universe.Type = {
      val macroTypeWithArguments          = c.typecheck(q"${c.prefix.tree}").tpe
      val annotationClass: ClassSymbol    = macroTypeWithArguments.typeSymbol.asClass
      val annotationTypePlaceholder: Type = annotationClass.typeParams.head.asType.toType
      annotationTypePlaceholder.asSeenFrom(macroTypeWithArguments, annotationClass)
    }
    if (argumentType.finalResultType == c.typeOf[Nothing]) {
      bail("Type parameter T must be provided")
    }
    // Hack for type mismatch
    // [error]  found   : context.universe.Type
    // [error]  required: c.universe.Type
    argumentType.asInstanceOf[T]
  }

  def isScalaJsNative(c: blackbox.Context)(mods: c.universe.Modifiers): Boolean = {
    import c.universe._
    mods.annotations.exists {
      case q"new scala.scalajs.js.native()" => true
      case q"new scalajs.js.native()"       => true
      case q"new js.native()"               => true
      case _                                => false
    }
  }

  def nativeIfNeeded(c: blackbox.Context)(tree: c.universe.Tree, isJsNative: Boolean): c.universe.Tree = {
    import c.universe._
    if (isJsNative) {
      tree match {
        case q"var $name: ${tpt}"                => q"var $name: $tpt = scala.scalajs.js.native"
        case q"val $name: ${tpt}"                => q"val $name: $tpt = scala.scalajs.js.native"
        case q"def $name(...$paramss): $retType" => q"def $name(...$paramss): $retType = scala.scalajs.js.native"
        case _                                   => tree
      }
    } else {
      tree
    }
  }
}
