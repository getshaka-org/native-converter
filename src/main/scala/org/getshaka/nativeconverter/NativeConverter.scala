package org.getshaka.nativeconverter

import scala.collection.{Iterable, Map, Seq, Set}
import scala.collection.mutable.{HashMap, HashSet, Buffer, ArrayBuffer}
import scala.collection.immutable.List
import scala.collection.immutable
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
      
  given [A, B, C, D, E]: NativeConverter[Function4[A, B, C, D, E]] with
    extension (t: Function4[A, B, C, D, E]) def toNative: js.Any =
      js.Any.fromFunction4(t)
    def fromNative(nativeJs: js.Any): Function4[A, B, C, D, E] =
      js.Any.toFunction4(nativeJs.asInstanceOf[js.Function4[A, B, C, D, E]])

  given [A, B, C, D, E, F]: NativeConverter[Function5[A, B, C, D, E, F]] with
    extension (t: Function5[A, B, C, D, E, F]) def toNative: js.Any =
      js.Any.fromFunction5(t)
    def fromNative(nativeJs: js.Any): Function5[A, B, C, D, E, F] =
      js.Any.toFunction5(nativeJs.asInstanceOf[js.Function5[A, B, C, D, E, F]])
      
  given [A, B, C, D, E, F, G]: NativeConverter[Function6[A, B, C, D, E, F, G]] with
    extension (t: Function6[A, B, C, D, E, F, G]) def toNative: js.Any =
      js.Any.fromFunction6(t)
    def fromNative(nativeJs: js.Any): Function6[A, B, C, D, E, F, G] =
      js.Any.toFunction6(nativeJs.asInstanceOf[js.Function6[A, B, C, D, E, F, G]])
      
  given [A, B, C, D, E, F, G, H]: NativeConverter[Function7[A, B, C, D, E, F, G, H]] with
    extension (t: Function7[A, B, C, D, E, F, G, H]) def toNative: js.Any =
      js.Any.fromFunction7(t)
    def fromNative(nativeJs: js.Any): Function7[A, B, C, D, E, F, G, H] =
      js.Any.toFunction7(nativeJs.asInstanceOf[js.Function7[A, B, C, D, E, F, G, H]])
      
  given [A, B, C, D, E, F, G, H, I]: NativeConverter[Function8[A, B, C, D, E, F, G, H, I]] with
    extension (t: Function8[A, B, C, D, E, F, G, H, I]) def toNative: js.Any =
      js.Any.fromFunction8(t)
    def fromNative(nativeJs: js.Any): Function8[A, B, C, D, E, F, G, H, I] =
      js.Any.toFunction8(nativeJs.asInstanceOf[js.Function8[A, B, C, D, E, F, G, H, I]])
      
  given [A, B, C, D, E, F, G, H, I, J]: NativeConverter[Function9[A, B, C, D, E, F, G, H, I, J]] with
    extension (t: Function9[A, B, C, D, E, F, G, H, I, J]) def toNative: js.Any =
      js.Any.fromFunction9(t)
    def fromNative(nativeJs: js.Any): Function9[A, B, C, D, E, F, G, H, I, J] =
      js.Any.toFunction9(nativeJs.asInstanceOf[js.Function9[A, B, C, D, E, F, G, H, I, J]])
      
  given [A, B, C, D, E, F, G, H, I, J, K]: NativeConverter[Function10[A, B, C, D, E, F, G, H, I, J, K]] with
    extension (t: Function10[A, B, C, D, E, F, G, H, I, J, K]) def toNative: js.Any =
      js.Any.fromFunction10(t)
    def fromNative(nativeJs: js.Any): Function10[A, B, C, D, E, F, G, H, I, J, K] =
      js.Any.toFunction10(nativeJs.asInstanceOf[js.Function10[A, B, C, D, E, F, G, H, I, J, K]])
      
  given [A, B, C, D, E, F, G, H, I, J, K, L]: NativeConverter[Function11[A, B, C, D, E, F, G, H, I, J, K, L]] with
    extension (t: Function11[A, B, C, D, E, F, G, H, I, J, K, L]) def toNative: js.Any =
      js.Any.fromFunction11(t)
    def fromNative(nativeJs: js.Any): Function11[A, B, C, D, E, F, G, H, I, J, K, L] =
      js.Any.toFunction11(nativeJs.asInstanceOf[js.Function11[A, B, C, D, E, F, G, H, I, J, K, L]])

  given [A, B, C, D, E, F, G, H, I, J, K, L, M]: NativeConverter[Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]] with
    extension (t: Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]) def toNative: js.Any =
      js.Any.fromFunction12(t)
    def fromNative(nativeJs: js.Any): Function12[A, B, C, D, E, F, G, H, I, J, K, L, M] =
      js.Any.toFunction12(nativeJs.asInstanceOf[js.Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]])
    
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N]: NativeConverter[Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]] with
    extension (t: Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]) def toNative: js.Any =
      js.Any.fromFunction13(t)
    def fromNative(nativeJs: js.Any): Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N] =
      js.Any.toFunction13(nativeJs.asInstanceOf[js.Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]])
    
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]: NativeConverter[Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]] with
    extension (t: Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]) def toNative: js.Any =
      js.Any.fromFunction14(t)
    def fromNative(nativeJs: js.Any): Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O] =
      js.Any.toFunction14(nativeJs.asInstanceOf[js.Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]])
  
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]: NativeConverter[Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]] with
    extension (t: Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]) def toNative: js.Any =
      js.Any.fromFunction15(t)
    def fromNative(nativeJs: js.Any): Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P] =
      js.Any.toFunction15(nativeJs.asInstanceOf[js.Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]])
      
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]: NativeConverter[Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]] with
    extension (t: Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]) def toNative: js.Any =
      js.Any.fromFunction16(t)
    def fromNative(nativeJs: js.Any): Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q] =
      js.Any.toFunction16(nativeJs.asInstanceOf[js.Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]])
    
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]: NativeConverter[Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]] with
    extension (t: Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]) def toNative: js.Any =
      js.Any.fromFunction17(t)
    def fromNative(nativeJs: js.Any): Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R] =
      js.Any.toFunction17(nativeJs.asInstanceOf[js.Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]])
    
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]: NativeConverter[Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]] with
    extension (t: Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]) def toNative: js.Any =
      js.Any.fromFunction18(t)
    def fromNative(nativeJs: js.Any): Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S] =
      js.Any.toFunction18(nativeJs.asInstanceOf[js.Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]])

  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]: NativeConverter[Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]] with
    extension (t: Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]) def toNative: js.Any =
      js.Any.fromFunction19(t)
    def fromNative(nativeJs: js.Any): Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T] =
      js.Any.toFunction19(nativeJs.asInstanceOf[js.Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]])

    given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]: NativeConverter[Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]] with
      extension (t: Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]) def toNative: js.Any =
        js.Any.fromFunction20(t)
      def fromNative(nativeJs: js.Any): Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U] =
        js.Any.toFunction20(nativeJs.asInstanceOf[js.Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]])
  
  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: NativeConverter[Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]] with
    extension (t: Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]) def toNative: js.Any =
      js.Any.fromFunction21(t)
    def fromNative(nativeJs: js.Any): Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V] =
      js.Any.toFunction21(nativeJs.asInstanceOf[js.Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]])

  given [A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]: NativeConverter[Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]] with
    extension (t: Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]) def toNative: js.Any =
      js.Any.fromFunction22(t)
    def fromNative(nativeJs: js.Any): Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W] =
      js.Any.toFunction22(nativeJs.asInstanceOf[js.Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]])
  
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
      t.map(nc.toNative).getOrElse(null.asInstanceOf[js.Any])
      
    def fromNative(nativeJs: js.Any): Option[A] =
      Option(nativeJs).map(nc.fromNative)

  given immutableSeqConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Seq[A]] with
    extension (t: immutable.Seq[A]) def toNative: js.Any =
      makeNativeArray(t, nc)

    def fromNative(nativeJs: js.Any): immutable.Seq[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toSeq

  given immutableMapConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Map[String, A]] with
    extension (t: immutable.Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- t do
        res.updateDynamic(k)(nc.toNative(v))
      res

    def fromNative(nativeJs: js.Any): immutable.Map[String, A] =
      val dict = nativeJs.asInstanceOf[js.Dictionary[js.Any]]
      var res = immutable.HashMap[String, A]()
      for (k, v) <- dict do
        res = res.updated(k, nc.fromNative(v))
      res

  given immutableSetConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Set[A]] with
    extension (t: immutable.Set[A]) def toNative: js.Any =
      makeNativeArray(t, nc)

    def fromNative(nativeJs: js.Any): immutable.Set[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toSet

  /*
  Converters for Literal Types.
  
  Not working with explicit nulls: https://github.com/lampepfl/dotty/issues/11645
   */
  type LiteralType = Double|Float|Long|Int|Short|Byte|Boolean|Char|String

  inline given literalDoubleConv[T <: Double]: NativeConverter[T] =
    NativeConverter[Double].asInstanceOf[NativeConverter[T]]

  inline given literalFloatConv[T <: Float]: NativeConverter[T] =
    NativeConverter[Float].asInstanceOf[NativeConverter[T]]

  inline given literalLongConv[T <: Long]: NativeConverter[T] =
    NativeConverter[Long].asInstanceOf[NativeConverter[T]]

  inline given literalIntConv[T <: Int]: NativeConverter[T] =
    NativeConverter[Int].asInstanceOf[NativeConverter[T]]

  inline given literalShortConv[T <: Short]: NativeConverter[T] =
    NativeConverter[Short].asInstanceOf[NativeConverter[T]]

  inline given literalByteConv[T <: Byte]: NativeConverter[T] =
    NativeConverter[Byte].asInstanceOf[NativeConverter[T]]

  inline given literalBooleanConv[T <: Boolean]: NativeConverter[T] =
    NativeConverter[Boolean].asInstanceOf[NativeConverter[T]]

  inline given literalCharConv[T <: Char]: NativeConverter[T] =
    NativeConverter[Char].asInstanceOf[NativeConverter[T]]
  
  inline given literalStringConv[T <: String]: NativeConverter[T] =
    NativeConverter[String].asInstanceOf[NativeConverter[T]]

  given literalSumConv[T <: LiteralType](using vo: ValueOf[T], nc: NativeConverter[T]): NativeConverter[T] with
    extension (t: T) def toNative: js.Any = nc.toNative(t)

    def fromNative(nativeJs: js.Any): T =
      val a = nc.fromNative(nativeJs)
      if vo.value == a then vo.value
      else throw IllegalArgumentException(s"Can not convert since $a not equal to literal ${vo.value}")

  /*
  NOW, LETS PROGRAM AT THE TYPE-LEVEL
  
  There are two runtime performance cases to consider (both on JVM and JS).

  1. The product/sum type is not generic. Then, only one NativeConverter
  is ever generated in the companion object.

  2. The product/sum type has a generic parameter(s). For example,
  case class Test[T](t: T). Then, every summon of NativeConverter[Test[Int]]
  would expect to receive a new instance, as specified here:
  https://dotty.epfl.ch/docs/reference/contextual/givens.html#given-instance-initialization
  Our derived NativeConverters are stateless in any case.
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
   * for the elements and adding a `@type` property providing the
   * (short) class name.
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
  private inline def isSingleton[T]: Boolean = summonFrom[T] {
    case product: Mirror.ProductOf[T] =>
      inline erasedValue[product.MirroredElemTypes] match
        case _: EmptyTuple => true
        case _ => false
    case _ => false
  }

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

  /**
   * Uses a `@type` property that holds the (short) class name. todo: make configurable
   */
  private inline def buildAdtSumConverter[T](m: Mirror.SumOf[T]): NativeConverter[T] =
    new NativeConverter[T]:
      extension (t: T) def toNative: js.Any =
        adtSumToNative[T, m.MirroredElemTypes, m.MirroredElemLabels](t, m.ordinal(t))

      def fromNative(nativeJs: js.Any): T =
        if !nativeJs.asInstanceOf[js.Object].hasOwnProperty("@type") then
          throw IllegalArgumentException("Missing required @type property: " + JSON.stringify(nativeJs))
        val typeName = nativeJs.asInstanceOf[js.Dynamic].`@type`.asInstanceOf[String]
        adtSumFromNative[T, m.MirroredLabel, m.MirroredElemTypes, m.MirroredElemLabels](typeName, nativeJs)

  /**
   * If the Sum type has any element that is not Singleton, we summon the NativeConverters
   * for the elements we want to convert.
   */
  private inline def adtSumToNative[T, Mets <: Tuple, Mels <: Tuple](t: T, ordinal: Int, i: Int = 0): js.Any =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) => // can never reach
      case _: ((met *: metsTail), (mel *: melsTail)) =>
        if i == ordinal then
          val res = summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[T]].toNative(t)
          res.asInstanceOf[js.Dynamic].`@type` = constString[mel]
          res
        else adtSumToNative[T, metsTail, melsTail](t, ordinal, i + 1)

  private inline def adtSumFromNative[T, Label, Mets <: Tuple, Mels <: Tuple](
    typeName: String,
    nativeJs: js.Any
  ): T =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) => throw IllegalArgumentException(
        "Cannot decode " + constString[Label] + " with " + JSON.stringify(nativeJs))
      case _: ((met *: metsTail), (mel *: melsTail)) =>
        if constString[mel] == typeName then
          summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[T]].fromNative(nativeJs)
        else
          adtSumFromNative[T, Label, metsTail, melsTail](typeName, nativeJs)

  /**
   * Makes a JS Object with a property for every Scala field.
   */
  private inline def productToNative[T](m: Mirror.ProductOf[T], p: Product): js.Any =
    buildProductToNative[m.MirroredElemTypes, m.MirroredElemLabels](p)
  
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

  /**
   * Builds a Scala product of type T from the JS Object properties.
   */
  private inline def nativeToProduct[T](m: Mirror.ProductOf[T], nativeJs: js.Any): T =
    buildNativeProduct[T, m.MirroredElemTypes, m.MirroredElemLabels](
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
