import org.getshaka.nativeconverter.*
import org.junit.Assert.*
import org.junit.Test

import scala.collection.mutable.ListBuffer
import scala.collection.{Iterable, Map, Seq, Set, mutable}
import scala.collection.immutable
import scala.scalajs.js
import scala.scalajs.js.JSON

class JsonTests:

  @Test
  def jsonSummonTest: Unit =
    case class Simple(a: Int) derives NativeConverter
    assertEquals(""" {"a":123} """.trim, Simple(123).toJson)
    val fromNative = summon[NativeConverter[Simple]].fromNative(JSON.parse(""" {"a":123}  """))
    assertEquals(123, fromNative.a)

    assertEquals(123, NativeConverter[Int].fromNative(JSON.parse("123")))

  @Test
  def jsonPrimitivesTest: Unit =
    import NativeConverter.given

    assertEquals(""""abcd"""", "abcd".toJson)
    assertEquals("abcd", NativeConverter[String].fromNative(JSON.parse(""""abcd"""")))

    assertEquals("""false""", false.toJson)
    assertEquals(false, """false""".fromJson[Boolean])

    assertEquals("""127""", 127.byteValue.toJson)
    assertEquals(127.toByte, "127".fromJson[Byte])

    assertEquals(Short.MaxValue.toString, Short.MaxValue.toJson)
    assertEquals(123.shortValue, "123".fromJson[Short])

    assertEquals("1", 1.toJson)
    assertEquals(Int.MinValue, Int.MinValue.toString.fromJson[Int])
    try
      Int.MaxValue.toString.fromJson[Short]
      fail("Should not be able to make Short from large Int")
    catch
      case _ => ()

    assertEquals("1.1", JSON.stringify(1.1.toFloat.toNative).substring(0, 3))
    assertEquals(1.1.toFloat, NativeConverter[Float].fromNative(JSON.parse("1.1")), .0001)

    assertEquals(Double.MinValue.toString, Double.MinValue.toJson)
    assertEquals(Double.MaxValue, NativeConverter[Double].fromNative(JSON.parse(Double.MaxValue.toString)), .0001)

    // todo more testing for nulls
    assertEquals("null", JSON.stringify(summon[NativeConverter[Null]].toNative(null)))

    val intC = NativeConverter[Int]
    assertEquals(2, intC.fromNative(intC.fromNative(JSON.parse("2")).toNative.toNative))

  end jsonPrimitivesTest

  @Test
  def jsonOverrideGivenTest: Unit =
    given NativeConverter[Long] with
      extension (t: Long) def toNative: js.Any =
        if t > Int.MaxValue || t < Int.MinValue then t.toString
        else t.toInt.asInstanceOf[js.Any]

      def fromNativeE(ps: ParseState): Either[String, Long] =
        ps.json.asInstanceOf[Any] match
          case i: Int => Right(i)
          case s: String => s.toLongOption.toRight(ps.left("Long in a String").value)
          case _ => ps.left("Long")

    val bigLongStr = s""" "${Long.MaxValue}" """.trim
    assertEquals(Long.MaxValue, bigLongStr.fromJson[Long])
    assertEquals(bigLongStr, Long.MaxValue.toJson)

    assertEquals(123L, "123".fromJson[Long])
    assertEquals("123", 123L.toJson)

  case class SimpleProduct(
    a: String,
    b: Boolean,
    c: Byte,
    d: Int,
    e: Double,
    f: Long
  ) derives NativeConverter

  case object CaseObject
  given NativeConverter[CaseObject.type] = NativeConverter.derived

  @Test
  def jsonSimpleCaseClassTest: Unit =
    val sp = SimpleProduct("hello", true, 127.toByte, 123, 1.11, Long.MaxValue)
    val spJson = sp.toJson
    assertEquals(
      s"""{"a":"hello","b":true,"c":127,"d":123,"e":1.11,"f":"${Long.MaxValue.toString}"}""",
      spJson
    )
    assertEquals(sp, spJson.fromJson[SimpleProduct])

    case class FuncClass(f: String => Long) derives NativeConverter
    val fc = FuncClass(s => Long.MaxValue)
    val nativeFc = fc.toNative.asInstanceOf[js.Dynamic]
    assertEquals(Long.MaxValue, nativeFc.applyDynamic("f")("hello"))
    val newFc = nativeFc.fromNative[FuncClass]
    assertEquals(Long.MaxValue, newFc.f(""))

    val nativeJs = JSON.parse(""" {"native": 1337} """)
    case class ClsWithNative(jsObj: js.Any) derives NativeConverter
    assertEquals(""" {"jsObj":{"native":1337}} """.trim, ClsWithNative(nativeJs).toJson)

    case class A(x: Int)
    case class B(a: A)
    case class C(b: B) derives NativeConverter
    val c = C(B(A(1234)))
    val strC: String = JSON.stringify(c.toNative)
    assertEquals("""{"b":{"a":{"x":1234}}}""", strC)
    assertEquals(c, strC.fromJson[C])

    case class SingletonCaseClass() derives NativeConverter
    assertEquals(""" {} """.trim, JSON.stringify(SingletonCaseClass().toNative))
    assertEquals(SingletonCaseClass(), """ {} """.fromJson[SingletonCaseClass])

    assertEquals(""" {} """.trim, JSON.stringify(CaseObject.toNative))
    assertEquals(CaseObject, """ {} """.fromJson[CaseObject.type])

  end jsonSimpleCaseClassTest

  enum Founder derives NativeConverter:
    case Adams, Washington, Jefferson

  enum Color(val rgb: Int) derives NativeConverter:
    case Red extends Color(0xFF0000)
    case Green extends Color(0x00FF00)
    case Blue extends Color(0x0000FF)

  enum Opt[+T] derives NativeConverter:
    case Sm(x: T)
    case Nn

  sealed trait O[+T] derives NativeConverter
  case class S[T](x: T) extends O[T]
  case object N extends O[Nothing]

  @Test
  def jsonSimpleEnumTest: Unit =
    assertEquals(""" "Adams" """.trim, JSON.stringify(Founder.Adams.toNative))
    assertEquals(Founder.Washington, """ "Washington" """.fromJson[Founder])

    assertEquals(""" "Blue" """.trim, JSON.stringify(Color.Blue.toNative))
    assertEquals(0x00FF00, """ "Green" """.fromJson[Color].rgb)

    assertEquals(""" {"@type":"Nn"} """.trim, JSON.stringify(Opt.Nn.toNative))
    try
      assertEquals(Opt.Nn, """ "Nn" """.fromJson[Opt[Long]])
      fail("@type should be required")
    catch _ => ()
    assertEquals(Opt.Nn, """ {"@type":"Nn"} """.fromJson[Opt[Long]])

    assertEquals(""" {"x":123,"@type":"Sm"} """.trim, JSON.stringify(Opt.Sm(123).toNative))
    assertEquals(
      Opt.Sm(Long.MaxValue),
      s""" {"x": "${Long.MaxValue}", "@type":"Sm"} """.fromJson[Opt[Long]]
    )

    try
      "1.1".fromJson[Opt[Int]]
      fail("should not be able to make Opt[Int] given a Double")
    catch case _ => ()

    assertEquals(""" {"@type":"N"} """.trim, JSON.stringify(N.toNative))
    assertEquals(N, """ {"@type":"N"} """.fromJson[O[String]])
    assertEquals(""" {"x":"123","@type":"S"} """.trim, JSON.stringify(S(123L).toNative))
    assertEquals(S(123L), """ {"x":"123","@type":"S"} """.fromJson[O[Long]])

  @Test
  def jsonCollectionTest: Unit =
    import NativeConverter.given

    val strArr = "[1,2,3]"
    val nativeArr = JSON.parse(strArr)

    val a = Array(1,2,3)
    assertEquals(strArr, JSON.stringify(a.toNative))
    assertTrue(a sameElements NativeConverter[Array[Int]].fromNative(nativeArr))

    val b = Iterable(1,2,3)
    assertEquals(strArr, JSON.stringify(b.toNative))
    assertTrue(b.iterator sameElements NativeConverter[Iterable[Int]].fromNative(nativeArr))

    val c = Seq(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[Seq[Int]].toNative(c)))
    // this is not inferring the right NativeConverter...
//    assertEquals(strArr, JSON.stringify(c.toNative))
    assertTrue(c sameElements NativeConverter[Seq[Int]].fromNative(nativeArr))

    val ci = immutable.Seq(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[immutable.Seq[Int]].toNative(ci)))
    assertTrue(ci sameElements NativeConverter[immutable.Seq[Int]].fromNative(nativeArr))

    val e = List(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[List[Int]].toNative(e)))
    assertTrue(e sameElements NativeConverter[List[Int]].fromNative(nativeArr))

    val f = ListBuffer(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[mutable.Buffer[Int]].toNative(f)))
    assertTrue(f sameElements NativeConverter[mutable.Buffer[Int]].fromNative(nativeArr))

    val g = immutable.Map[String, Int]("a" -> 1, "b" -> 2, "c" -> 3)
    val strMap = """ {"a":1,"b":2,"c":3} """.trim
    assertEquals(strMap, JSON.stringify(NativeConverter[immutable.Map[String, Int]].toNative(g)))
    assertEquals(g, NativeConverter[immutable.Map[String, Int]].fromNative(JSON.parse(strMap)))

    val h = immutable.Set[Int](1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[immutable.Set[Int]].toNative(h)))
    assertEquals(h, NativeConverter[immutable.Set[Int]].fromNative(JSON.parse(strArr)))

    val composite = Array(Map("a" -> Set(1, 2, 3), "b" -> Set(4, 5, 6)))
    assertEquals(""" [{"a":[1,2,3],"b":[4,5,6]}]  """.trim, composite.toJson)

    val some = Some(Array(1,2,3))
    val someStr = "[1,2,3]"
    assertEquals(someStr, JSON.stringify(some.toNative))
    assertEquals(some.get(1), NativeConverter[Option[Array[Int]]].fromNative(JSON.parse(someStr)).get(1))
    val n = None
    assertEquals("null", JSON.stringify(NativeConverter[Option[Int]].toNative(n)))
    assertEquals(n, NativeConverter[Option[Int]].fromNative(JSON.parse("null")))
    try
      NativeConverter[Option[Int]].fromNative(JSON.parse("[1,2]"))
      fail("invalid option")
    catch case _ => ()

    case class X(a: List[String])
    case class Y(b: Option[X]) derives NativeConverter
    val y = Y(Some(X(List())))
    val yStr = """ {"b":{"a":[]}} """.trim
    assertEquals(yStr, JSON.stringify(y.toNative))
    assertEquals(y, NativeConverter[Y].fromNative(JSON.parse(yStr)))

  end jsonCollectionTest

  /*
  Literal types are too buggy currently

  @Test
  def literalTypeDerivations: Unit =
    assertEquals("1", JSON.stringify(NativeConverter[1].toNative(2 - 1)))
    assertEquals(1, NativeConverter[1].fromNative(JSON.parse("1")))
    try
      NativeConverter[1].fromNative(JSON.parse("2"))
      fail("should have thrown")
    catch _ => ()

      // not working, since no Mirror.Of[(Int | String)] provided..
//    val nc: NativeConverter[Int|String] = NativeConverter.derived
//    assertEquals(3, nc.fromNative(JSON.parse("3")))
    
    // not working, since there is (correctly) no ValueOf[3|"X"]
    // and we can't make a less strict given because the above mirror
    // is unavailable
//    type ThreeOrX = 3 | "X"
//    assertEquals(3, NativeConverter[ThreeOrX].fromNative(JSON.parse("3")))

    type Square = "X"|"O"|Null
    case class Game1(square: Square) derives NativeConverter
    assertEquals(""" {"square":"X"}  """.trim, JSON.stringify(Game1("X").toNative))
    assertEquals(Game1("X"), NativeConverter[Game1].fromNative(JSON.parse(""" {"square":"X"}  """)))

    case class Game2(squares: Seq["X"|"Y"]) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game2(Seq("X")).toNative))
    assertEquals(Game2(Seq("X")), NativeConverter[Game2].fromNative(JSON.parse(""" {"squares":["X"]}  """)))

    type Squares = Seq["X"|"Y"]
    case class Game3(squares: Squares) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game3(Seq("X")).toNative))
    assertEquals(Game3(Seq("X")), NativeConverter[Game3].fromNative(JSON.parse(""" {"squares":["X"]}  """)))

    type Squares1 = Seq["X"|"Y"|Null]
    case class Game4(squares: Squares1) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game4(Seq("X")).toNative))
    assertEquals(Game4(Seq("X")), NativeConverter[Game4].fromNative(JSON.parse(""" {"squares":["X"]}  """)))

    type Squares2 = Seq[Square]
    case class Game5(squares: Squares2) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game5(Seq("X")).toNative))
    assertEquals(Game5(Seq("X")), NativeConverter[Game5].fromNative(JSON.parse(""" {"squares":["X"]}  """)))

  end literalTypeDerivations
   */


  case class Node(children: List[Node]) derives NativeConverter

  @Test
  def recursiveTest: Unit =
    val n = Node(List(Node(Nil), Node(Nil), Node(List(Node(Nil)))))
    val json =
      """ {"children":[{"children":[]},{"children":[]},{"children":[{"children":[]}]}]}  """.trim
    assertEquals(json, n.toJson)
    assertEquals(n, json.fromJson[Node])


    
