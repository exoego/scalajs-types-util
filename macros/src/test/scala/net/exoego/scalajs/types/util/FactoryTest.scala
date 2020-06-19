package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

class FactoryTest extends AnyFlatSpec with Matchers {
  "Factory macro" should "not compile when NOT applied to a trait" in {
    """@Factory def x: String = "a"""" shouldNot compile
    """@Factory var x: String = "a"""" shouldNot compile
    """@Factory val x: String = "a"""" shouldNot compile
    """@Factory class X""" shouldNot compile
    """@Factory object X""" shouldNot compile
    """def x(@Factory y: X): String = "a"""" shouldNot compile
  }

  it should "compile when applied to a Scala.js-native trait extending js.Object" in {
    """@Factory @js.native trait X extends js.Object""" should compile
    """@Factory @js.native trait X extends js.Object {}""" should compile
    """@Factory @js.native trait X extends scala.scalajs.js.Object {}""" should compile
  }

  it should "not compile when applied to a trait not extending js.Object" in {
    """@Factory @js.native trait X""" shouldNot compile
    """@Factory @js.native trait X {}""" shouldNot compile
  }

  it should "also compile when applied to a Scala-native trait extending js.Object" in {
    """@Factory trait X extends js.Object""" should compile
    """@Factory trait X extends js.Object {}""" should compile
    """@Factory trait X extends scala.scalajs.js.Object {}""" should compile
  }

  it should "not compile when applied to a Scala-native trait not extending js.Object" in {
    """@Factory trait X""" shouldNot compile
    """@Factory trait X {}""" shouldNot compile
    """@Factory trait X extends Seq[Int]""" shouldNot compile
  }

  it should "support referencing nested trait/classes" in {
    """val o: GenericTrait[String, Int] = GenericTrait[String, Int](a = "yay", b = 42)""" should compile
  }

  it should "support generic types" in {
    """val o: GenericTrait[String, Int] = GenericTrait[String, Int](a = "yay", b = 42)""" should compile
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

  it should "have backquoted member as parameter" in {
    """ val a: Target = Target(name = "yay", `type` = "wow")
      | val x: String = a.`type`.getOrElse("")
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

  it should "be added to the companion object with nested member" in {
    """ val a: Nested = Nested(name = "yay", foo = 42, x = ???, y = new Nested.Y)
      | """.stripMargin should compile
  }

  it should "have own members as parameter" in {
    """ val a: Inherited = Inherited(own = 42)
      | """.stripMargin shouldNot compile
  }

  it should "have inherited members as parameter" in {
    """ val a: Inherited = Inherited(name= "yay", own = 42, type_ = "wow")
      | val x: String = a.name
      | val y: Int = a.own
      | """.stripMargin should compile
  }
}

@Factory
@js.native
trait Target extends js.Object {
  val hoge: js.UndefOr[Boolean]  = js.native
  var age: js.UndefOr[Int]       = js.native
  var name: String               = js.native
  var `type`: js.UndefOr[String] = js.native

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

@js.native
trait Hoge extends js.Object {
  var foo: Int = js.native
}

@Factory
@js.native
trait Nested extends js.Object {
  var foo: Int       = js.native
  var name: Nested.Z = js.native
  var x: Nested.X    = js.native
  var y: Nested.Y    = js.native
}
object Nested {
  type Z = String
  trait X extends js.Object
  class Y extends js.Object
}

@Factory
trait Inherited extends js.Object with TargetScalaNative {
  var own: Int
  @JSName("type") var type_ : String
  @JSName("NAMED") var named: js.UndefOr[String] = js.undefined
}

@Factory
trait GenericTrait[A, B] extends js.Object {
  val a: A
  val b: B
}
