import org.getshaka.nativeconverter.NativeConverter

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.Random

/*
It is GREAT that we can construct js.Objects with stringly-typed
parameters in a fast way, since Scala 3 Mirror provides parameter names
as type constant String.
 */
object JsObjectConstructionBenchmark:
  
  class C(val a: String, val b: Boolean, val c: Int) extends js.Object
  
  case class D(a: String, b: Boolean, c: Int) derives NativeConverter
  
  @main def bench(): Unit =
    for _ <- 1 to 20 do loop()
  
  def loop(): Unit =
    val limit = 1000000
    val res = Array.ofDim[String](limit)
    var i = 0
    
    var start = js.Date.now()
    while i < limit do
      res(i) = JSON.stringify(C("hello world", true, 69))
      i += 1
    var end = js.Date.now()
    def randomJson: String = res(Random.nextInt(res.size))
    println(s"ms to make $randomJson classes: ${end - start}")
  
    i = 0
    start = js.Date.now()
    while i < limit do
      res(i) = JSON.stringify(js.Dynamic.literal("a" -> "hello world", "b" -> true, "c" -> 69))
      i += 1
    end = js.Date.now()
    println(s"ms to make $randomJson literals via tuples: ${end - start}")
    
    i = 0
    start = js.Date.now()
    while i < limit do
      res(i) = JSON.stringify(js.Dynamic.literal(a = "hello world", b = true, c = 69))
      i += 1
    end = js.Date.now()
    println(s"ms to make $randomJson literals: ${end - start}")
    
    i = 0
    start = js.Date.now()
    while i < limit do
      val literal = js.Dynamic.literal()
      literal.updateDynamic("a")("hello world")
      literal.updateDynamic("b")(true)
      literal.updateDynamic("c")(69)
      res(i) = JSON.stringify(literal)
      i += 1
    end = js.Date.now()
    println(s"ms to make $randomJson literals via updateDynamic: ${end - start}")
    
    i = 0
    start = js.Date.now()
    while i < limit do
      val dynamicObj = js.Object().asInstanceOf[js.Dynamic]
      dynamicObj.updateDynamic("a")("hello world")
      dynamicObj.updateDynamic("b")(true)
      dynamicObj.updateDynamic("c")(69)
      res(i) = JSON.stringify(dynamicObj)
      i += 1
    end = js.Date.now()
    println(s"ms to make $randomJson objects with new js.Object + updateDynamic: ${end - start}")
  
    i = 0
    start = js.Date.now()
    while i < limit do
      res(i) = JSON.stringify(D("hello world", true, 69).toNative)
      i += 1
    end = js.Date.now()
    println(s"ms to make $randomJson objects with derived NativeConverter: ${end - start}")
    println()
