import org.getshaka.nativeconverter.{EsConverters, NativeConverter}
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test

import scala.collection.Map
import scala.scalajs.js

class EsConverterTests:
  import EsConverters.ESMapConv
  
  @Test
  def functionTest: Unit =
    val f2c = NativeConverter[(String, Int) => String]
    val f2 = (s: String, i: Int) => s + i
    val nativeF2 = f2c.toNative(f2)
    assertEquals("a1", nativeF2.asInstanceOf[js.Dynamic]("a", 1))
    val f2FromNative = f2c.fromNative(nativeF2)
    assertEquals("a1", f2FromNative("a", 1))

  @Test
  def es6MapTest: Unit =
    val nc = NativeConverter[Map[Int, Int]]
    val m = Map(1 -> 2, 3 -> 4)
    val nativeM = nc.toNative(m).asInstanceOf[js.Dynamic]
    assertEquals(4, nativeM.applyDynamic("get")(3))
    assertEquals(4, m.toNative.asInstanceOf[js.Dynamic].applyDynamic("get")(3))
    assertTrue(m.iterator sameElements nc.fromNative(nativeM))

    
