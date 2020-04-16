package net.exoego.scalajs.types.util

import scala.reflect.macros.blackbox

private[util] object Helper {
  def bail(message: String)(implicit c: blackbox.Context): Nothing = {
    c.abort(c.enclosingPosition, message)
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

}
