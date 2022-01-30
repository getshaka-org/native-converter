package org.getshaka.nativeconverter

import scala.annotation.implicitNotFound
import scala.collection.{Iterable, Map, Seq, Set, immutable, mutable}
import scala.collection.mutable.{ArrayBuffer, Buffer, HashMap, HashSet, Builder}
import scala.collection.immutable.List
import scala.deriving.Mirror
import scala.compiletime.{constValue, constValueTuple, erasedValue, error, summonAll, summonFrom, summonInline}
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
     *
     * This is an extension method, so it's available on all types
     * that `derive NativeConverter`. To use for other types, like Int, summon
     * a NativeConverter and use: `NativeConverter[Int].toNative(123)`
     */
    def toNative: js.Any

    /** Convert type A to a JSON string */
    def toJson: String = JSON.stringify(a.toNative)

  /**
   * Convert a native Javascript type to Scala.js.
   * Returns either A, or a String error.
   */
  def fromNativeE(ps: ParseState): Either[String, A]

  /**
   * Convert a native Javascript type to Scala.js.
   * Returns either A, or a String error.
   */
  def fromNativeE(nativeJs: js.Any): Either[String, A] =
    fromNativeE(ParseState(nativeJs))

  /**
   * Convert a native Javascript type to Scala.js.
   * Returns A, or throws.
   */
  def fromNative(nativeJs: js.Any): A =
    fromNativeE(nativeJs) match
      case Right(a) => a
      case Left(e) => throw RuntimeException(e)

  /**
   * Convert a Json String to type A.
   * Returns either A, or a String error.
   */
  def fromJsonE(json: String): Either[String, A] =
    try fromNativeE(JSON.parse(json))
    catch case t => Left("Not valid Json: \n" + json)

  /**
   * Convert a Json String to type A.
   * Returns A, or throws.
   */
  def fromJson(json: String): A =
    fromJsonE(json) match
      case Right(a) => a
      case Left(e) => throw RuntimeException(e)

object NativeConverter:

  inline def apply[A](using nc: NativeConverter[A]): NativeConverter[A] = nc

  private type ImplicitlyJsAny = String | Boolean | Byte | Short | Int | Float | Double | Null | js.Any

  given StringConv: NativeConverter[String] with
    extension (s: String) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, String] =
      ps.json.asInstanceOf[Any] match
        case s: String => Right(s)
        case _ => ps.left("String")

  given BooleanConv: NativeConverter[Boolean] with
    extension (b: Boolean) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Boolean] =
      ps.json.asInstanceOf[Any] match
        case b: Boolean => Right(b)
        case _ => ps.left("Boolean")

  given ByteConv: NativeConverter[Byte] with
    extension (b: Byte) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Byte] =
      ps.json.asInstanceOf[Any] match
        case b: Byte => Right(b)
        case _ => ps.left("Byte")

  given ShortConv: NativeConverter[Short] with
    extension (s: Short) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Short] =
      ps.json.asInstanceOf[Any] match
        case s: Short => Right(s)
        case _ => ps.left("Short")

  given IntConv: NativeConverter[Int] with
    extension (i: Int) def toNative: js.Any = i.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Int] =
      ps.json.asInstanceOf[Any] match
        case i: Int => Right(i)
        case _ => ps.left("Int")

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given FloatConv: NativeConverter[Float] with
    extension (f: Float) def toNative: js.Any = f.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Float] =
      ps.json.asInstanceOf[Any] match
        case f: Float => Right(f)
        case _ => ps.left("Float")

  /**
   * Infinity and NaN are not supported, since JSON does not support
   * serializing those values.
   */
  given DoubleConv: NativeConverter[Double] with
    extension (d: Double) def toNative: js.Any = d.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Double] =
      ps.json.asInstanceOf[Any] match
        case d: Double => Right(d)
        case _ => ps.left("Float")

  given NullConv: NativeConverter[Null] with
    extension (n: Null) def toNative: js.Any = n.asInstanceOf[js.Any]
    def fromNativeE(ps: ParseState): Either[String, Null] =
      ps.json.asInstanceOf[Any] match
        case null => Right(null)
        case _ => ps.left("null")

  given JSDateConv: NativeConverter[js.Date] with
    extension (d: js.Date)
      def toNative: js.Any = d
      override def toJson: String = d.toISOString
    def fromNativeE(ps: ParseState): Either[String, js.Date] =
      ps.json match
        case d: js.Date => Right(d)
        case _ => ps.left("js.Date")
    override def fromJsonE(json: String): Either[String, js.Date] =
      try Right(new js.Date(json))
      catch case t => Left("Invalid ISO JS Date: \n" + json)

  given JSAnyConv: NativeConverter[js.Any] with
    extension (a: js.Any) def toNative: js.Any = a
    def fromNativeE(ps: ParseState): Either[String, js.Any] = Right(ps.json)

  /*
  Char Long, etc, don't precisely map to JS, so the conversion is debatable.
  Good thing is that they can be easily overriden.
  */

  given CharConv: NativeConverter[Char] with
    extension (c: Char) def toNative: js.Any = c.toString
    def fromNativeE(ps: ParseState): Either[String, Char] =
      ps.json.asInstanceOf[Any] match
        case s: String if s.nonEmpty => Right(s.charAt(0))
        case _ => ps.left("Char")

  given LongConv: NativeConverter[Long] with
    extension (l: Long) def toNative: js.Any = l.toString
    def fromNativeE(ps: ParseState): Either[String, Long] =
      ps.json.asInstanceOf[Any] match
        case i: Int => Right(i)
        case s: String => s.toLongOption.toRight(ps.left("Long").value)
        case _ => ps.left("Long")

  /*
  Functions are converted with Scala.js's helper methods in js.Any
  */

  given F0Conv[A]: NativeConverter[Function0[A]] with
    extension (f: Function0[A]) def toNative: js.Any =
      js.Any.fromFunction0(f)
    def fromNativeE(ps: ParseState): Either[String, Function0[A]] =
      Right(js.Any.toFunction0(ps.json.asInstanceOf[js.Function0[A]]))

  given F1Conv[A, B]: NativeConverter[Function1[A, B]] with
    extension (f: Function1[A, B]) def toNative: js.Any =
      js.Any.fromFunction1(f)
    def fromNativeE(ps: ParseState): Either[String, Function1[A, B]] =
      Right(js.Any.toFunction1(ps.json.asInstanceOf[js.Function1[A, B]]))

  given F2Conv[A, B, C]: NativeConverter[Function2[A, B, C]] with
    extension (f: Function2[A, B, C]) def toNative: js.Any =
      js.Any.fromFunction2(f)
    def fromNativeE(ps: ParseState): Either[String, Function2[A, B, C]] =
      Right(js.Any.toFunction2(ps.json.asInstanceOf[js.Function2[A, B, C]]))

  given F3Conv[A, B, C, D]: NativeConverter[Function3[A, B, C, D]] with
    extension (f: Function3[A, B, C, D]) def toNative: js.Any =
      js.Any.fromFunction3(f)
    def fromNativeE(ps: ParseState): Either[String, Function3[A, B, C, D]] =
      Right(js.Any.toFunction3(ps.json.asInstanceOf[js.Function3[A, B, C, D]]))

  given F4Conv[A, B, C, D, E]: NativeConverter[Function4[A, B, C, D, E]] with
    extension (f: Function4[A, B, C, D, E]) def toNative: js.Any =
      js.Any.fromFunction4(f)
    def fromNativeE(ps: ParseState): Either[String, Function4[A, B, C, D, E]] =
      Right(js.Any.toFunction4(ps.json.asInstanceOf[js.Function4[A, B, C, D, E]]))

  given F5Conv[A, B, C, D, E, F]: NativeConverter[Function5[A, B, C, D, E, F]] with
    extension (f: Function5[A, B, C, D, E, F]) def toNative: js.Any =
      js.Any.fromFunction5(f)
    def fromNativeE(ps: ParseState): Either[String, Function5[A, B, C, D, E, F]] =
      Right(js.Any.toFunction5(ps.json.asInstanceOf[js.Function5[A, B, C, D, E, F]]))

  given F6Conv[A, B, C, D, E, F, G]: NativeConverter[Function6[A, B, C, D, E, F, G]] with
    extension (f: Function6[A, B, C, D, E, F, G]) def toNative: js.Any =
      js.Any.fromFunction6(f)
    def fromNativeE(ps: ParseState): Either[String, Function6[A, B, C, D, E, F, G]] =
      Right(js.Any.toFunction6(ps.json.asInstanceOf[js.Function6[A, B, C, D, E, F, G]]))

  given F7Conv[A, B, C, D, E, F, G, H]: NativeConverter[Function7[A, B, C, D, E, F, G, H]] with
    extension (f: Function7[A, B, C, D, E, F, G, H]) def toNative: js.Any =
      js.Any.fromFunction7(f)
    def fromNativeE(ps: ParseState): Either[String, Function7[A, B, C, D, E, F, G, H]] =
      Right(js.Any.toFunction7(ps.json.asInstanceOf[js.Function7[A, B, C, D, E, F, G, H]]))

  given F8Conv[A, B, C, D, E, F, G, H, I]: NativeConverter[Function8[A, B, C, D, E, F, G, H, I]] with
    extension (f: Function8[A, B, C, D, E, F, G, H, I]) def toNative: js.Any =
      js.Any.fromFunction8(f)
    def fromNativeE(ps: ParseState): Either[String, Function8[A, B, C, D, E, F, G, H, I]] =
      Right(js.Any.toFunction8(ps.json.asInstanceOf[js.Function8[A, B, C, D, E, F, G, H, I]]))

  given F9Conv[A, B, C, D, E, F, G, H, I, J]: NativeConverter[Function9[A, B, C, D, E, F, G, H, I, J]] with
    extension (f: Function9[A, B, C, D, E, F, G, H, I, J]) def toNative: js.Any =
      js.Any.fromFunction9(f)
    def fromNativeE(ps: ParseState): Either[String, Function9[A, B, C, D, E, F, G, H, I, J]] =
      Right(js.Any.toFunction9(ps.json.asInstanceOf[js.Function9[A, B, C, D, E, F, G, H, I, J]]))

  given F10Conv[A, B, C, D, E, F, G, H, I, J, K]: NativeConverter[Function10[A, B, C, D, E, F, G, H, I, J, K]] with
    extension (f: Function10[A, B, C, D, E, F, G, H, I, J, K]) def toNative: js.Any =
      js.Any.fromFunction10(f)
    def fromNativeE(ps: ParseState): Either[String, Function10[A, B, C, D, E, F, G, H, I, J, K]] =
      Right(js.Any.toFunction10(ps.json.asInstanceOf[js.Function10[A, B, C, D, E, F, G, H, I, J, K]]))

  given F11Conv[A, B, C, D, E, F, G, H, I, J, K, L]: NativeConverter[Function11[A, B, C, D, E, F, G, H, I, J, K, L]] with
    extension (f: Function11[A, B, C, D, E, F, G, H, I, J, K, L]) def toNative: js.Any =
      js.Any.fromFunction11(f)
    def fromNativeE(ps: ParseState): Either[String, Function11[A, B, C, D, E, F, G, H, I, J, K, L]] =
      Right(js.Any.toFunction11(ps.json.asInstanceOf[js.Function11[A, B, C, D, E, F, G, H, I, J, K, L]]))

  given F12Conv[A, B, C, D, E, F, G, H, I, J, K, L, M]: NativeConverter[Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]] with
    extension (f: Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]) def toNative: js.Any =
      js.Any.fromFunction12(f)
    def fromNativeE(ps: ParseState): Either[String, Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]] =
      Right(js.Any.toFunction12(ps.json.asInstanceOf[js.Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]]))

  given F13Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N]: NativeConverter[Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]] with
    extension (f: Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]) def toNative: js.Any =
      js.Any.fromFunction13(f)
    def fromNativeE(ps: ParseState): Either[String, Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]] =
      Right(js.Any.toFunction13(ps.json.asInstanceOf[js.Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]]))

  given F14Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]: NativeConverter[Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]] with
    extension (f: Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]) def toNative: js.Any =
      js.Any.fromFunction14(f)
    def fromNativeE(ps: ParseState): Either[String, Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]] =
      Right(js.Any.toFunction14(ps.json.asInstanceOf[js.Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]]))

  given F15Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]: NativeConverter[Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]] with
    extension (f: Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]) def toNative: js.Any =
      js.Any.fromFunction15(f)
    def fromNativeE(ps: ParseState): Either[String, Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]] =
      Right(js.Any.toFunction15(ps.json.asInstanceOf[js.Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]]))

  given F16Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]: NativeConverter[Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]] with
    extension (f: Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]) def toNative: js.Any =
      js.Any.fromFunction16(f)
    def fromNativeE(ps: ParseState): Either[String, Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]] =
      Right(js.Any.toFunction16(ps.json.asInstanceOf[js.Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]]))

  given F17Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]: NativeConverter[Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]] with
    extension (f: Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]) def toNative: js.Any =
      js.Any.fromFunction17(f)
    def fromNativeE(ps: ParseState): Either[String, Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]] =
      Right(js.Any.toFunction17(ps.json.asInstanceOf[js.Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]]))

  given F18Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]: NativeConverter[Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]] with
    extension (f: Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]) def toNative: js.Any =
      js.Any.fromFunction18(f)
    def fromNativeE(ps: ParseState): Either[String, Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]] =
      Right(js.Any.toFunction18(ps.json.asInstanceOf[js.Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]]))

  given F19Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]: NativeConverter[Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]] with
    extension (f: Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]) def toNative: js.Any =
      js.Any.fromFunction19(f)
    def fromNativeE(ps: ParseState): Either[String, Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]] =
      Right(js.Any.toFunction19(ps.json.asInstanceOf[js.Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]]))

  given F20Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]: NativeConverter[Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]] with
    extension (f: Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]) def toNative: js.Any =
      js.Any.fromFunction20(f)
    def fromNativeE(ps: ParseState): Either[String, Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]] =
      Right(js.Any.toFunction20(ps.json.asInstanceOf[js.Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]]))

  given F21Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: NativeConverter[Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]] with
    extension (f: Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]) def toNative: js.Any =
      js.Any.fromFunction21(f)
    def fromNativeE(ps: ParseState): Either[String, Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]] =
      Right(js.Any.toFunction21(ps.json.asInstanceOf[js.Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]]))

  given F22Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]: NativeConverter[Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]] with
    extension (f: Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]) def toNative: js.Any =
      js.Any.fromFunction22(f)
    def fromNativeE(ps: ParseState): Either[String, Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]] =
      Right(js.Any.toFunction22(ps.json.asInstanceOf[js.Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]]))

  private def makeNativeArray[A: NativeConverter](it: Iterable[A]): js.Array[js.Any] =
    val res = js.Array[js.Any]()
    for t <- it do res.push(t.toNative)
    res

  private def asJSArray(ps: ParseState): Either[String, js.Array[js.Any]] =
    ps.json match
      case a: js.Array[?] => Right(a.asInstanceOf[js.Array[js.Any]])
      case _ => ps.left("js.Array")

  private def jsonArrayToCollection[A, C[_]](
    ps: ParseState,
    arr: js.Array[js.Any],
    resBuilder: Builder[A, C[A]]
  )(using nc: NativeConverter[A]): Either[String, C[A]] =
    resBuilder.sizeHint(arr.length)

    var i = 0
    val it = arr.iterator
    while it.hasNext do
      val part = nc.fromNativeE(ps.atIndex(i, it.next()))
      i += 1
      part match
        case l: Left[?, ?] => return l.asInstanceOf[Left[String, C[A]]]
        case Right(a) => resBuilder.addOne(a)

    Right(resBuilder.result())

  private def jsToCollection[A: NativeConverter, C[_]](
    ps: ParseState,
    builder: Builder[A, C[A]]
  ): Either[String, C[A]] =
    for
      jsArr <- asJSArray(ps)
      col <- jsonArrayToCollection(ps, jsArr, builder)
    yield col

  given ArrayConv[A: ClassTag: NativeConverter]: NativeConverter[Array[A]] with
    extension (a: Array[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNativeE(ps: ParseState): Either[String, Array[A]] = jsToCollection(ps, Array.newBuilder)

  given IArrayConv[A: ClassTag: NativeConverter]: NativeConverter[IArray[A]] with
    extension (a: IArray[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNativeE(ps: ParseState): Either[String, IArray[A]] = jsToCollection(ps, IArray.newBuilder)

  given IterableConv[A: NativeConverter]: NativeConverter[Iterable[A]] with
    extension (a: Iterable[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNativeE(ps: ParseState): Either[String, Iterable[A]] = jsToCollection(ps, ArrayBuffer.newBuilder)

  given SeqConv[A: NativeConverter]: NativeConverter[Seq[A]] with
    extension (s: Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNativeE(ps: ParseState): Either[String, Seq[A]] = jsToCollection(ps, ArrayBuffer.newBuilder)

  given ImmutableSeqConv[A: NativeConverter]: NativeConverter[immutable.Seq[A]] with
    extension (s: immutable.Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNativeE(ps: ParseState): Either[String, immutable.Seq[A]] = jsToCollection(ps, immutable.Seq.newBuilder)

  given SetCodec[A: NativeConverter]: NativeConverter[Set[A]] with
    extension (s: Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNativeE(ps: ParseState): Either[String, Set[A]] = jsToCollection(ps, HashSet.newBuilder)

  given ImmutableSetCodec[A: NativeConverter]: NativeConverter[immutable.Set[A]] with
    extension (s: immutable.Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNativeE(ps: ParseState): Either[String, immutable.Set[A]] = jsToCollection(ps, immutable.Set.newBuilder)

  given ListConv[A: NativeConverter]: NativeConverter[List[A]] with
    extension (l: List[A]) def toNative: js.Any = makeNativeArray(l)
    def fromNativeE(ps: ParseState): Either[String, List[A]] = jsToCollection(ps, List.newBuilder)

  given BufferConv[A: NativeConverter]: NativeConverter[Buffer[A]] with
    extension (b: Buffer[A]) def toNative: js.Any = makeNativeArray(b)
    def fromNativeE(ps: ParseState): Either[String, mutable.Buffer[A]] = jsToCollection(ps, ArrayBuffer.newBuilder)

  private def asJSDict(ps: ParseState): Either[String, js.Dictionary[js.Any]] =
    ps.json match
      case o: js.Object => Right(o.asInstanceOf[js.Dictionary[js.Any]])
      case _ => ps.left("js.Object")

  private def jsToCollection[A, C[_, _]](
    ps: ParseState,
    dict: js.Dictionary[js.Any],
    resBuilder: Builder[(String, A), C[String, A]]
  )(using nc: NativeConverter[A]): Either[String, C[String, A]] =
    resBuilder.sizeHint(dict.size)
    val it = dict.iterator

    while it.hasNext do
      val (k, v) = it.next()

      val key: String = k.asInstanceOf[Any] match
        case s: String => s
        case _ => return ps.left("all String keys")

      val value: A = nc.fromNativeE(ps.atKey(key, v)) match
        case Right(a) => a
        case l: Left[?, ?] => return l.asInstanceOf[Left[String, C[String, A]]]

      resBuilder.addOne((key, value))

    Right(resBuilder.result())

  given MapConv[A: NativeConverter]: NativeConverter[Map[String, A]] with
    extension (m: Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- m do
        res.updateDynamic(k)(v.toNative)
      res

    def fromNativeE(ps: ParseState): Either[String, Map[String, A]] =
      for
        jsDict <- asJSDict(ps)
        m <- jsToCollection(ps, jsDict, HashMap.newBuilder)
      yield m

  given ImmutableMapConv[A: NativeConverter]: NativeConverter[immutable.Map[String, A]] with
    extension (m: immutable.Map[String, A]) def toNative: js.Any =
      val res = js.Object().asInstanceOf[js.Dynamic]
      for (k, v) <- m do
        res.updateDynamic(k)(v.toNative)
      res

    def fromNativeE(ps: ParseState): Either[String, Predef.Map[String, A]] =
      for
        jsDict <- asJSDict(ps)
        m <- jsToCollection(ps, jsDict, immutable.Map.newBuilder)
      yield m

  given OptionCodec[A: NativeConverter]: NativeConverter[Option[A]] with
    extension (o: Option[A]) def toNative: js.Any =
      o.map(_.toNative).getOrElse(null.asInstanceOf[js.Any])

    def fromNativeE(ps: ParseState): Either[String, Option[A]] =
      ps.json match
        case null => Right(None)
        case _ => NativeConverter[A].fromNativeE(ps).map(Some(_))

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
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    inline m match
      case p: Mirror.ProductOf[A] =>
        new NativeConverter[A]:
          extension (a: A) def toNative: js.Any =
            productToNative[Mets, Mels](a.asInstanceOf[Product])
          def fromNativeE(ps: ParseState): Either[String, A] =
            for
              jsDict <- asJSDict(ps)
              resArr = Array.ofDim[Any](constValue[Tuple.Size[Mets]])
              a <- nativeToProduct[A, Mets, Mels](p, resArr, ps, jsDict)
            yield a

      case s: Mirror.SumOf[A] => sumConverter[A, Mets](s)

  private inline def productToNative[Mets, Mels](
    p: Product,
    i: Int = 0,
    res: js.Dynamic = js.Object().asInstanceOf[js.Dynamic]
  ): js.Any =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      // base case
      case _: (EmptyTuple, EmptyTuple) => res

      case _: (ImplicitlyJsAny *: metsTail, mel *: melsTail) =>
        res.updateDynamic(constValue[mel & String])(p.productElement(i).asInstanceOf[js.Any])
        productToNative[metsTail, melsTail](p, i + 1, res)

      case _: (met *: metsTail, mel *: melsTail) =>
        val nc = summonInline[NativeConverter[met]]
        val nativeElem = nc.toNative(p.productElement(i).asInstanceOf[met])
        res.updateDynamic(constValue[mel & String])(nativeElem)
        productToNative[metsTail, melsTail](p, i + 1, res)

  private inline def nativeToProduct[A, Mets, Mels](
    mirror: Mirror.ProductOf[A],
    resArr: Array[Any],
    ps: ParseState,
    jsDict: js.Dictionary[js.Any],
    i: Int = 0
  ): Either[String, A] =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        Right(mirror.fromProduct(ArrayProduct(resArr)))

      case _: (met *: metsTail, mel *: melsTail) =>
        val nc = summonInline[NativeConverter[met]]
        val key = constValue[mel & String]
        val elementJs = jsDict(key)
        val elementEither = nc.fromNativeE(ps.atKey(key, elementJs))
        elementEither match
          case l: Left[?, ?] => l.asInstanceOf[Left[String, A]]
          case Right(v) =>
            resArr(i) = v
            nativeToProduct[A, metsTail, melsTail](mirror, resArr, ps, jsDict, i + 1)

  private inline def sumConverter[A, Mets](m: Mirror.SumOf[A]): NativeConverter[A] =
    inline erasedValue[Mets] match
      case _: (met *: metsTail) =>
        inline if isSingleton[met] then sumConverter[A, metsTail](m)
        else adtSumConverter(m)

      case _: EmptyTuple => simpleSumConverter(m)

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

  private inline def simpleSumConverter[A](m: Mirror.SumOf[A]): NativeConverter[A] =
    type Mets = m.MirroredElemTypes
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any = simpleSumToNative[Mels](m.ordinal(a))
      def fromNativeE(ps: ParseState): Either[String, A] =
        for
          name <-
            ps.json.asInstanceOf[Any] match
              case s: String => Right(s)
              case _ => ps.left("String")
          a <- simpleSumFromNative[A, Label, Mets, Mels](ps, name)
        yield a

  private inline def simpleSumToNative[Mels](n: Int, i: Int = 0): js.Any =
    inline erasedValue[Mels] match
      case _: EmptyTuple => // can never reach
      case _: (mel *: melsTail) =>
        if i == n then constValue[mel].asInstanceOf[js.Any]
        else simpleSumToNative[melsTail](n, i + 1)

  private inline def simpleSumFromNative[A, Label, Mets, Mels](
    ps: ParseState,
    name: String
  ): Either[String, A] =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        ps.left(s"a member of Sum type ${constValue[Label]}")
      case _: (met *: metsTail, mel *: melsTail) =>
        if constValue[mel] == name then
          Right(summonInline[Mirror.ProductOf[met & A]].fromProduct(EmptyTuple))
        else
          simpleSumFromNative[A, Label, metsTail, melsTail](ps, name)

  private inline def adtSumConverter[A](m: Mirror.SumOf[A]): NativeConverter[A] =
    type Mets = m.MirroredElemTypes
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any = adtSumToNative[A, Mets, Mels](a, m.ordinal(a))
      def fromNativeE(ps: ParseState): Either[String, A] =
        for
          jsDict <- asJSDict(ps)
          typeName <-
            jsDict("@type").asInstanceOf[Any] match
              case s: String => Right(s)
              case _ => ps.left("js.Object with existing '@type' key/value")
          a <- adtSumFromNative[A, Label, Mets, Mels](ps, typeName)
        yield a


  private inline def adtSumToNative[A, Mets, Mels](a: A, ordinal: Int, i: Int = 0): js.Any =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) => // can never reach
      case _: (met *: metsTail, mel *: melsTail) =>
        if i == ordinal then
          val res = summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[A]].toNative(a)
          res.asInstanceOf[js.Dynamic].`@type` = constValue[mel].asInstanceOf[js.Any]
          res
        else
          adtSumToNative[A, metsTail, melsTail](a, ordinal, i + 1)

  private inline def adtSumFromNative[A, Label, Mets, Mels](
    ps: ParseState,
    typeName: String
  ): Either[String, A] =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        ps.left(s"a valid '@type' for ${constValue[Label]}")
      case _: (met *: metsTail, mel *: melsTail) =>
        if constValue[mel] == typeName then
          summonInline[NativeConverter[met]].asInstanceOf[NativeConverter[A]].fromNativeE(ps)
        else
          adtSumFromNative[A, Label, metsTail, melsTail](ps, typeName)

