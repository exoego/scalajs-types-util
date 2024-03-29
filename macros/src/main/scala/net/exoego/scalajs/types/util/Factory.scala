package net.exoego.scalajs.types.util

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.scalajs.js

/** Generate a case-class-like companion object with factory method, or add such method to the existing companion
  * object. This might be helpful if you want to instantiate JS traits, especially it is a native (since a
  * Scala.js-defined JS class cannot directly extend it).
  *
  * If the below code given,
  *
  * {{{
  * @Factory
  * trait Base extends js.Object {
  *   var foo: String
  *   val bar: js.UndefOr[js.Array[Int]]
  *   def buz(x: String): Boolean
  * }
  * }}}
  *
  * it is identical to
  *
  * {{{
  * trait Base extends js.Object {
  *   var foo: String
  *   val bar: js.UndefOr[js.Array[Int]]
  *   def buz(x: String): Boolean
  * }
  *
  * object Base {
  *   @inline
  *   def apply(
  *     foo: String,
  *     bar: js.UndefOr[js.Array[Int]] = js.undefined,
  *   ): Base = ...
  * }
  * }}}
  *
  * so you can use factory methods like
  *
  * {{{
  * val o1 = Existing(name = "yay")
  *
  * // instead of...
  * val o2 = new Existing() {
  *   override var name = "omg"
  * }
  * }}}
  *
  * Known limitation: If used inside object, `isTopLevel` argument must be `false`.
  *
  * {{{
  * object Outer {
  *   @Factory(isTopLevel = false)
  *   trait Base extends js.Object {
  *     var foo: String
  *   }
  * }
  * }}}
  *
  * By default, factory methods will be inlined (marked with `@inline` annotation), so companion object may be
  * completely removed in fullOptStage You may avoid inlinining by setting `inline` parameter to `false`.
  *
  * {{{
  * object Outer {
  *   @Factory(inline = false)
  *   trait Base extends js.Object {
  *     var foo: String
  *   }
  * }
  * }}}
  */
@compileTimeOnly("Enable macro to expad this macro annotation")
class Factory(isTopLevel: Boolean = true, inline: Boolean = true) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro FactoryImpl.impl
}

private[util] object FactoryImpl {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context: whitebox.Context = c

    annotteeShouldBeTrait(c)(annottees)

    def addFactoryMethod(cd: ClassDef, md: ModuleDef, isJsNative: Boolean): ModuleDef = {
      val (isTopLevel: Boolean, addInline: Boolean) = c.prefix.tree match {
        case q"new Factory($isToplLevel, $inline)" =>
          (c.eval[Boolean](c.Expr(isToplLevel)), c.eval[Boolean](c.Expr(inline)))
        case q"new Factory($isToplLevel)" => (c.eval[Boolean](c.Expr(isToplLevel)), true)
        case q"new Factory()"             => (true, true)
        case _                            => bail("unexpected annotation pattern!")
      }
      val classType: Option[c.universe.Type] =
        try {
          Option(
            c.typecheck(cd, silent = true, mode = if (isTopLevel) c.TERMmode else c.TYPEmode).symbol.asClass.toType
          )
            .filter(_ != NoType)
        } catch {
          case _: Throwable =>
            info(
              s"@Factory macro failed to type check the trait `${cd.name}` so inherited members are not added to factory method."
            )
            None
        }
      val notExtendingJsObject = !classType.exists(_.baseClasses.exists(_ == c.symbolOf[js.Object])) && !cd.impl.parents
        .map(_.toString)
        .exists(_.endsWith("js.Object"))
      if (notExtendingJsObject) {
        bail(
          "Trait must extends scala.scalajs.js.Object. It is required to explicitly extends (trait X extends js.Object with Y) if inside companion object"
        )
      }
      val inheritedMembers: Seq[ValDef] = classType match {
        case None => Seq.empty
        case Some(traitType) =>
          val list: Seq[Symbol] = (traitType.members.toSet -- c.typeOf[js.Object].members.toSet).toList
          list
            .filterNot(_.isConstructor)
            .filterNot { s =>
              // TODO: Do not rely on string rep since toString is slow
              val rep = s.toString
              rep.startsWith("method ") || rep.startsWith("variable ") && s.asMethod.returnType == c.typeOf[Unit]
            }
            .map(syn => {
              val isDefined = !(syn.typeSignature.finalResultType <:< c.typeOf[js.UndefOr[_]])
              val rhs       = if (isDefined) EmptyTree else q"scala.scalajs.js.undefined"
              ValDef(Modifiers(), syn.name.toTermName, tq"${syn.typeSignature.finalResultType}", rhs)
            })
      }

      val typeParams = cd.tparams.map(_.name.toTypeName)
      def symbolToJSKeyName(s: ValDef): String = {
        val jsName = s.mods.annotations.collectFirst {
          case t if t.toString.contains("JSName(") =>
            t.children.tail.head
        }
        jsName match {
          case Some(Literal(Constant(name: String))) => name
          case _                                     => s.name.decodedName.toTermName.toString
        }
      }

      val members: Seq[(Boolean, ValDef)] = {
        val ownValues = cd.impl.body.collect { case x: ValDef => x }
        distinctBy(ownValues ++ inheritedMembers)(_.name)
          .map { tree =>
            val tpes      = tree.tpt.toString()
            val isDefined = !(tpes.startsWith("js.UndefOr") || tpes.startsWith("scala.scalajs.js.UndefOr"))
            (isDefined, tree)
          }
          .sortBy(p => (!p._1, p._2.name.toString))
      }

      val impl = {
        val groupedByDefined = members.groupBy(_._1)
        val requiredArgs: Seq[c.universe.Apply] = groupedByDefined.getOrElse(true, Seq.empty).map { case (_, s) =>
          val memberName = s.name.toTermName
          val jsKeyName  = symbolToJSKeyName(s)
          q"${jsKeyName} -> $memberName.asInstanceOf[scala.scalajs.js.Any]".asInstanceOf[Apply]
        }
        val optionalArgs = groupedByDefined.getOrElse(false, Seq.empty).map { case (_, s) =>
          val memberName = s.name.toTermName
          val jsKeyName  = symbolToJSKeyName(s)
          q"$memberName.foreach(v$$ => obj$$.updateDynamic(${jsKeyName})(v$$.asInstanceOf[js.Any])) "
            .asInstanceOf[Apply]
        }
        q"""
           val obj$$ = scala.scalajs.js.Dynamic.literal(
             ..$requiredArgs
           )
           ..$optionalArgs
           obj$$.asInstanceOf[${md.name.toTypeName}[..${typeParams}]]
         """
      }
      val arguments = members
        .map { case (isDefined, s) =>
          val name       = s.name.toTermName
          val returnType = s.tpt
          val rhs        = if (isDefined) EmptyTree else q"scala.scalajs.js.undefined"
          ValDef(Modifiers(Flag.PARAM), name, q"${returnType}", rhs)
        }
        .filter(_.nonEmpty)
      val factoryMethod = if (addInline) {
        q"""
        @inline
        def apply[..${cd.tparams}](
          ..$arguments
        ): ${md.name.toTypeName}[..${typeParams}] = { ..${impl} }
        """
      } else {
        q"""
        def apply[..${cd.tparams}](
          ..$arguments
        ): ${md.name.toTypeName}[..${typeParams}] = { ..${impl} }
        """
      }
      ModuleDef(
        md.mods,
        md.name,
        Template(
          md.impl.parents,
          noSelfType,
          md.impl.body ++ List(factoryMethod)
        )
      )
    }
    val inputs: List[Tree] = annottees.map(_.tree).toList
    val outputs: List[Tree] = inputs match {
      case (cd @ ClassDef(classMod, cName, _, _)) :: tail =>
        val moduleDef: ModuleDef = tail match {
          case (existingCompanionObject @ ModuleDef(_, mName, _)) :: Nil if cName.toTermName == mName =>
            existingCompanionObject
          case Nil =>
            var moduleModFlags = NoFlags
            if (classMod.hasFlag(Flag.PRIVATE)) moduleModFlags |= Flag.PRIVATE
            if (classMod.hasFlag(Flag.PROTECTED)) moduleModFlags |= Flag.PROTECTED
            if (classMod.hasFlag(Flag.LOCAL)) moduleModFlags |= Flag.LOCAL
            val moduleMod  = Modifiers(moduleModFlags, classMod.privateWithin, Nil)
            val moduleName = cName.toTermName
            q"$moduleMod object $moduleName".asInstanceOf[ModuleDef]
          case _ => bail("Expected a companion object")
        }

        val isJsNative = isScalaJsNative(c)(classMod)
        cd :: addFactoryMethod(cd, moduleDef, isJsNative) :: Nil
      case _ => bail("Must annotate a trait")
    }
    c.Expr[Any](Block(outputs, Literal(Constant(()))))
  }

  // Workaround for Scala 2.12, which does not have Seq.distinctBy
  private def distinctBy[A, B](source: Seq[A])(op: A => B): Seq[A] = {
    val seen = scala.collection.mutable.HashSet.empty[B]
    source.flatMap { a =>
      val b = op(a)
      if (seen.contains(b)) {
        None
      } else {
        seen.add(b)
        Some(a)
      }
    }
  }
}
