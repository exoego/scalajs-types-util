package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class PartialTest extends AnyFlatSpec with Matchers {
  "Partial macro" should "not compile when NOT applied to a trait" in {
    """@Partial def x: String = "a"""" shouldNot compile
    """@Partial var x: String = "a"""" shouldNot compile
    """@Partial val x: String = "a"""" shouldNot compile
    """@Partial class X""" shouldNot compile
    """@Partial object X""" shouldNot compile
    """@Partial def x(@Partial y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@Partial trait X {}""" shouldNot compile
  }

  it should "compile when applied to a trait with type argument" in {
    """ @Partial[Foo] trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: PartialFoo = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have inherited property wrapped with js.UndefOr" in {
    """ val a: PartialFoo = ???
      | val b: js.UndefOr[String] = a.name
      | val c: js.UndefOr[js.Array[Int]] = a.x
      | """.stripMargin should compile
  }

  it should "have inherited method" in {
    """ val a: PartialFoo = ???
      | val b: Int = a.bar("buz")
      | """.stripMargin should compile
  }
}

@js.native
trait Foo extends js.Object {
  var name: String = js.native
  val x: js.Array[Int] = js.native
  def bar(x: String): Int = js.native
}

@Partial[Foo]
@js.native
trait PartialFoo extends js.Object {
  var own: Boolean = js.native
  def buz(x: String): Int = js.native
}
