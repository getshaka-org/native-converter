import org.getshaka.nativeconverter.NativeConverter
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
    assertEquals(""" {"a":123} """.trim, JSON.stringify(Simple(123).toNative))
    val fromNative = summon[NativeConverter[Simple]].fromNative(JSON.parse(""" {"a":123}  """))
    assertEquals(123, fromNative.a)

    assertEquals(123, NativeConverter[Int].fromNative(JSON.parse("123")))

  @Test
  def jsonPrimitivesTest: Unit =
    import NativeConverter.given

    assertEquals(""""abcd"""", JSON.stringify("abcd".toNative))
    assertEquals("abcd", NativeConverter[String].fromNative(JSON.parse(""""abcd"""")))
    
    assertEquals("""false""", JSON.stringify(false.toNative))
    assertEquals(false, NativeConverter[Boolean].fromNative(JSON.parse("""false""")))

    assertEquals("""127""", JSON.stringify(127.byteValue.toNative))
    assertEquals(127.toByte, NativeConverter[Byte].fromNative(JSON.parse("127")))

    assertEquals(Short.MaxValue.toString, JSON.stringify(NativeConverter[Short].toNative(Short.MaxValue)))
    assertEquals(123.shortValue, NativeConverter[Short].fromNative(JSON.parse("123")))

    assertEquals("1", JSON.stringify(NativeConverter[Int].toNative(1)))
    assertEquals(Integer.MIN_VALUE, NativeConverter[Int].fromNative(JSON.parse(Integer.MIN_VALUE.toString)))
    try
      NativeConverter[Short].fromNative(JSON.parse(Int.MaxValue.toString))
      fail("Should not be able to make Short from large Int")
    catch
      case _ => ()

    assertEquals("1.1", JSON.stringify(1.1.toFloat.toNative).substring(0, 3))
    assertEquals(1.1.toFloat, NativeConverter[Float].fromNative(JSON.parse("1.1")), .0001)

    assertEquals(Double.MinValue.toString, JSON.stringify(Double.MinValue.toNative))
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
      
      def fromNative(nativeJs: js.Any): Long =
        try nativeJs.asInstanceOf[Int]
        catch case _ => nativeJs.asInstanceOf[String].toLong
        
    val bigLongStr = s""" "${Long.MaxValue}" """.trim
    assertEquals(Long.MaxValue, NativeConverter[Long].fromNative(JSON.parse(bigLongStr)))
    assertEquals(bigLongStr, JSON.stringify(NativeConverter[Long].toNative(Long.MaxValue)))
    
    assertEquals(123L, NativeConverter[Long].fromNative(JSON.parse("123")))
    assertEquals("123", JSON.stringify(NativeConverter[Long].toNative(123L)))

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
    val spJson = JSON.stringify(sp.toNative)
    assertEquals(s"""{"a":"hello","b":true,"c":127,"d":123,"e":1.11,"f":"${Long.MaxValue.toString}"}""", spJson)
    val newSp = NativeConverter[SimpleProduct].fromNative(JSON.parse(spJson))
    assertEquals(sp, newSp)
    
    case class FuncClass(f: String => Long) derives NativeConverter
    val fc = FuncClass(s => Long.MaxValue)
    val nativeFc = fc.toNative.asInstanceOf[js.Dynamic]
    assertEquals(Long.MaxValue, nativeFc.applyDynamic("f")("hello"))
    val newFc = NativeConverter[FuncClass].fromNative(nativeFc)
    assertEquals(Long.MaxValue, newFc.f(""))

    val nativeJs = JSON.parse(""" {"native": 1337} """)
    case class ClsWithNative(jsObj: js.Any) derives NativeConverter
    assertEquals(""" {"jsObj":{"native":1337}} """.trim, JSON.stringify(ClsWithNative(nativeJs).toNative))
    
    case class A(x: Int)
    case class B(a: A)
    case class C(b: B) derives NativeConverter
    val c = C(B(A(1234)))
    val strC: String = JSON.stringify(c.toNative)
    assertEquals("""{"b":{"a":{"x":1234}}}""", strC)
    assertEquals(c, NativeConverter[C].fromNative(JSON.parse(strC)))
    
    case class SingletonCaseClass() derives NativeConverter
    assertEquals(""" {} """.trim, JSON.stringify(SingletonCaseClass().toNative))
    assertEquals(SingletonCaseClass(),
      NativeConverter[SingletonCaseClass].fromNative(JSON.parse(""" {} """)))
    
    assertEquals(""" {} """.trim, JSON.stringify(CaseObject.toNative))
    assertEquals(CaseObject, NativeConverter[CaseObject.type].fromNative(JSON.parse(""" {} """)))
    
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
    assertEquals(Founder.Washington, NativeConverter[Founder].fromNative(JSON.parse(""" "Washington" """)))
    
    assertEquals(""" "Blue" """.trim, JSON.stringify(Color.Blue.toNative))
    assertEquals(0x00FF00, NativeConverter[Color].fromNative(JSON.parse(""" "Green" """)).rgb)
    
    assertEquals(""" {"@type":"Nn"} """.trim, JSON.stringify(Opt.Nn.toNative))
    try
      assertEquals(Opt.Nn, NativeConverter[Opt[Long]].fromNative(JSON.parse(""" "Nn" """)))
      fail("@type should be required")
    catch _ => ()
    assertEquals(Opt.Nn, NativeConverter[Opt[Long]].fromNative(JSON.parse(""" {"@type":"Nn"} """)))
    
    assertEquals(""" {"x":123,"@type":"Sm"} """.trim, JSON.stringify(Opt.Sm(123).toNative))
    assertEquals(Opt.Sm(Long.MaxValue),
      NativeConverter[Opt[Long]].fromNative(JSON.parse(s""" {"x": "${Long.MaxValue}", "@type":"Sm"} """)))
    
    try
      NativeConverter[Opt[Int]].fromNative(JSON.parse("1.1"))
      fail("should not be able to make Opt[Int] given a Double")
    catch case _ => ()
  
    assertEquals(""" {"@type":"N"} """.trim, JSON.stringify(N.toNative))
    assertEquals(N, NativeConverter[O[String]].fromNative(JSON.parse(""" {"@type":"N"} """)))
    assertEquals(""" {"x":"123","@type":"S"} """.trim, JSON.stringify(S(123L).toNative))
    assertEquals(S(123L), NativeConverter[O[Long]].fromNative(JSON.parse(""" {"x":"123","@type":"S"} """)))

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
    assertEquals(""" [{"a":[1,2,3],"b":[4,5,6]}]  """.trim, JSON.stringify(composite.toNative))

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

    
