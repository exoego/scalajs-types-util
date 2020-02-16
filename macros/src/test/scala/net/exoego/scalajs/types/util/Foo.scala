package net.exoego.scalajs.types.util

import scala.scalajs.js
@js.native
trait Foo extends js.Object {
  var name: String        = js.native
  val x: js.Array[Int]    = js.native
  def bar(x: String): Int = js.native
}
