package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class PickTest extends AnyFlatSpec with Matchers {
  "Pick macro" should "not compile when NOT applied to a trait" in {
    """@Pick def x: String = "a"""" shouldNot compile
    """@Pick var x: String = "a"""" shouldNot compile
    """@Pick val x: String = "a"""" shouldNot compile
    """@Pick class X""" shouldNot compile
    """@Pick object X""" shouldNot compile
    """def x(@Pick y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@Pick trait X {}""" shouldNot compile
  }

  it should "not compile when applied to a trait with type argument but no keys" in {
    """ @Pick[Foo] trait Y {}""".stripMargin shouldNot compile
    """ @Pick[Foo]() trait Y {}""".stripMargin shouldNot compile
    """ @Pick[Bar] trait Y {}""".stripMargin shouldNot compile
    """ @Pick[Bar]() trait Y {}""".stripMargin shouldNot compile
  }

  it should "not compile when applied to a trait with type argument and some keys" in {
    """ @Pick[Foo]("") trait Y {}""".stripMargin should compile
    """ @Pick[Bar]("") trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: PickFoo = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
    """ val a: PickBar = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have picked members" in {
    """ val a: PickFoo = ???
      | val b: String = a.name
      | val c: String = a.`type`
      | """.stripMargin should compile

    """ val a: PickBar = ???
      | val b: String = a.name
      | """.stripMargin should compile

    """ val a: PickFoo2 = ???
      | val b: String = a.name
      | val c: js.Array[Int] = a.x
      | val d: Int = a.bar("yay")
      | """.stripMargin should compile
  }

  it should "not have un-picked members" in {
    """ val a: PickFoo = ???
      | a.x
      | """.stripMargin shouldNot compile
    """ val a: PickFoo = ???
      | a.bar("yay")
      | """.stripMargin shouldNot compile

    """ val a: PickBar = ???
      | a.x
      | """.stripMargin shouldNot compile
    """ val a: PickBar = ???
      | a.bar("yay")
      | """.stripMargin shouldNot compile
  }
}

@Pick[Foo]("name", "type")
@js.native
trait PickFoo extends js.Object {
  var own: Boolean        = js.native
  def buz(x: String): Int = js.native
}

@Pick[Foo]("name", "x", "bar")
@js.native
trait PickFoo2 extends js.Object

@Pick[Bar]("name")
@js.native
trait PickBar extends js.Object {
  var own: Boolean
  def buz(x: String): Int
}
