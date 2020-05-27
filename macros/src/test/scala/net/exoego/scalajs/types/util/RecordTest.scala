package net.exoego.scalajs.types.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

class RecordTest extends AnyFlatSpec with Matchers {
  "Record macro" should "not compile when NOT applied to a trait" in {
    """@Record def x: String = "a"""" shouldNot compile
    """@Record var x: String = "a"""" shouldNot compile
    """@Record val x: String = "a"""" shouldNot compile
    """@Record class X""" shouldNot compile
    """@Record object X""" shouldNot compile
    """def x(@Record y: Int): String = "a"""" shouldNot compile
  }

  it should "not compile when applied to a trait without type argument" in {
    """@Record trait X {}""" shouldNot compile
  }

  it should "not compile when applied to a trait with type argument but without keys" in {
    """ @Record[Foo] trait Y {}""".stripMargin shouldNot compile
  }

  it should "not compile when specified key conflict own property" in {
    """ @Record[Foo]("own") trait X { var own: Int } """.stripMargin shouldNot compile
  }

  it should "compile when applied to a trait with type argument and keys" in {
    """ @Record[Foo]("key") trait Y {}""".stripMargin should compile
  }

  it should "have own property as-is" in {
    """ val a: RecordFoo = ???
      | val own: Boolean = a.own
      | """.stripMargin should compile
  }

  it should "have set of properties" in {
    """ val a: RecordFoo = ???
      | val b: PageInfo = a.about
      | val c: PageInfo = a.contact
      | val d: PageInfo = a.home
      | val x: String = d.`type`
      | """.stripMargin should compile
  }

}

trait PageInfo extends js.Object {
  var title: String;
  var `type`: String
}

@Record[PageInfo]("about", "contact", "home")
@js.native
trait RecordFoo extends js.Object {
  var own: Boolean
}
