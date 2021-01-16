package net.exoego.scalajs.types.util

import scala.annotation.{StaticAnnotation, compileTimeOnly}

@compileTimeOnly("Enable macro to expad this macro annotation")
class Factory(isTopLevel: Boolean = true, inline: Boolean = true) extends StaticAnnotation
