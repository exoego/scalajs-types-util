package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class ReadOnlyTest extends AnyFlatSpec with Matchers {
  "ReadOnly macro" should "not compile when NOT applied to a trait" in {
    """@ReadOnly def x: String = "a"""" shouldNot compile
    """@ReadOnly var x: String = "a"""" shouldNot compile
    """@ReadOnly val x: String = "a"""" shouldNot compile
    """@ReadOnly class X""" shouldNot compile
    """@ReadOnly object X""" shouldNot compile
    """def x(@ReadOnly y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@ReadOnly trait X {}""" shouldNot compile
  }

  it should "compile when applied to a trait with type argument" in {
    """ @ReadOnly[Foo] trait Y {}""".stripMargin should compile
    """ @ReadOnly[Bar] trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: ReadOnlyFoo = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
    """ val a: ReadOnlyBar = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have inherited property that is readonly" in {
    """ val a: ReadOnlyFoo = ???
      | val b: String = a.name
      | """.stripMargin should compile
    """ val a: ReadOnlyFoo = ???
      | a.name = ""
      | """.stripMargin shouldNot compile

    """ val a: ReadOnlyBar = ???
      | val b: String = a.name
      | """.stripMargin should compile
    """ val a: ReadOnlyBar = ???
      | a.name = ""
      | """.stripMargin shouldNot compile
  }

  it should "have inherited method" in {
    """ val a: ReadOnlyFoo = ???
      | val b: Int = a.bar("buz")
      | """.stripMargin should compile
    """ val a: ReadOnlyBar = ???
      | val b: Int = a.bar("buz")
      | """.stripMargin should compile
  }
}

@ReadOnly[Foo]
@js.native
trait ReadOnlyFoo extends js.Object {
  var own: Boolean        = js.native
  def buz(x: String): Int = js.native
}

@ReadOnly[Bar]
@js.native
trait ReadOnlyBar extends js.Object {
  var own: Boolean
  def buz(x: String): Int
}
