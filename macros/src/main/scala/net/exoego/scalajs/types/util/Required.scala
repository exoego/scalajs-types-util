package net.exoego.scalajs.types.util

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Enrich the annotated type with all properties of `T` set to mandatory (unwrapped with `js.UndefOr`).
  * Methods ('def`) are also added to the annotated type, but the return type is not modified.
  *
  * If the below code given,
  *
  * {{{
  * @js.native
  * trait Base extends js.Object {
  *   var foo1: js.UndefOr[String] = js.native
  *   var foo2: js.UndefOr[Int] = js.native
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
  *     var foo1: String = js.native
  *     var foo2: Int = js.native
  *     val bar: js.Array[Int] = js.native
  *     def buz(x: String): Boolean = js.native
  *   }
  * }}}
  *
  * @tparam T
  */
class Required[T <: js.Object] extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Required.impl
}

object Required {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context = c

    val jsUndefOrSymbol = c.typeOf[scala.scalajs.js.UndefOr[_]].typeSymbol
    def unwrap(retType: c.universe.Type): c.universe.Type = {
      if (retType.typeSymbol == jsUndefOrSymbol) {
        retType.finalResultType.typeArgs.head
      } else {
        retType
      }
    }

    def toRequired(s: Symbol, isJsNative: Boolean): Tree = {
      val name      = TermName(s.name.decodedName.toString)
      val stringRep = s.toString
      if (stringRep.startsWith("value ")) {
        val tpt = unwrap(s.typeSignature)
        nativeIfNeeded(c)(q"val $name: $tpt", isJsNative)
      } else if (stringRep.startsWith("variable ")) {
        val m       = s.asMethod
        val retType = unwrap(m.returnType)
        if (retType == c.typeOf[Unit]) {
          EmptyTree
        } else {
          nativeIfNeeded(c)(q"var $name: $retType", isJsNative)
        }
      } else {
        val m = s.asMethod
        val paramss = m.paramLists.map(_.map(param => {
          internal.valDef(param)
        }))
        val retType = unwrap(m.returnType)
        nativeIfNeeded(c)(q"def $name (...$paramss): $retType", isJsNative)
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
        val partialMembers   = inheritedMembers.map(s => toRequired(s, isJsNative))
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
