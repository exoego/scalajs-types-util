# scalajs-types-util

Scala.js types utility to facilitate common type transformations

## Support matrix

|            |   ScalaJS 0.6.28+                     |   ScalaJS 1.x      |
| ---------- | :-----------------------------------: | :----------------: |
| Scala 2.13 | :heavy_check_mark: (v0.3.0 was final) | :heavy_check_mark: |
| Scala 2.12 | :heavy_check_mark: (v0.3.0 was final) | :heavy_check_mark: |
| Scala 2.11 |         N/A                           |       N/A          |
| Scala 2.10 |         N/A                           |       N/A          |

## How to use

Add below line to your SBT project.

```sbt
libraryDependencies += "net.exoego" %%% "scalajs-types-util" % "0.3.0"
```

### Factory macro

`@Factory` macro creates a highly-optimized factory method for JS trait, just like normal case classes.
Each factory methods are added to a corresponding companion object (if not exist, created automatically).

JS trait is generally lighter and faster than JS class, since plain old JS object can be trait, but class need extra overheads.
However, creating a instance of JS trait in Scala.js is a bit error-prone or verbose.
`@Factory` macro improves the situation !

See how to use it.

```scala
import scala.scalajs.js
import net.exoego.scalajs.types.util.Factory

@Factory
trait Foo extends js.Object {
  var x: Int
  var y: js.UndefOr[String]
}

val f = Foo(x = 1)
assert(f.x === 1)
assert(f.y === js.undefined)
```

Type aliases are also supported.

```scala
import scala.scalajs.js
import net.exoego.scalajs.types.util.Factory

@Factory
trait Foo extends js.Object {
  var x: Foo.X
  var y: Foo.Y
}
object Foo {
  type X = Int
  type y = js.UndefOr[String]
}

val f = Foo(x = 1)
assert(f.x === 1)
assert(f.y === js.undefined)
```

#### Inlining factory method or not

By default, factory methods will be inlined (marked with `@inline` annotation), so companion object may be
completely removed in `fullOptStage`.
Inlining may reduce the size of generated JS for many cases, but if same factory methods are used many times, may be inlining increase JS size.
In such case, you may avoid inlinining by setting `inline` parameter to `false`.

```scala
@Factory(inline = false)
trait Base extends js.Object {
  var foo: String
}
```
  
#### Limitation 

If trait is defined inside object, `isTopLevel` argument must be `false`.
 
```scala
object Outer {
    @Factory(isTopLevel = false)
    trait Base extends js.Object {
      var foo: String
    }
}
```

