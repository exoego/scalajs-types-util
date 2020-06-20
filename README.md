# scalajs-types-util

Scala.js types utility to facilitate common type transformations

## Support matrix

|            |   ScalaJS 0.6.28+  |   ScalaJS 1.x      |
| ---------- | :----------------: | :----------------: |
| Scala 2.13 | :heavy_check_mark: | :heavy_check_mark: |
| Scala 2.12 | :heavy_check_mark: | :heavy_check_mark: |
| Scala 2.11 |         N/A        |       N/A          |
| Scala 2.10 |         N/A        |       N/A          |

## How to use

Add below line to your SBT project.

```sbt
libraryDependencies += "net.exoego" %%% "scalajs-types-util" % "0.2.0"
```

```scala
import scala.scalajs.js
import net.exoego.scalajs.types.util.Factory

@Factory
trait Foo extends js.Object {
  var x: Int
  var y: js.UndefOr[String]]
}

val f = Foo(x = 1)
assert(f.x === 1)
assert(f.isEmpty)
```