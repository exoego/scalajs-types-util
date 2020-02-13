package examples

import net.exoego.scalajs.types.util.Partial

import scala.scalajs.js


object Hoge {
  def main(args: Array[String]): Unit = {
        val p: PartialFoo3 = ???
    println(p.immut.isDefined)
    List.empty
    
//    println(p.name)
  }
}

@js.native
trait Foo extends js.Object {
  var age: Int = js.native
  val immut: js.Array[Int] = js.native
  
  def i_am_method(x: String, y: Boolean): Int = js.native
}

@Partial[Foo]
@js.native
trait PartialFoo1 extends js.Object

@Partial[Foo]
@js.native
trait PartialFoo2 extends js.Object {
  var own: Boolean = js.native
}

@Partial[Foo]
@scala.scalajs.js.native
trait PartialFoo3 extends js.Object {
  var own: Boolean = js.native
}
