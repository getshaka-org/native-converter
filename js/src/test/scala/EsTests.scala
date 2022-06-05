import org.getshaka.nativeconverter.NativeConverter
import org.junit.Test
import org.junit.Assert.assertEquals

import scala.scalajs.js

class EsTests:

  @Test
  def testJsDate(): Unit =
    // From OffsetDateTime#toString
    val isoOffsetString = "2021-11-07T19:41:30.786417802-08:00"
    // From Instant#toString
    val isoInstantString = "2021-11-08T03:41:57.993515993Z"

    val jsInstantDate = NativeConverter[js.Date].fromJson(isoInstantString)
    assertEquals(jsInstantDate.toISOString, NativeConverter[js.Date].toJson(jsInstantDate))

    // val jsOffsetDate = NativeConverter[js.Date].fromJson(isoOffsetString)
    // assertEquals(jsOffsetDate.toISOString, NativeConverter[js.Date].toJson(jsOffsetDate))
