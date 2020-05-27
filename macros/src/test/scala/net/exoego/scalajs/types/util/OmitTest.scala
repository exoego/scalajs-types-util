package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class OmitTest extends AnyFlatSpec with Matchers {
  "Omit macro" should "not compile when NOT applied to a trait" in {
    """@Omit def x: String = "a"""" shouldNot compile
    """@Omit var x: String = "a"""" shouldNot compile
    """@Omit val x: String = "a"""" shouldNot compile
    """@Omit class X""" shouldNot compile
    """@Omit object X""" shouldNot compile
    """def x(@Omit y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@Omit trait X {}""" shouldNot compile
  }

  it should "not compile when applied to a trait with type argument but no keys" in {
    """ @Omit[Foo] trait Y {}""".stripMargin shouldNot compile
    """ @Omit[Foo]() trait Y {}""".stripMargin shouldNot compile
    """ @Omit[Bar] trait Y {}""".stripMargin shouldNot compile
    """ @Omit[Bar]() trait Y {}""".stripMargin shouldNot compile
  }

  it should "not compile when applied to a trait with type argument and some keys" in {
    """ @Omit[Foo]("") trait Y {}""".stripMargin should compile
    """ @Omit[Bar]("") trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: OmitFoo = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
    """ val a: OmitBar = ???
      | val b: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have all members except omitted" in {
    """ val a: OmitFoo = ???
      | val b: String = a.name
      | """.stripMargin should compile

    """ val a: OmitBar = ???
      | val b: String = a.name
      | """.stripMargin should compile
  }

  it should "not have omitted members" in {
    """ val a: OmitFoo = ???
      | a.x
      | """.stripMargin shouldNot compile
    """ val a: OmitFoo = ???
      | a.bar("yay")
      | """.stripMargin shouldNot compile

    """ val a: OmitBar = ???
      | a.x
      | """.stripMargin shouldNot compile
    """ val a: OmitBar = ???
      | a.bar("yay")
      | """.stripMargin shouldNot compile
  }

  it should "exclude inherited members by name" in {
    """ val a: OmitInherited = ???
      | val y: Int = a.own
      | """.stripMargin shouldNot compile
  }

  it should "include inherited members" in {
    """ val a: OmitInherited = ???
      | val x: String = a.name
      | val y: Int = a.own2
      | """.stripMargin should compile
  }
}

@Omit[Foo]("x", "bar")
@js.native
trait OmitFoo extends js.Object {
  var own: Boolean        = js.native
  def buz(x: String): Int = js.native
}

@Omit[Bar]("x", "bar")
@js.native
trait OmitBar extends js.Object {
  var own: Boolean
  def buz(x: String): Int
}

@Omit[OmitFoo]("own")
@js.native
trait OmitInherited extends OmitFoo {
  var own2: Int = js.native
}
