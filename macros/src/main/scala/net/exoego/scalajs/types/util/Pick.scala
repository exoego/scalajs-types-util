package net.exoego.scalajs.types.util

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.scalajs.js

/**
  * Enrich the annotated type with only picked members of `T`.
  *
  * If the below code given,
  *
  * {{{
  * @js.native
  * trait Base extends js.Object {
  *   var foo: String = js.native
  *   val bar: js.Array[Int] = js.native
  *   def buz(x: String): Boolean = js.native
  * }
  *
  * @Pick[Base]("foo", "buz")
  * @js.native
  * trait Enriched extends js.Object {
  *   var ownProp: String = js.native
  * }
  * }}}
  *
  * The `Enriched` will be:
  *
  * {{{
  *   @js.native
  *   trait Enriched extends js.Object {
  *     var ownProp: String = js.native
  *
  *     // Picked from `Base`
  *     var foo: String = js.native
  *     def buz(x: String): Boolean = js.native
  *   }
  * }}}
  *
  * @tparam T
  */
class Pick[T <: js.Object](keys: String*) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Pick.impl
}

object Pick {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    def bail(message: String) = c.abort(c.enclosingPosition, message)

    val specifiedFieldNames: Set[String] = c.prefix.tree match {
      case q"new Pick[$a](..$b)" => b.map(_.toString.drop(1).dropRight(1)).toSet
      case _                     => bail("""@Pick requires a type argument T and at-least one field names to be picked from T.""")
    }
    val argumentType: Type = {
      val macroTypeWithArguments          = c.typecheck(q"${c.prefix.tree}").tpe
      val annotationClass: ClassSymbol    = macroTypeWithArguments.typeSymbol.asClass
      val annotationTypePlaceholder: Type = annotationClass.typeParams.head.asType.toType
      annotationTypePlaceholder.asSeenFrom(macroTypeWithArguments, annotationClass)
    }
    if (argumentType.finalResultType == c.typeOf[Nothing]) {
      bail("Type parameter T must be provided")
    }

    val inputs = annottees.map(_.tree).toList
    if (!inputs.headOption.exists(_.isInstanceOf[ClassDef])) {
      bail("Can annotate only trait")
    }
    def toPick(s: Symbol, isJsNative: Boolean): Tree = {
      val decodedName = s.name.decodedName.toString
      val name        = TermName(decodedName)
      if (!specifiedFieldNames.contains(decodedName)) {
        EmptyTree
      } else {
        val stringRep = s.toString
        if (stringRep.startsWith("variable ")) {
          val m       = s.asMethod
          val retType = m.returnType
          if (isJsNative) {
            q"var $name: $retType = scala.scalajs.js.native"
          } else {
            q"var $name: $retType"
          }
        } else if (stringRep.startsWith("value ")) {
          val tpt = s.typeSignature
          if (isJsNative) {
            q"val $name: $tpt = scala.scalajs.js.native"
          } else {
            q"val $name: $tpt"
          }
        } else {
          val m = s.asMethod
          val paramss = m.paramLists.map(_.map(param => {
            internal.valDef(param)
          }))
          val retType = m.returnType
          if (isJsNative) {
            q"def $name (...$paramss): $retType = scala.scalajs.js.native"
          } else {
            q"def $name (...$paramss): $retType"
          }
        }
      }
    }
    annottees.map(_.tree) match {
      case List(
          q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$ownMembers }"
          ) =>
        val isJsNative = mods.annotations.exists {
          case q"new scala.scalajs.js.native()" => true
          case q"new scalajs.js.native()"       => true
          case q"new js.native()"               => true
          case _                                => false
        }
        val inheritedMembers =
          // maybe decls instead of members?
          (argumentType.members.toSet -- c.typeOf[js.Object].members.toSet).toList
            .filterNot(_.isConstructor)
            .sortBy(_.name.decodedName.toString)
        val partialMembers = inheritedMembers.map(s => toPick(s, isJsNative))
        c.Expr[Any](q"""
          $mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => 
            ..$ownMembers
            ..$partialMembers
          }
        """)
      case _ => bail("Must be a trait")
    }
  }
}
