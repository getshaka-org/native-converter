import com.augustnagro.nativeconverter.NativeConverter
import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable.ListBuffer
import scala.collection.{Iterable, Map, Seq, Set, mutable}
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

    assertEquals(Short.MaxValue.toString, JSON.stringify(Short.MaxValue.toNative))
    assertEquals(123.shortValue, NativeConverter[Short].fromNative(JSON.parse("123")))

    assertEquals("1", JSON.stringify(1.toNative))
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
        
    val bigLongStr = s""" "${Long.MaxValue}" """.trim.nn
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
    assertEquals(""" "SingletonCaseClass" """.trim, JSON.stringify(SingletonCaseClass().toNative))
    assertEquals(SingletonCaseClass(),
      NativeConverter[SingletonCaseClass].fromNative(JSON.parse(""" "SingletonCaseClass" """)))
    
    assertEquals(""" "CaseObject" """.trim, JSON.stringify(CaseObject.toNative))
    assertEquals(CaseObject, NativeConverter[CaseObject.type].fromNative(JSON.parse(""" "CaseObject" """)))
    
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
    
    assertEquals(""" "Nn" """.trim, JSON.stringify(Opt.Nn.toNative))
    assertEquals(Opt.Nn, NativeConverter[Opt[Long]].fromNative(JSON.parse(""" "Nn" """)))
    
    assertEquals(""" {"x":123} """.trim, JSON.stringify(Opt.Sm(123).toNative))
    assertEquals(Opt.Sm(Long.MaxValue),
      NativeConverter[Opt[Long]].fromNative(JSON.parse(s""" {"x": "${Long.MaxValue}"} """)))
    
    try
      NativeConverter[Opt[Int]].fromNative(JSON.parse("1.1"))
      fail("should not be able to make Opt[Int] given a Double")
    catch case _ => ()
  
    assertEquals(""" "N" """.trim, JSON.stringify(N.toNative))
    assertEquals(N, NativeConverter[O[String]].fromNative(""" "N" """))
    assertEquals(""" {"x":"123"} """.trim, JSON.stringify(S(123L).toNative))
    assertEquals(S(123L), NativeConverter[O[Long]].fromNative(JSON.parse(""" {"x":"123"} """)))

  @Test
  def jsonCollectionTest: Unit =
    import NativeConverter.{given}

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

    val e = List(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[List[Int]].toNative(e)))
    assertTrue(e sameElements NativeConverter[List[Int]].fromNative(nativeArr))

    val f = ListBuffer(1,2,3)
    assertEquals(strArr, JSON.stringify(NativeConverter[mutable.Buffer[Int]].toNative(f)))
    assertTrue(f sameElements NativeConverter[mutable.Buffer[Int]].fromNative(nativeArr))

    val composite = Array(Map("a" -> Set(1, 2, 3), "b" -> Set(4, 5, 6)))
    assertEquals(""" [{"a":[1,2,3],"b":[4,5,6]}]  """.trim, JSON.stringify(composite.toNative))

    val some = Some(Array(1,2,3))
    val someStr = "[[1,2,3]]"
    assertEquals(someStr, JSON.stringify(some.toNative))
    assertEquals(some.get(1), NativeConverter[Option[Array[Int]]].fromNative(JSON.parse(someStr)).get(1))
    val n = None
    assertEquals("[]", JSON.stringify(NativeConverter[Option[Int]].toNative(n)))
    try
      NativeConverter[Option[Int]].fromNative(JSON.parse("[1,2]"))
      fail("invalid option")
    catch case _ => ()

    case class X(a: List[String])
    case class Y(b: Option[X]) derives NativeConverter
    val y = Y(Some(X(List())))
    val yStr = """ {"b":[{"a":[]}]} """.trim.nn
    assertEquals(yStr, JSON.stringify(y.toNative))
    assertEquals(y, NativeConverter[Y].fromNative(JSON.parse(yStr)))

  end jsonCollectionTest

  @Test
  def literalTypeDerivations(): Unit =
    /*
    This works without any changes
     */
    type Square = "X"|"O"|Null
    case class Game1(square: Square) derives NativeConverter
    assertEquals(""" {"square":"X"}  """.trim, JSON.stringify(Game1("X").toNative))

    /*
    This works, but only after adding the below typeclass to the NativeConverter
    Companion object (line 180):
    
    given [A <: String]: NativeConverter[A] with
      extension (t: A) def toNative: js.Any = NativeConverter[String].toNative(t)
      def fromNative(nativeJs: js.Any): A =
        NativeConverter[String].fromNative(nativeJs).asInstanceOf[A]
        
    Trying to add in this method and not in companion object
    causes StackOverflow, despite summonInline.
     */
    case class Game2(squares: Seq["X"|"Y"]) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game2(Seq("X")).toNative))
    
    /*
    This also works with the added typeclass
     */
    type Squares = Seq["X"|"Y"]
    case class Game3(squares: Squares) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game3(Seq("X")).toNative))
    
    /*
    But this does not
     */
    type Squares1 = Seq["X"|"Y"|Null]
    case class Game4(squares: Squares1) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game4(Seq("X")).toNative))
    
    /*
    Neither does this (different error)
     */
    type Squares2 = Seq[Square]
    case class Game5(squares: Squares2) derives NativeConverter
    assertEquals(""" {"squares":["X"]}  """.trim, JSON.stringify(Game5(Seq("X")).toNative))

  end literalTypeDerivations

// todo collections, Option

    
