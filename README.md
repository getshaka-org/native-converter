A [Scala.js](https://www.scala-js.org/) project that makes it easy to convert to and from Json and native JavaScript.

```Scala
import scala.scalajs.js
import org.getshaka.nativeconverter.{NativeConverter, fromJson, fromNative}

case class User(name: String, isAdmin: Boolean, age: Int) derives NativeConverter
val u = User("John Smith", true, 42)

// serialize
val json: String = u.toJson
val nativeJsObject: js.Any = u.toNative

// deserialize
val parsedUser: User = json.fromJson[User]
val parsedNativeUser: User = nativeJsObject.fromNative[User]
```

The primary goals are:
1. Easy conversion from case classes and enums to Json Strings.
2. Make interop with native JavaScript libraries easier.
3. High performance and no dependencies.

## Contents
* [Installing](#installing)
* [ScalaDoc](#scaladoc)
* [Built-In NativeConverters](#built-in-nativeconverters)
* [Either vs Exceptions](#either-instead-of-exceptions)
* [Typeclass Derivation](#typeclass-derivation)
* [Cross Building](#cross-building)
* [Performance](#performance)
* [Thanks](#thanks)
* [License](#license)

## Installing
This library requires Scala >= 3.1.0. After [setting up a Scala.js project with SBT](https://www.scala-js.org/doc/tutorial/basic/),

In `/project/plugins.sbt` add the latest sbt-dotty and Scala.js plugin:
```Scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.1")
```

Then in `/build.sbt`, set the scala version and add the native-converter dependency:

```Scala
libraryDependencies ++= Seq(
  "org.getshaka" %%% "native-converter" % "0.5.3"
)
```

## ScalaDoc

[https://javadoc.io/doc/org.getshaka/native-converter_sjs1_3/latest/api/org/getshaka/nativeconverter/NativeConverter.html](https://javadoc.io/doc/org.getshaka/native-converter_sjs1_3/latest/api/org/getshaka/nativeconverter/NativeConverter.html).

## Built-In NativeConverters

Many built-in NativeConverters are already included.

### Primitive Types

You can summon built-in NativeConverters for all the primitive types:

```Scala
val i: Int = NativeConverter[Int].fromNative(JSON.parse("100"))

val nativeByte: js.Any = NativeConverter[Byte].toNative(127.toByte)

val s: String = NativeConverter[String]
  .fromJson(""" "hello world" """)
```

### Either instead of Exceptions

If `fromNative` or `fromJson` fail, a RuntimeException is thrown with a helpful error message. If you prefer `Either` instead, use the `fromNativeE` and `fromJsonE` methods.

```scala
// Left(helpfulErrorMessage)
val e: Either[String, Int] = NativeConverter[Int].fromJsonE(""" "abc" """)
```

### Char, Long, and Overriding the Defaults

Char and Long are always converted to String, since they cannot be represented directly in JavaScript:

```Scala
// native String
val nativeLong = NativeConverter[Long].toNative(Long.MaxValue)

val parsedLong = NativeConverter[Long]
  .fromJson(s""" "${Long.MaxValue}" """)

```

If you want to change this behavior for Long, implement a `given` instance of NativeConverter[Long]. The example below uses String for conversion only when the Long is bigger than Int.

```Scala
  given LongConv: NativeConverter[Long] with
    extension (l: Long) def toNative: js.Any =
      if l > Int.MaxValue || l < Int.MinValue then l.toString
      else l.toInt.asInstanceOf[js.Any]

    def fromNativeE(ps: ParseState): Either[String, Long] =
      ps.json.asInstanceOf[Any] match
        case i: Int => Right(i)
        case s: String => s.toLongOption.toRight(ps.left("Long").value)
        case _ => ps.left("Long")

// "123"
val smallLong: String = NativeConverter[Long].toJson(123L)

// """ "9223372036854775807" """.trim
val bigLong: String = NativeConverter[Long].toJson(Long.MaxValue)
```

Note that we must implement `fromNativeE(ps: ParseState)`. The ParseState is used to build helpful error messages.

### Functions

Functions can be converted between Scala.js and Native:

```Scala
val helloWorld = (name: String) => "hello, " + name

val nativeFunc = NativeConverter[String => String].toNative(helloWorld)

// returns "hello, Ray"
nativeFunc.asInstanceOf[js.Dynamic]("Ray")
```

But remember, Javascript functions are not valid Json and will be not included in `toJson` output.

### IArrays, Arrays, Iterables, Seqs, Sets, and Lists

These collections are serialized using JavaScript Arrays:

```Scala
import scala.collection.{Seq, Set}

val seq = Seq(1, 2, 3)
val set = Set(1, 2, 3)

// "[1,2,3]"
val seqJson = NativeConverter[Seq[Int]].toJson(seq)

// "[1,2,3]"
val setJson = NativeConverter[Set[Int]].toJson(set)
```

### Maps and EsConverters

Maps become JavaScript objects:

```Scala
import scala.collection.Map
import scala.collection.mutable.HashMap

val map = HashMap("a" -> 1, "b" -> 2)

// """ {"a":1,"b":2} """.trim
val mapJson = NativeConverter[Map[String, Int]].toJson(map)
```

Only String keys are supported, since JSON requires String keys. If you'd rather convert to an [ES 2016 Map](https://www.scala-js.org/api/scalajs-library/latest/scala/scalajs/js/Map.html), do the following:

```Scala
import org.getshaka.nativeconverter.EsConverters.given

val map = HashMap(1 -> 2, 3 -> 4)

val nativeMap = NativeConverter[Map[Int, Int]].toNative(map)

// returns 4
nativeMap.asInstanceOf[js.Dynamic].get(3)
```

Converters are not yet implemented for many native ES types, please file an issue or PR if we're missing one you'd like.

### Option

Option is serialized with null if None, and the converted value if Some.
```Scala
val nc = NativeConverter[Option[Array[Int]]]
val some = Some(Array(1,2,3))

// "[1,2,3]"
val someJson = nc.toJson(some)

// None
val none = nc.fromJson("null")
```

## Typeclass Derivation

Any [Product](https://www.scala-lang.org/api/current/scala/Product.html) or Sum type can derive a NativeConverter. Product types are serialized into objects with the parameter names as keys. Simple Sum types (ie, non-parameterized [enums](https://dotty.epfl.ch/docs/reference/enums/enums.html) and sealed hierarchies) are serialized using their (short) type name. Other Sum types are serialized and deserialized using a `@type` property that equals the (short) type name.

This behavior closely matches [Jackson](https://github.com/FasterXML/jackson) and other popular libraries, in order to maximize compatibility.

You can for example redefine Option as a Scala 3 enum:

```Scala
enum Opt[+T] derives NativeConverter:
  case Sm(x: T)
  case Nn

// """ {"@type":"Nn"} """.trim
val nnJson = Opt.Nn.toJson

// Opt.Sm(123L)
val sm = """ {"x":123,"@type":"Sm"} """.fromJson[Opt[Long]]
```

And of course, you can nest to any depth you wish:

```Scala
// recommended but not required for X to derive NativeConverter
case class X(a: List[String]) 
case class Y(b: Option[X]) derives NativeConverter

val y = Y(Some(X(List())))
val yStr = """ {"b":{"a":[]}} """.trim

assertEquals(yStr, y.toJson)

assertEquals(y, yStr.fromJson[Y])
```

## Cross Building

If [Cross Building](https://www.scala-js.org/doc/project/cross-build.html) your Scala project you can use one language for both frontend and backend development. Sub-project `/jvm` will have your JVM sources, `/js` your JavaScript, and in `/shared` you can define all of your validations and request/response DTOs once. NativeConverter only works in JS, and will throw `UnsupportedOperationException` when used on the JVM.

Make sure to add the `native-converter` library dependency to both the JVM and JS projects, otherwise `/shared` won't compile.

Here is a sample cross-project you can clone: [https://github.com/AugustNagro/native-converter-crossproject](https://github.com/AugustNagro/native-converter-crossproject)

## Performance

But what about performance, surely making your own js.Object subclasses is faster?
Nope, derived NativeDecoders are 2x faster, even for simple cases like `User("John Smith", true, 42)`:

![bench](native-converter-vs-js-object-bench.png)

The generated JavaScript code is very clean. This is all possible because of Scala 3's [`inline`](https://dotty.epfl.ch/docs/reference/metaprogramming/inline.html) keyword, and powerful type-level programming capabilities. That's right.. no Macros used whatsoever! The `derives` keyword on type T causes the NativeConverter Typeclass to be auto-generated in T's companion object. Only once, and when first requested.

## Thanks

It is safe to say that Scala 3 is very impressive. And a big thank you to Sébastien Doeraene and Tobias Schlatter, who are first-rate maintainers of Scala.js, as well as Jamie Thompson who provided advice on the conversion of Sum types.

## License
https://www.apache.org/licenses/LICENSE-2.0
