package com.augustnagro.nativeconverter

import scala.collection.{Iterable, Map, Seq, Set}
import scala.collection.mutable.{HashMap, HashSet, Buffer, ArrayBuffer}
import scala.collection.immutable.List
import scala.deriving.Mirror
import scala.compiletime.{constValue, constValueTuple, erasedValue, error, summonFrom, summonInline}
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.{JSON, WrappedArray, WrappedMap}

/**
 * Typeclass for converting between Scala.js and native JavaScript.
 * @tparam T the type to convert
 */
trait NativeConverter[T]:
  /**
   * Convert a Scala.js type to native JavaScript.
   * <br>
   * This is an extension method, so it's available on all types
   * that `derive NativeConverter`. To use for other types, like Int, summon
   * a NativeConverter and use: `NativeConverter[Int].toNative(123)`
   * <br>
   * Any RuntimeException subclass may be thrown if conversion fails.
   */
  extension (t: T) def toNative: js.Any

  /**
   * Convert a native Javascript type to Scala.js.
   * <br>
   * Any RuntimeException subclass may be thrown if conversion fails.
   */
  def fromNative(nativeJs: js.Any): T

object NativeConverter:

  /**
   * Helper method so summoning NativeConverters may be done with
   * `NativeConverter[A].fromNative(..)`, instead of
   * `summon[NativeConverter[A]].fromNative(..)`.
   */
  inline def apply[A](using nc: NativeConverter[A]) = nc

  /*
  Here we define some base type-classes. Most of the primitive types
  like String and Boolean have the same representation in Scala.js and JavaScript,
  so we just need to cast.
   */

  /**
   * These types are already implicitly native Javascript
   */
  private type ImplicitlyJsAny = (String | Boolean | Byte | Short | Int | Float | Double | Null | js.Any)

  given NativeConverter[String] with
    extension (t: String) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): String = nativeJs.asInstanceOf[String]

  given NativeConverter[Boolean] with
    extension (t: Boolean) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Boolean = nativeJs.asInstanceOf[Boolean]

  given NativeConverter[Byte] with
    extension (t: Byte) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Byte = nativeJs.asInstanceOf[Byte]

  given NativeConverter[Short] with
    extension (t: Short) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Short = nativeJs.asInstanceOf[Short]

  given NativeConverter[Int] with
    extension (t: Int) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Int = nativeJs.asInstanceOf[Int]

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given NativeConverter[Float] with
    extension (t: Float) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Float = nativeJs.asInstanceOf[Float]

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given NativeConverter[Double] with
    extension (t: Double) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Double = nativeJs.asInstanceOf[Double]

  given NativeConverter[Null] with
    extension (t: Null) def toNative: js.Any = t.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Null = nativeJs.asInstanceOf[Null]

  given NativeConverter[js.Any] with
    extension (t: js.Any) def toNative: js.Any = t
    def fromNative(nativeJs: js.Any): js.Any = nativeJs

  /*
  Char Long, etc, don't precisely map to JS, so the conversion is debatable.
  Good thing is that they can be easily overriden.
   */

  given NativeConverter[Char] with
    extension (t: Char) def toNative: js.Any = t.toString
    def fromNative(nativeJs: js.Any): Char = nativeJs.asInstanceOf[String].charAt(0)

  given NativeConverter[Long] with
    extension (t: Long) def toNative: js.Any = t.toString
    def fromNative(nativeJs: js.Any): Long = nativeJs.asInstanceOf[String].toLong
  
  /*
  Functions are converted with Scala.js's helper methods in js.Any
   */

  given [A]: NativeConverter[Function0[A]] with
    extension (t: Function0[A]) def toNative: js.Any =
      js.Any.fromFunction0(t)
    def fromNative(nativeJs: js.Any): Function0[A] =
      js.Any.toFunction0(nativeJs.asInstanceOf[js.Function0[A]])

  given [A, B]: NativeConverter[Function1[A, B]] with
    extension (t: Function1[A, B]) def toNative: js.Any =
      js.Any.fromFunction1(t)
    def fromNative(nativeJs: js.Any): Function1[A, B] =
      js.Any.toFunction1(nativeJs.asInstanceOf[js.Function1[A, B]])

  given [A, B, C]: NativeConverter[Function2[A, B, C]] with
    extension (t: Function2[A, B, C]) def toNative: js.Any =
      js.Any.fromFunction2(t)
    def fromNative(nativeJs: js.Any): Function2[A, B, C] =
      js.Any.toFunction2(nativeJs.asInstanceOf[js.Function2[A, B, C]])

  given [A, B, C, D]: NativeConverter[Function3[A, B, C, D]] with
    extension (t: Function3[A, B, C, D]) def toNative: js.Any =
      js.Any.fromFunction3(t)
    def fromNative(nativeJs: js.Any): Function3[A, B, C, D] =
      js.Any.toFunction3(nativeJs.asInstanceOf[js.Function3[A, B, C, D]])
      
  /*
  Collection types. Arrays, Iterables, Seqs, Sets, Lists, and Buffers
  are serialized using JavaScript Arrays. Maps become JS objects, although only
  String keys are supported, like in JSON. The EsConverters class has conversions
  to js.Map.
   */
  
  private def makeNativeArray[T](it: Iterable[T], nc: NativeConverter[T]): js.Array[js.Any] =
    val res = js.Array[js.Any]()
    for t <- it do res.push(nc.toNative(t))
    res

  given [A: ClassTag](using nc: NativeConverter[A]): NativeConverter[Array[A]] with
    extension (t: Array[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
  
    def fromNative(nativeJs: js.Any): Array[A] =
      val jsArr = nativeJs.asInstanceOf[js.Array[js.Any]]
      val len = jsArr.length
      val res = Array.ofDim[A](len)
      var i = 0
      while i < len do
        res(i) = nc.fromNative(jsArr(i))
        i += 1
      res

  given [A](using nc: NativeConverter[A]): NativeConverter[Iterable[A]] with
    extension (t: Iterable[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
      
    def fromNative(nativeJs: js.Any): Iterable[A] =
        nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative)

  given [A](using nc: NativeConverter[A]): NativeConverter[Seq[A]] with
    extension (t: Seq[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
      
    def fromNative(nativeJs: js.Any): Seq[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toSeq

  given [A <: String]: NativeConverter[A] with
    extension (t: A) def toNative: js.Any = NativeConverter[String].toNative(t)

    def fromNative(nativeJs: js.Any): A =
      NativeConverter[String].fromNative(nativeJs).asInstanceOf[A]
  
  given [A](using nc: NativeConverter[A]): NativeConverter[Set[A]] with
    extension (t: Set[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
      
    def fromNative(nativeJs: js.Any): Set[A] =
      val jsArr = nativeJs.asInstanceOf[js.Array[js.Any]]
      val res = HashSet[A]()
      val len = jsArr.length
      var i = 0
      while i < len do
        res += nc.fromNative(jsArr(i))
        i += 1
      res

  given [A](using nc: NativeConverter[A]): NativeConverter[List[A]] with
    extension (t: List[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
  
    def fromNative(nativeJs: js.Any): List[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toList
      
  given [A](using nc: NativeConverter[A]): NativeConverter[Buffer[A]] with
    extension (t: Buffer[A]) def toNative: js.Any =
      makeNativeArray(t, nc)
      
    def fromNative(nativeJs: js.Any): Buffer[A] =
      val jsArr = nativeJs.asInstanceOf[js.Array[js.Any]]
      val len = jsArr.length
      val res = ArrayBuffer[A]()
      res.sizeHint(len)
      var i = 0
      while i < len do
        res += nc.fromNative(jsArr(i))
        i += 1
      res
      
  given [A](using nc: NativeConverter[A]): NativeConverter[Map[String, A]] with
    extension (t: Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- t do
        res.updateDynamic(k)(nc.toNative(v))
      res
  
    def fromNative(nativeJs: js.Any): Map[String, A] =
      val dict = nativeJs.asInstanceOf[js.Dictionary[js.Any]]
      val res = HashMap[String, A]()
      for (k, v) <- dict do
        res(k) = nc.fromNative(v)
      res

  given [A](using nc: NativeConverter[A]): NativeConverter[Option[A]] with
    extension (t: Option[A]) def toNative: js.Any =
      t.map(v => js.Array(nc.toNative(v))).getOrElse(js.Array())
      
    def fromNative(nativeJs: js.Any): Option[A] =
      val arr = nativeJs.asInstanceOf[js.Array[js.Any]]
      arr.length match
        case 0 => None
        case 1 => Some(nc.fromNative(arr(0)))
        case _ => throw IllegalArgumentException("Cannot make Option if Array has > 1 element: " + arr)

  /*
  NOW, LETS PROGRAM AT THE TYPE-LEVEL
  
  There are two runtime performance cases to consider (both on JVM and JS).

  1. The product/sum type is not generic. Then, only one NativeConverter
  is ever generated in the companion object.

  2. The product/sum type has a generic parameter(s). For example,
  case class Test[T](t: T). Then, every summon of NativeConverter[Test[Int]]
  would expect to receive a new instance, as specified here:
  https://dotty.epfl.ch/docs/reference/contextual/givens.html#given-instance-initialization
   */

  /**
   * Derive a NativeConverter for type T. This method is called by the compiler automatically
   * when adding `derives NativeConverter` on a class. You can also use it to derive given
   * instances anywhere, which is useful if Cross-Building a Scala.js project:
   * <br>
   * `given NativeConverter[User] = NativeConverter.derived`
   * <br>
   * Only Sum and Product types are supported
   */
  inline given derived[T](using m: Mirror.Of[T]): NativeConverter[T] = inline m match
    case s: Mirror.SumOf[T] => sumConverter[T, s.MirroredElemTypes](s)
      
    case p: Mirror.ProductOf[T] => new NativeConverter[T]:
      extension (t: T) def toNative: js.Any =
        productToNative[T](p, t.asInstanceOf[Product])
        
      def fromNative(nativeJs: js.Any): T =
        nativeToProduct[T](p, nativeJs)

  /**
   * If every element of the Sum type is a Singleton, then
   * serialize using the type name. Otherwise, it is an ADT
   * that is serialized the normal way, by summoning NativeConverters
   * for the elements.
   */
  private inline def sumConverter[T, Mets <: Tuple](m: Mirror.SumOf[T]): NativeConverter[T] =
    inline erasedValue[Mets] match
      case _: (met *: metsTail) =>
        inline if isSingleton[met] then sumConverter[T, metsTail](m)
        else buildAdtSumConverter[T](m)
        
      // all of the elements were Singletons.. build simple enum case  
      case _: EmptyTuple => simpleSumConverter[T](m)

  /**
   * A singleton is a Product with no parameter elements
   */
  private inline def isSingleton[T]: Boolean = summonFrom[T]:
    case product: Mirror.ProductOf[T] =>
      inline erasedValue[product.MirroredElemTypes] match
        case _: EmptyTuple => true
        case _ => false
    case _ => false

  private inline def simpleSumConverter[T](m: Mirror.SumOf[T]): NativeConverter[T] =
    new NativeConverter[T]:
      extension (t: T) def toNative: js.Any =
        simpleSumToNative[m.MirroredElemLabels](m.ordinal(t))
        
      def fromNative(nativeJs: js.Any): T =
        simpleSumFromNative[T, m.MirroredLabel, m.MirroredElemTypes, m.MirroredElemLabels](
          nativeJs.asInstanceOf[String])

  /**
   * This generates an if-else chain that returns the String type name for a given ordinal.
   */
  private inline def simpleSumToNative[Mels <: Tuple](n: Int, i: Int = 0): js.Any =
    inline erasedValue[Mels] match
      case _: EmptyTuple => // can never reach
      case _: (mel *: melsTail) => 
        if i == n then constString[mel]
        else simpleSumToNative[melsTail](n, i + 1)

  /**
   * This generates an if-else chain that compares the deserialized String to the element type names.
   * The Mirror.ProductOf::fromProduct returns the 1 instance of that Singleton, so there's no need
   * to summon[NativeConverter[met]] in the simple case.
   */
  private inline def simpleSumFromNative[T, Label, Mets <: Tuple, Mels <: Tuple](name: String): T =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        throw IllegalArgumentException("Sum type " + constString[Label] + " does not have element " + name)
      case _: ((met *: metsTail), (mel *: melsTail)) =>
        if constString[mel] == name then
          summonInline[Mirror.ProductOf[met & T]].fromProduct(EmptyTuple)
        else simpleSumFromNative[T, Label, metsTail, melsTail](name)
    
  private inline def buildAdtSumConverter[T](m: Mirror.SumOf[T]): NativeConverter[T] =
    new NativeConverter[T]:
      extension (t: T) def toNative: js.Any =
        adtSumToNative[T, m.MirroredElemTypes](t, m.ordinal(t))
      
      def fromNative(nativeJs: js.Any): T =
        adtSumFromNative[T, m.MirroredLabel, m.MirroredElemTypes](nativeJs)

  /**
   * If the Sum type has any element that is not Singleton, we summon the NativeConverters
   * for the elements we want to convert.
   */
  private inline def adtSumToNative[T, Mets <: Tuple](t: T, ordinal: Int, i: Int = 0): js.Any =
    inline erasedValue[Mets] match
      case _: EmptyTuple => // can never reach
      case _: (met *: metsTail) =>
        if i == ordinal then
          summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[T]].toNative(t)
        else adtSumToNative[T, metsTail](t, ordinal, i + 1)
        
  private inline def adtSumFromNative[T, Label, Mets <: Tuple](nativeJs: js.Any): T =
    inline erasedValue[Mets] match
      case _: EmptyTuple => throw IllegalArgumentException(
        "Cannot decode " + constString[Label] + " with " + JSON.stringify(nativeJs))
      case _: (met *: metsTail) =>
        try summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[T]].fromNative(nativeJs)
        catch _ => adtSumFromNative[T, Label, metsTail](nativeJs)


  /**
   * When deserializing a Product, first we check if it is a Singleton. If yes, return the
   * String productPrefix. If no, then create a JS Object with element names as keys, and
   * summon NativeConverters for every element to call toNative() on that element.
   * <br>
   * The reason we use productPrefix instead of constString[TypeLabel] (like in the simple Sum
   * case) is because of case objects. If you have `case object None`, the type name becomes `None$`,
   * which is not very helpful.
   */
  private inline def productToNative[T](m: Mirror.ProductOf[T], p: Product): js.Any =
    inline if isSingleton[T] then p.productPrefix
    else buildProductToNative[m.MirroredElemTypes, m.MirroredElemLabels](p)
  
  private inline def buildProductToNative[Mets <: Tuple, Mels <: Tuple](
    p: Product,
    i: Int = 0,
    res: js.Dynamic = js.Object().asInstanceOf[js.Dynamic]
  ): js.Any =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      // base case.. return res
      case _: (EmptyTuple, EmptyTuple) => res
      
      // Manually inline the common cases to avoid any instanceof checks.
      case _: ((ImplicitlyJsAny *: metsTail), (mel *: melsTail)) =>
        res.updateDynamic(constString[mel])(p.productElement(i).asInstanceOf[js.Any])
        buildProductToNative[metsTail, melsTail](p, i + 1, res)
        
      // try to summon a NativeConverter for the MirroredElemType
      case _: ((met *: metsTail), (mel *: melsTail)) =>
        val converter = summonInline[NativeConverter[met]]
        res.updateDynamic(constString[mel])(converter.toNative(p.productElement(i).asInstanceOf[met]))
        buildProductToNative[metsTail, melsTail](p, i + 1, res)
        
  // use the EmptyTuple if singleton, otherwise construct
  private inline def nativeToProduct[T](m: Mirror.ProductOf[T], nativeJs: js.Any): T =
    inline if isSingleton[T] then m.fromProduct(EmptyTuple)
    else buildNativeProduct[T, m.MirroredElemTypes, m.MirroredElemLabels](
      m, nativeJs.asInstanceOf[js.Dynamic], ArrayProduct(sizeOf[m.MirroredElemTypes]))
  
  private inline def buildNativeProduct[T, Mets <: Tuple, Mels <: Tuple](
    mirror: Mirror.ProductOf[T],
    nativeJs: js.Dynamic,
    res: ArrayProduct,
    i: Int = 0
  ): T = {
    inline (erasedValue[Mets], erasedValue[Mels]) match
      // base case, return new instance
      case _: (EmptyTuple, EmptyTuple) => mirror.fromProduct(res)

      // Manually inline the common cases to avoid any instanceof checks.
      case _: ((ImplicitlyJsAny *: metsTail), (mel *: melsTail)) =>
        res(i) = nativeJs.selectDynamic(constString[mel]).asInstanceOf[Any]
        buildNativeProduct[T, metsTail, melsTail](mirror, nativeJs, res, i + 1)
      
      // try to summon a NativeConverter for the MirroredElemType
      case _: ((met *: metsTail), (mel *: melsTail)) =>
        val converter = summonInline[NativeConverter[met]]
        val convertedProp: met = converter.fromNative(nativeJs.selectDynamic(constString[mel]))
        res(i) = convertedProp.asInstanceOf[Any]
        buildNativeProduct[T, metsTail, melsTail](mirror, nativeJs, res, i + 1)
  }

  /**
   * MirroredElemLabels and MirroredLabels are always Tuples of String.
   * However, the tuples themseves don't have types.. so we just call .toString
   * after materializing the constant value.
   * 
   * I can confirm that the toString call is eliminated at compile time by inspecting
   * the compiled JS.
   */
  private inline def constString[T]: String = constValue[T].toString


  private inline def sizeOf[T <: Tuple]: Int = constValue[Tuple.Size[T]]

/**
 * Helper extension method to convert from native JS types.
 * You can call `nativeJsType.fromNative[Int]` instead of
 * `summon[NativeConverter[Int]].fromNative(nativeJsType)`
 *
 * If this extension method is inside the companion object then it can use
 * the given derived method to automatically derive NativeConverters for any random class.
 * I don't know how I feel about that.. probably less error prone to explicitly derive whenever possible.
 * Although if users import NativeConverter.given to get `toNative` methods on Strings, Booleans, etc,
 * then they can still go wild.
 */
//extension [T](nativeJs: js.Any)(using nc: NativeConverter[T]) def fromNative: T =
//  nc.fromNative(nativeJs)
