package net.exoego.scalajs.types.util

import scala.scalajs.js

@js.native
trait Foo extends js.Object {
  var name: String        = js.native
  val x: js.Array[Int]    = js.native
  def bar(x: String): Int = js.native
}

trait Bar extends js.Object {
  var name: String
  val x: js.Array[Int]
  def bar(x: String): Int
}

@js.native
trait OptionalFoo extends js.Object {
  var a: js.UndefOr[Int]    = js.native
  val b: js.UndefOr[String] = js.native
  var c: Boolean            = js.native
}

trait OptionalBar extends js.Object {
  var a: js.UndefOr[Int]
  val b: js.UndefOr[String]
  var c: Boolean
}
