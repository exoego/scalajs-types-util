package net.exoego.scalajs.types.util

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.scalajs.js

/**
  * Generate a case-class-like companion object with factory method, or add such method to the existing companion object.
  * This might be helpful if you want to instantiate JS traits, especially it is a native (since a Scala.js-defined
  * JS class cannot directly extend it).
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
  *
  */
class Factory extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Factory.impl
}

object Factory {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._
    import Helper._
    implicit val context: blackbox.Context = c

    annotteeShouldBeTrait(c)(annottees)

    def addFactoryMethod(rawCd: ClassDef, md: ModuleDef, isJsNative: Boolean): ModuleDef = {
      val cd = if (!md.impl.exists(_.isInstanceOf[TypeDef])) {
        rawCd
      } else {
        val aliasToTypes = md.impl.collect {
          case td: TypeDef => s"${rawCd.name}.${td.name}" -> td.children.head
        }.toMap
        ClassDef(
          mods = rawCd.mods,
          name = rawCd.name,
          tparams = rawCd.tparams,
          impl = Template(
            parents = rawCd.impl.parents,
            self = rawCd.impl.self,
            body = rawCd.impl.body.map {
              case vd: ValDef if aliasToTypes.contains(vd.tpt.toString) =>
                ValDef(
                  mods = vd.mods,
                  name = vd.name,
                  tpt = aliasToTypes(vd.tpt.toString),
                  rhs = vd.rhs
                )
              case otherwise => otherwise
            }
          )
        )
      }
      val traitType = c.typecheck(cd).symbol.asClass.toType
      if (!traitType.baseClasses.contains(c.symbolOf[js.Object])) {
        bail("Trait must extends scala.scalajs.js.Object.")
      }
      val jsNameType = c.typeOf[js.annotation.JSName]
      def symbolToJSKeyName(s: Symbol): String = {
        val jsName = s.annotations.collectFirst {
          case t if t.tree.tpe == jsNameType =>
            t.tree.children.tail.head
        }
        jsName match {
          case Some(Literal(Constant(name: String))) => name
          case None                                  => s.name.toTermName.toString
        }
      }
      val members: Seq[(Boolean, Symbol)] =
        (traitType.members.toSet -- c.typeOf[js.Object].members.toSet).toList
          .filterNot(_.isConstructor)
          .map(s => {
            val isDefined = !(s.typeSignature.finalResultType <:< c.typeOf[js.UndefOr[_]])
            (isDefined, s)
          })
          .filterNot {
            case (_, s) =>
              // TODO: Do not rely on string rep since toString is slow
              val rep = s.toString
              rep.startsWith("method ") || rep.startsWith("variable ") && s.asMethod.returnType == c.typeOf[Unit]
          }
          .sortBy(p => (!p._1, p._2.name.toString))
      val impl = {
        val groupedByDefined = members.groupBy(_._1)
        val requiredArgs: Seq[c.universe.Apply] = groupedByDefined.getOrElse(true, Seq.empty).map {
          case (_, s) =>
            val memberName = s.name.toTermName
            val jsKeyName  = symbolToJSKeyName(s)
            q"${jsKeyName} -> $memberName.asInstanceOf[scala.scalajs.js.Any]".asInstanceOf[Apply]
        }
        val optionalArgs = groupedByDefined.getOrElse(false, Seq.empty).map {
          case (_, s) =>
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
           obj$$.asInstanceOf[${md.name.toTypeName}]
         """
      }
      val arguments = members
        .map {
          case (isDefined, s) =>
            val name      = TermName(s.name.decodedName.toString)
            val stringRep = s.toString
            if (!stringRep.startsWith("value ") && !stringRep.startsWith("variable ")) {
              EmptyTree
            } else {
              val returnType = s.asMethod.returnType
              if (isDefined) {
                q"${name}: ${returnType}"
              } else {
                ValDef(Modifiers(), name, q"${returnType}", q"scala.scalajs.js.undefined")
              }
            }
        }
        .filter(_.nonEmpty)
      ModuleDef(
        md.mods,
        md.name,
        Template(
          md.impl.parents,
          noSelfType,
          md.impl.body ++ List(
            q"""
            def apply(
              ..$arguments
            ): ${md.name.toTypeName} = { ..$impl }
            """
          )
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
}
