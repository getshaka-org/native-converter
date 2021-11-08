import org.getshaka.nativeconverter.NativeConverter
import org.junit.Test

class JsonTests:

  case class User(first: Option[String], last: String, age: Int) derives NativeConverter

  @Test(expected = classOf[UnsupportedOperationException])
  def testToNative: Unit =
    println(User(None, "Nagro", 99).toNative)

  @Test(expected = classOf[UnsupportedOperationException])
  def testToJson: Unit =
    println(User(None, "Nagro", 99).toJson)

  @Test(expected = classOf[UnsupportedOperationException])
  def testFromNative: Unit =
    val nc = summon[NativeConverter[User]]
    println(nc.fromNative(null))

  @Test(expected = classOf[UnsupportedOperationException])
  def testFromJson: Unit =
    val nc = summon[NativeConverter[User]]
    println(nc.fromJson(""" {} """))

