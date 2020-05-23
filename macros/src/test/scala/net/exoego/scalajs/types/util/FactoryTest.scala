package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class FactoryTest extends AnyFlatSpec with Matchers {
  "Factory macro" should "not compile when NOT applied to a trait" in {
    """@Factory def x: String = "a"""" shouldNot compile
    """@Factory var x: String = "a"""" shouldNot compile
    """@Factory val x: String = "a"""" shouldNot compile
    """@Factory class X""" shouldNot compile
    """@Factory object X""" shouldNot compile
    """def x(@Factory y: X): String = "a"""" shouldNot compile
  }

  it should "compile when applied to a Scala.js-native trait" in {
    """@Factory @js.native trait X""" should compile
    """@Factory @js.native trait X {}""" should compile
  }

  it should "also compile when applied to a Scala-native trait" in {
    """@Factory trait X""" should compile
    """@Factory trait X {}""" should compile
  }

  "factory method " should "have defined parameter" in {
    """ val a: Target = Target(name = "yay")
      | """.stripMargin should compile
    """ val a: Target = Target()
      | """.stripMargin shouldNot compile

    """ val a: TargetScalaNative = TargetScalaNative(name = "yay")
      | """.stripMargin should compile
    """ val a: TargetScalaNative = TargetScalaNative()
      | """.stripMargin shouldNot compile
  }

  it should "allow omitting optional parameters" in {
    """ val a: Target = Target(name = "yay")
      | """.stripMargin should compile
  }

  it should "have parameters for variable and value members" in {
    """ val a: Target = Target(name = "yay", age = 1, hoge = true)
      | """.stripMargin should compile
  }

  it should "not not have parameter for def" in {
    """ val a: Target = Target(name = "yay", foo = "FAIL")
      | """.stripMargin shouldNot compile
  }

  it should "be added to the existing companion object" in {
    """ val a: Existing = Existing(name = "yay")
        | """.stripMargin should compile
  }

  it should "be added to the companion object with nested member" ignore {
    """ val a: Nested = Nested(name = "yay")
      | """.stripMargin should compile
  }
}

@Factory
@js.native
trait Target extends js.Object {
  val hoge: js.UndefOr[Boolean] = js.native
  var age: js.UndefOr[Int]      = js.native
  var name: String              = js.native

  def foo: String = js.native
}

@Factory
trait TargetScalaNative extends js.Object {
  val hoge: js.UndefOr[Boolean]
  var age: js.UndefOr[Int]
  var name: String

  def foo: String
}

@Factory
@js.native
trait Existing extends js.Object {
  var name: String = js.native
}
object Existing {
  val Z = "yay"
}

@Factory
@js.native
trait Nested extends js.Object {
  var name: Nested.Z = js.native
}
object Nested {
  type Z = String
}
