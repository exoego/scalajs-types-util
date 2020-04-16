package net.exoego.scalajs.types.util

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.scalajs.js

/**
  * Enrich the annotated type with all properties of `T` set to optional (wrapped with `js.UndefOr`).
  * Methods ('def`) are also added to the annotated type, but the return type is not modified.
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
  * @Partial[Base]
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
  *     // Added from `Base`
  *     var foo: js.UndefOr[String] = js.native
  *     val bar: js.UndefOr[js.Array[Int]] = js.native
  *     def buz(x: String): Boolean = js.native
  *   }
  * }}}
  *
  * @tparam T
  */
class Partial[T <: js.Object] extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Partial.impl
}

object Partial {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context = c

    def toPartial(s: Symbol, isJsNative: Boolean): Tree = {
      val name      = TermName(s.name.decodedName.toString)
      val stringRep = s.toString
      if (stringRep.startsWith("value ")) {
        val tpt = s.typeSignature
        nativeIfNeeded(c)(q"val $name: js.UndefOr[$tpt]", isJsNative)
      } else if (stringRep.startsWith("variable ")) {
        val m       = s.asMethod
        val retType = m.returnType
        if (retType == c.typeOf[Unit]) {
          EmptyTree
        } else {
          nativeIfNeeded(c)(q"var $name: js.UndefOr[$retType]", isJsNative)
        }
      } else {
        val m = s.asMethod
        val paramss = m.paramLists.map(_.map(param => {
          internal.valDef(param)
        }))
        val retType = m.returnType
        nativeIfNeeded(c)(q"def $name(...$paramss): $retType", isJsNative)
      }
    }

    val argumentType = getArgumentType[Type]()
    annotteeShouldBeTrait(c)(annottees)

    annottees.map(_.tree) match {
      case List(
          q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$ownMembers }"
          ) =>
        val isJsNative       = isScalaJsNative(c)(mods)
        val inheritedMembers = getInheritedMembers(c)(argumentType)
        val partialMembers   = inheritedMembers.map(s => toPartial(s, isJsNative))
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
