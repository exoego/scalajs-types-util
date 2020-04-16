package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class RequiredTest extends AnyFlatSpec with Matchers {
  "Partial macro" should "not compile when NOT applied to a trait" in {
    """@Required def x: String = "a"""" shouldNot compile
    """@Required var x: String = "a"""" shouldNot compile
    """@Required val x: String = "a"""" shouldNot compile
    """@Required class X""" shouldNot compile
    """@Required object X""" shouldNot compile
    """@Required def x(@Required y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@Required trait X {}""" shouldNot compile
  }

  it should "compile when applied to a trait with type argument" in {
    """ @Required[Foo] trait Y {}""".stripMargin should compile
    """ @Required[Bar] trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: RequiredFoo = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
    """ val a: RequiredBar = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have inherited property unwrapped with js.UndefOr" in {
    """ val a: RequiredFoo = ???
      | val b: Int = a.a
      | val c: String = a.b
      | val d: Boolean = a.c
      | """.stripMargin should compile
    """ val a: RequiredBar = ???
      | val b: Int = a.a
      | val c: String = a.b
      | val d: Boolean = a.c
      | """.stripMargin should compile
  }

  it should "allow reassign to var" in {
    """ val a: RequiredFoo = ???
      | a.a = 42
      | """.stripMargin should compile
    """ val a: RequiredBar = ???
      | a.a = 42
      | """.stripMargin should compile
  }
}

@Required[OptionalFoo]
@js.native
trait RequiredFoo extends js.Object {
  var own: Boolean = js.native
}

@Required[OptionalBar]
trait RequiredBar extends js.Object {
  var own: Boolean
}
