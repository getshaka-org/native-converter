import com.augustnagro.nativeconverter.{EsConverters, NativeConverter}
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test

import scala.collection.Map
import scala.scalajs.js

class EsConverterTests:
  import EsConverters.given
  
  @Test
  def es6MapTest: Unit =
    val nc = NativeConverter[Map[Int, Int]]
    val m = Map(1 -> 2, 3 -> 4)
    val nativeM = nc.toNative(m).asInstanceOf[js.Dynamic]
    assertEquals(4, nativeM.applyDynamic("get")(3))
    assertEquals(4, m.toNative.asInstanceOf[js.Dynamic].applyDynamic("get")(3))
    assertTrue(m.iterator sameElements nc.fromNative(nativeM))

    
