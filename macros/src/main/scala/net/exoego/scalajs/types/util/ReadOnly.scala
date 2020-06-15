package net.exoego.scalajs.types.util

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.scalajs.js

/**
  * Enrich the annotated type with all properties of `T` set to readonly (`def field: String` in Scala).
  * Methods are also added to the annotated type.
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
  * @ReadOnly[Base]
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
  *     def foo: String = js.native
  *     def bar: js.Array[Int] = js.native
  *     def buz(x: String): Boolean = js.native
  *   }
  *
  *   val instance: Enriched = ???
  *   instance.foo = "" // Compile error
  * }}}
  *
  * @tparam T
  */
@compileTimeOnly("Enable macro to expand this macro annotation")
class ReadOnly[T <: js.Object] extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ReadOnly.impl
}

object ReadOnly {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context = c

    def toPartial(s: Symbol, isJsNative: Boolean): Tree = {
      val name      = TermName(s.name.decodedName.toString)
      val stringRep = s.toString
      if (stringRep.startsWith("value ")) {
        val tpt = s.typeSignature
        nativeIfNeeded(c)(q"def $name: $tpt", isJsNative)
      } else if (stringRep.startsWith("variable ")) {
        val m = s.asMethod
        val paramss = m.paramLists.map(_.map(param => {
          internal.valDef(param)
        }))
        val retType = m.returnType
        if (retType == c.typeOf[Unit]) {
          // Omit update method for var
          EmptyTree
        } else {
          nativeIfNeeded(c)(q"def $name: $retType", isJsNative)
        }
      } else {
        val m = s.asMethod
        val paramss = m.paramLists.map(_.map(param => {
          internal.valDef(param)
        }))
        val retType = m.returnType
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
