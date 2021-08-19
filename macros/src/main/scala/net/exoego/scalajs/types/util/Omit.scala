package net.exoego.scalajs.types.util

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.scalajs.js

/** Enrich the annotated type with all members of `T` except the omitted members .
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
  * @Omit[Base]("bar")
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
  *     // From `Base`, except the omitted `bar`
  *     var foo: String = js.native
  *     def buz(x: String): Boolean = js.native
  *   }
  * }}}
  *
  * @tparam T
  */
@compileTimeOnly("Enable macro to expand this macro annotation")
class Omit[T <: js.Object](keys: String*) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Omit.impl
}

object Omit {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context: whitebox.Context = c

    val specifiedFieldNames: Set[String] = c.prefix.tree match {
      case q"new Omit[$a](..$b)" => b.map(_.toString.drop(1).dropRight(1)).toSet
      case _ => bail("""@Omit requires a type argument T and at-least one field names to be picked from T.""")
    }
    val argumentType = getArgumentType[Type]()
    annotteeShouldBeTrait(c)(annottees)

    def toPick(s: Symbol, isJsNative: Boolean): Tree = {
      val decodedName = s.name.decodedName.toString
      val name        = TermName(decodedName)
      if (specifiedFieldNames.contains(decodedName)) {
        EmptyTree
      } else {
        val stringRep = s.toString
        if (stringRep.startsWith("variable ")) {
          val m       = s.asMethod
          val retType = m.returnType
          nativeIfNeeded(c)(q"var $name: $retType", isJsNative)
        } else if (stringRep.startsWith("value ")) {
          val tpt = s.typeSignature
          nativeIfNeeded(c)(q"val $name: $tpt", isJsNative)
        } else {
          val m = s.asMethod
          val paramss = m.paramLists.map(_.map(param => {
            internal.valDef(param)
          }))
          val retType = m.returnType
          nativeIfNeeded(c)(q"def $name (...$paramss): $retType", isJsNative)
        }
      }
    }
    annottees.map(_.tree) match {
      case List(
            q"$mods trait $tpname[..$tparams] extends { ..$earlydefns } with ..$parents { $self => ..$ownMembers }"
          ) =>
        val isJsNative = isScalaJsNative(c)(mods)

        val inheritedMembers = getInheritedMembers(c)(argumentType)
        val partialMembers   = inheritedMembers.map(s => toPick(s, isJsNative))
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
