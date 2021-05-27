package org.getshaka.nativeconverter

import scala.annotation.implicitNotFound
import scala.collection.{Iterable, Map, Seq, Set}
import scala.collection.mutable.{ArrayBuffer, Buffer, HashMap, HashSet}
import scala.collection.immutable.List
import scala.collection.immutable
import scala.deriving.Mirror
import scala.compiletime.{constValue, constValueTuple, erasedValue, error, summonFrom, summonInline, summonAll}
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.{JSON, WrappedArray, WrappedMap}

/**
 * Typeclass for converting between Scala.js and native JavaScript.
 * @tparam A the type to convert
 */
@implicitNotFound("Could not find an implicit NativeConverter[${A}]")
trait NativeConverter[A]:
  extension (a: A)
   /**
    * Convert a Scala.js type to native JavaScript.
    * <br>
    * This is an extension method, so it's available on all types
    * that `derive NativeConverter`. To use for other types, like Int, summon
    * a NativeConverter and use: `NativeConverter[Int].toNative(123)`
    * <br>
    * Any RuntimeException subclass may be thrown if conversion fails.
    */
    def toNative: js.Any

    /**
     * Convert type A to a JSON string
     * <br>
     * Any RuntimeException subclass may be thrown if conversion fails.
     */
    def toJson: String = JSON.stringify(a.toNative)

  /**
   * Convert a native Javascript type to Scala.js.
   * <br>
   * Any RuntimeException subclass may be thrown if conversion fails.
   */
  def fromNative(nativeJs: js.Any): A

  /**
   * Convert a Json String to type A
   * <br>
   * Any RuntimeException subclass may be thrown if conversion fails.
   */
  def fromJson(json: String): A = fromNative(JSON.parse(json))

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

  given stringConv: NativeConverter[String] with
    extension (s: String) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): String = nativeJs.asInstanceOf[String]

  given booleanConv: NativeConverter[Boolean] with
    extension (b: Boolean) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Boolean = nativeJs.asInstanceOf[Boolean]

  given byteConv: NativeConverter[Byte] with
    extension (b: Byte) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Byte = nativeJs.asInstanceOf[Byte]

  given shortConv: NativeConverter[Short] with
    extension (s: Short) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Short = nativeJs.asInstanceOf[Short]

  given intConv: NativeConverter[Int] with
    extension (i: Int) def toNative: js.Any = i.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Int = nativeJs.asInstanceOf[Int]

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given floatConv: NativeConverter[Float] with
    extension (f: Float) def toNative: js.Any = f.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Float = nativeJs.asInstanceOf[Float]

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given doubleConv: NativeConverter[Double] with
    extension (d: Double) def toNative: js.Any = d.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Double = nativeJs.asInstanceOf[Double]

  given nullConv: NativeConverter[Null] with
    extension (n: Null) def toNative: js.Any = n.asInstanceOf[js.Any]
    def fromNative(nativeJs: js.Any): Null = nativeJs.asInstanceOf[Null]

  given jsAnyConv: NativeConverter[js.Any] with
    extension (a: js.Any) def toNative: js.Any = a
    def fromNative(nativeJs: js.Any): js.Any = nativeJs

  /*
  Char Long, etc, don't precisely map to JS, so the conversion is debatable.
  Good thing is that they can be easily overriden.
   */

  given charConv: NativeConverter[Char] with
    extension (c: Char) def toNative: js.Any = c.toString
    def fromNative(nativeJs: js.Any): Char = nativeJs.asInstanceOf[String].charAt(0)

  given longConv: NativeConverter[Long] with
    extension (l: Long) def toNative: js.Any = l.toString
    def fromNative(nativeJs: js.Any): Long = nativeJs.asInstanceOf[String].toLong
  
  /*
  Functions are converted with Scala.js's helper methods in js.Any
   */

  given fConv[A]: NativeConverter[Function0[A]] with
    extension (f: Function0[A]) def toNative: js.Any =
      js.Any.fromFunction0(f)
    def fromNative(nativeJs: js.Any): Function0[A] =
      js.Any.toFunction0(nativeJs.asInstanceOf[js.Function0[A]])

  given f1Conv[A, B]: NativeConverter[Function1[A, B]] with
    extension (f: Function1[A, B]) def toNative: js.Any =
      js.Any.fromFunction1(f)
    def fromNative(nativeJs: js.Any): Function1[A, B] =
      js.Any.toFunction1(nativeJs.asInstanceOf[js.Function1[A, B]])

  given f2Conv[A, B, C]: NativeConverter[Function2[A, B, C]] with
    extension (f: Function2[A, B, C]) def toNative: js.Any =
      js.Any.fromFunction2(f)
    def fromNative(nativeJs: js.Any): Function2[A, B, C] =
      js.Any.toFunction2(nativeJs.asInstanceOf[js.Function2[A, B, C]])

  given f3Conv[A, B, C, D]: NativeConverter[Function3[A, B, C, D]] with
    extension (f: Function3[A, B, C, D]) def toNative: js.Any =
      js.Any.fromFunction3(f)
    def fromNative(nativeJs: js.Any): Function3[A, B, C, D] =
      js.Any.toFunction3(nativeJs.asInstanceOf[js.Function3[A, B, C, D]])
      
  given f4Conv[A, B, C, D, E]: NativeConverter[Function4[A, B, C, D, E]] with
    extension (f: Function4[A, B, C, D, E]) def toNative: js.Any =
      js.Any.fromFunction4(f)
    def fromNative(nativeJs: js.Any): Function4[A, B, C, D, E] =
      js.Any.toFunction4(nativeJs.asInstanceOf[js.Function4[A, B, C, D, E]])

  given f5Conv[A, B, C, D, E, F]: NativeConverter[Function5[A, B, C, D, E, F]] with
    extension (f: Function5[A, B, C, D, E, F]) def toNative: js.Any =
      js.Any.fromFunction5(f)
    def fromNative(nativeJs: js.Any): Function5[A, B, C, D, E, F] =
      js.Any.toFunction5(nativeJs.asInstanceOf[js.Function5[A, B, C, D, E, F]])
      
  given f6Conv[A, B, C, D, E, F, G]: NativeConverter[Function6[A, B, C, D, E, F, G]] with
    extension (f: Function6[A, B, C, D, E, F, G]) def toNative: js.Any =
      js.Any.fromFunction6(f)
    def fromNative(nativeJs: js.Any): Function6[A, B, C, D, E, F, G] =
      js.Any.toFunction6(nativeJs.asInstanceOf[js.Function6[A, B, C, D, E, F, G]])
      
  given f7Conv[A, B, C, D, E, F, G, H]: NativeConverter[Function7[A, B, C, D, E, F, G, H]] with
    extension (t: Function7[A, B, C, D, E, F, G, H]) def toNative: js.Any =
      js.Any.fromFunction7(t)
    def fromNative(nativeJs: js.Any): Function7[A, B, C, D, E, F, G, H] =
      js.Any.toFunction7(nativeJs.asInstanceOf[js.Function7[A, B, C, D, E, F, G, H]])
      
  given f8Conv[A, B, C, D, E, F, G, H, I]: NativeConverter[Function8[A, B, C, D, E, F, G, H, I]] with
    extension (f: Function8[A, B, C, D, E, F, G, H, I]) def toNative: js.Any =
      js.Any.fromFunction8(f)
    def fromNative(nativeJs: js.Any): Function8[A, B, C, D, E, F, G, H, I] =
      js.Any.toFunction8(nativeJs.asInstanceOf[js.Function8[A, B, C, D, E, F, G, H, I]])
      
  given f9Conv[A, B, C, D, E, F, G, H, I, J]: NativeConverter[Function9[A, B, C, D, E, F, G, H, I, J]] with
    extension (f: Function9[A, B, C, D, E, F, G, H, I, J]) def toNative: js.Any =
      js.Any.fromFunction9(f)
    def fromNative(nativeJs: js.Any): Function9[A, B, C, D, E, F, G, H, I, J] =
      js.Any.toFunction9(nativeJs.asInstanceOf[js.Function9[A, B, C, D, E, F, G, H, I, J]])
      
  given f10Conv[A, B, C, D, E, F, G, H, I, J, K]: NativeConverter[Function10[A, B, C, D, E, F, G, H, I, J, K]] with
    extension (f: Function10[A, B, C, D, E, F, G, H, I, J, K]) def toNative: js.Any =
      js.Any.fromFunction10(f)
    def fromNative(nativeJs: js.Any): Function10[A, B, C, D, E, F, G, H, I, J, K] =
      js.Any.toFunction10(nativeJs.asInstanceOf[js.Function10[A, B, C, D, E, F, G, H, I, J, K]])
      
  given f11Conv[A, B, C, D, E, F, G, H, I, J, K, L]: NativeConverter[Function11[A, B, C, D, E, F, G, H, I, J, K, L]] with
    extension (f: Function11[A, B, C, D, E, F, G, H, I, J, K, L]) def toNative: js.Any =
      js.Any.fromFunction11(f)
    def fromNative(nativeJs: js.Any): Function11[A, B, C, D, E, F, G, H, I, J, K, L] =
      js.Any.toFunction11(nativeJs.asInstanceOf[js.Function11[A, B, C, D, E, F, G, H, I, J, K, L]])

  given f12Conv[A, B, C, D, E, F, G, H, I, J, K, L, M]: NativeConverter[Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]] with
    extension (f: Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]) def toNative: js.Any =
      js.Any.fromFunction12(f)
    def fromNative(nativeJs: js.Any): Function12[A, B, C, D, E, F, G, H, I, J, K, L, M] =
      js.Any.toFunction12(nativeJs.asInstanceOf[js.Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]])
    
  given f13Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N]: NativeConverter[Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]] with
    extension (f: Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]) def toNative: js.Any =
      js.Any.fromFunction13(f)
    def fromNative(nativeJs: js.Any): Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N] =
      js.Any.toFunction13(nativeJs.asInstanceOf[js.Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]])
    
  given f14Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]: NativeConverter[Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]] with
    extension (f: Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]) def toNative: js.Any =
      js.Any.fromFunction14(f)
    def fromNative(nativeJs: js.Any): Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O] =
      js.Any.toFunction14(nativeJs.asInstanceOf[js.Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]])
  
  given f15Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]: NativeConverter[Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]] with
    extension (f: Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]) def toNative: js.Any =
      js.Any.fromFunction15(f)
    def fromNative(nativeJs: js.Any): Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P] =
      js.Any.toFunction15(nativeJs.asInstanceOf[js.Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]])
      
  given f16Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]: NativeConverter[Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]] with
    extension (f: Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]) def toNative: js.Any =
      js.Any.fromFunction16(f)
    def fromNative(nativeJs: js.Any): Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q] =
      js.Any.toFunction16(nativeJs.asInstanceOf[js.Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]])
    
  given f17Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]: NativeConverter[Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]] with
    extension (f: Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]) def toNative: js.Any =
      js.Any.fromFunction17(f)
    def fromNative(nativeJs: js.Any): Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R] =
      js.Any.toFunction17(nativeJs.asInstanceOf[js.Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]])
    
  given f18Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]: NativeConverter[Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]] with
    extension (f: Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]) def toNative: js.Any =
      js.Any.fromFunction18(f)
    def fromNative(nativeJs: js.Any): Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S] =
      js.Any.toFunction18(nativeJs.asInstanceOf[js.Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]])

  given f19Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]: NativeConverter[Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]] with
    extension (f: Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]) def toNative: js.Any =
      js.Any.fromFunction19(f)
    def fromNative(nativeJs: js.Any): Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T] =
      js.Any.toFunction19(nativeJs.asInstanceOf[js.Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]])

    given f20Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]: NativeConverter[Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]] with
      extension (f: Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]) def toNative: js.Any =
        js.Any.fromFunction20(f)
      def fromNative(nativeJs: js.Any): Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U] =
        js.Any.toFunction20(nativeJs.asInstanceOf[js.Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]])
  
  given f21Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: NativeConverter[Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]] with
    extension (f: Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]) def toNative: js.Any =
      js.Any.fromFunction21(f)
    def fromNative(nativeJs: js.Any): Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V] =
      js.Any.toFunction21(nativeJs.asInstanceOf[js.Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]])

  given f22Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]: NativeConverter[Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]] with
    extension (f: Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]) def toNative: js.Any =
      js.Any.fromFunction22(f)
    def fromNative(nativeJs: js.Any): Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W] =
      js.Any.toFunction22(nativeJs.asInstanceOf[js.Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]])
  
  /*
  Collection types. Arrays, Iterables, Seqs, Sets, Lists, and Buffers
  are serialized using JavaScript Arrays. Maps become JS objects, although only
  String keys are supported, like in JSON. The EsConverters class has conversions
  to js.Map.
   */
  
  private def makeNativeArray[T: NativeConverter](it: Iterable[T]): js.Array[js.Any] =
    val res = js.Array[js.Any]()
    for t <- it do res.push(t.toNative)
    res

  given arrayConv[A: ClassTag](using nc: NativeConverter[A]): NativeConverter[Array[A]] with
    extension (a: Array[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNative(nativeJs: js.Any): Array[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toArray

  given iArrayConv[A: ClassTag](using nc: NativeConverter[A]): NativeConverter[IArray[A]] with
    extension (a: IArray[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNative(nativeJs: js.Any): IArray[A] =
      IArray.from(nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative))

  given iterableConv[A](using nc: NativeConverter[A]): NativeConverter[Iterable[A]] with
    extension (i: Iterable[A]) def toNative: js.Any = makeNativeArray(i)
    def fromNative(nativeJs: js.Any): Iterable[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].map(nc.fromNative)

  given seqConv[A](using nc: NativeConverter[A]): NativeConverter[Seq[A]] with
    extension (s: Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(nativeJs: js.Any): Seq[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].map(nc.fromNative)

  given immutableSeqConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Seq[A]] with
    extension (s: immutable.Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(nativeJs: js.Any): immutable.Seq[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toSeq

  given setConv[A](using nc: NativeConverter[A]): NativeConverter[Set[A]] with
    extension (s: Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(nativeJs: js.Any): Set[A] =
      HashSet.from(nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative))

  given immutableSetConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Set[A]] with
    extension (s: immutable.Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(nativeJs: js.Any): immutable.Set[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toSet

  given listConv[A](using nc: NativeConverter[A]): NativeConverter[List[A]] with
    extension (l: List[A]) def toNative: js.Any = makeNativeArray(l)
    def fromNative(nativeJs: js.Any): List[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toList

  given bufferConv[A](using nc: NativeConverter[A]): NativeConverter[Buffer[A]] with
    extension (b: Buffer[A]) def toNative: js.Any = makeNativeArray(b)
    def fromNative(nativeJs: js.Any): Buffer[A] =
      nativeJs.asInstanceOf[js.Array[js.Any]].view.map(nc.fromNative).toBuffer

  given mapConv[A](using nc: NativeConverter[A]): NativeConverter[Map[String, A]] with
    extension (m: Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- m do
        res.updateDynamic(k)(v.toNative)
      res
    def fromNative(nativeJs: js.Any): Map[String, A] =
      val dict = nativeJs.asInstanceOf[js.Dictionary[js.Any]]
      val res = HashMap[String, A]()
      for (k, v) <- dict do
        res(k) = nc.fromNative(v)
      res

  given immutableMapConv[A](using nc: NativeConverter[A]): NativeConverter[immutable.Map[String, A]] with
    extension (m: immutable.Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- m do
        res.updateDynamic(k)(v.toNative)
      res
    def fromNative(nativeJs: js.Any): immutable.Map[String, A] =
      val dict = nativeJs.asInstanceOf[js.Dictionary[js.Any]]
      var res = immutable.HashMap[String, A]()
      for (k, v) <- dict do
        res = res.updated(k, nc.fromNative(v))
      res

  given optionConv[A](using nc: NativeConverter[A]): NativeConverter[Option[A]] with
    extension (o: Option[A]) def toNative: js.Any =
      o.map(_.toNative).getOrElse(null.asInstanceOf[js.Any])
    def fromNative(nativeJs: js.Any): Option[A] =
      Option(nativeJs).map(nc.fromNative)

  /**
   * Derive a NativeConverter for type T. This method is called by the compiler automatically
   * when adding `derives NativeConverter` on a class. You can also use it to derive given
   * instances anywhere, which is useful if Cross-Building a Scala.js project:
   * <br>
   * `given NativeConverter[User] = NativeConverter.derived`
   * <br>
   * Only Sum and Product types are supported
   */
  inline given derived[A](using m: Mirror.Of[A]): NativeConverter[A] =
    type Mets = m.MirroredElemTypes

    inline m match
      case s: Mirror.SumOf[A] => sumConverter[A, Mets](s)
      case p: Mirror.ProductOf[A] => productConverter(p)

  private inline def summonElementConverters[A](m: Mirror.Of[A]): IArray[Object] =
    summonAll[Tuple.Map[m.MirroredElemTypes, NativeConverter]]
      .toIArray

  private inline def mirroredElemLabels[A](m: Mirror.Of[A]): IArray[Object] =
    constValueTuple[m.MirroredElemLabels]
      .toIArray

  /**
   * If every element of the Sum type is a Singleton, then
   * serialize using the type name. Otherwise, it is an ADT
   * that is serialized the normal way, by summoning NativeConverters
   * for the elements and adding a `@type` property providing the
   * (short) class name.
   */
  private inline def sumConverter[A, Mets](m: Mirror.SumOf[A]): NativeConverter[A] =
    inline erasedValue[Mets] match
      // all of the elements were Singletons.. build simple enum case
      case _: EmptyTuple => simpleSumConverter(m)

      case _: (met *: metsTail) =>
        inline if isSingleton[met] then sumConverter[A, metsTail](m)
        else buildAdtSumConverter(m)

  /**
   * A singleton is a Product with no parameter elements
   */
  private inline def isSingleton[A]: Boolean = summonFrom[A] {
    case product: Mirror.ProductOf[A] =>
      inline erasedValue[product.MirroredElemTypes] match
        case _: EmptyTuple => true
        case _ => false
    case _ => false
  }

  private inline def simpleSumConverter[A](m: Mirror.SumOf[A]): NativeConverter[A] =
    val elementMirrors: IArray[Object] =
      summonAll[Tuple.Map[m.MirroredElemTypes, Mirror.ProductOf]]
        .toIArray
    val mels: IArray[Object] = mirroredElemLabels(m)
    type Label = m.MirroredLabel

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any =
        mels(m.ordinal(a)).asInstanceOf[String]
        
      def fromNative(nativeJs: js.Any): A =
        val typeName = nativeJs.asInstanceOf[String]
        val idx = mels.indexOf(typeName)
        require(idx >= 0, s"Sum type ${constValue[Label]} does not have element $typeName")
        val mirror = elementMirrors(idx).asInstanceOf[Mirror.ProductOf[A]]
        mirror.fromProduct(EmptyTuple)

  /**
   * Uses a `@type` property that holds the (short) class name.
   */
  private inline def buildAdtSumConverter[A](m: Mirror.SumOf[A]): NativeConverter[A] =
    type Label = m.MirroredLabel
    val converters: IArray[Object] = summonElementConverters(m)
    val mels: IArray[Object] = mirroredElemLabels(m)

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any =
        val ordinal = m.ordinal(a)
        val obj: js.Dynamic = converters(ordinal)
          .asInstanceOf[NativeConverter[A]]
          .toNative(a)
          .asInstanceOf[js.Dynamic]
        val typeName = mels(ordinal).asInstanceOf[String]
        obj.`@type` = typeName
        obj

      def fromNative(nativeJs: js.Any): A =
        require(nativeJs.asInstanceOf[js.Object].hasOwnProperty("@type"),
          "Missing required @type property: " + JSON.stringify(nativeJs))
        val typeName = nativeJs.asInstanceOf[js.Dynamic].`@type`.asInstanceOf[String]
        val idx = mels.indexOf(typeName)
        converters(idx).asInstanceOf[NativeConverter[A]].fromNative(nativeJs)

  private inline def productConverter[A](m: Mirror.ProductOf[A]): NativeConverter[A] =
    val converters: IArray[Object] = summonElementConverters(m)
    val mels: IArray[Object] = mirroredElemLabels(m)

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any =
        val res = js.Object().asInstanceOf[js.Dynamic]
        val product = a.asInstanceOf[Product]
        for i <- 0 until converters.length do
          val mel = mels(i).asInstanceOf[String]
          val converter = converters(i).asInstanceOf[NativeConverter[Any]]
          res.updateDynamic(mel)(converter.toNative(product.productElement(i)))
        res

      def fromNative(nativeJs: js.Any): A =
        val dict = nativeJs.asInstanceOf[js.Dictionary[js.Any]]
        val resArr = IArray.tabulate(converters.length)(i =>
          val converter = converters(i).asInstanceOf[NativeConverter[Any]]
          val jsonElement = dict.getOrElse(mels(i).asInstanceOf[String], throw IllegalArgumentException(
            s"Json missing required property '${mels(i)}': \n${JSON.stringify(nativeJs)}'"))
          converter.fromNative(jsonElement)
        )
        m.fromProduct(ArrayProduct(resArr))


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
