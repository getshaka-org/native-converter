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
import java.util.UUID

/** Typeclass for converting between Scala.js and native JavaScript.
  * @tparam A
  *   the type to convert
  */
@implicitNotFound("Could not find an implicit NativeConverter[${A}]")
trait NativeConverter[A]:
  extension (a: A)
    /** Convert a Scala.js type to native JavaScript.
      *
      * This is an extension method, so it's available on all types that `derive NativeConverter`. To use for other types, like Int, summon a NativeConverter and use: `NativeConverter[Int].toNative(123)`
      */
    def toNative: js.Any

    /** Convert type A to a JSON string */
    def toJson: String = JSON.stringify(a.toNative)

  /** Convert a native Javascript type to Scala.js. Returns either A, or a String error.
    */
  def fromNative(ps: ParseState): A

  /** Convert a native Javascript type to Scala.js. Returns either A, or a String error.
    */
  def fromNative(nativeJs: js.Any): A =
    fromNative(ParseState(nativeJs))

  /** Convert a Json String to type A. Returns either A, or a String error.
    */
  def fromJson(json: String): A =
    fromNative(JSON.parse(json))

object NativeConverter:

  inline def apply[A](using nc: NativeConverter[A]): NativeConverter[A] = nc

  private type ImplicitlyJsAny = String | Boolean | Byte | Short | Int | Float | Double | Null | js.Any

  given StringConv: NativeConverter[String] with
    extension (s: String) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): String =
      try ps.json.asInstanceOf[String]
      catch case _ => ps.fail("String")

  given BooleanConv: NativeConverter[Boolean] with
    extension (b: Boolean) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Boolean =
      try ps.json.asInstanceOf[Boolean]
      catch case _ => ps.fail("Boolean")

  given ByteConv: NativeConverter[Byte] with
    extension (b: Byte) def toNative: js.Any = b.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Byte =
      try ps.json.asInstanceOf[Byte]
      catch case _ => ps.fail("Byte")

  given ShortConv: NativeConverter[Short] with
    extension (s: Short) def toNative: js.Any = s.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Short =
      try ps.json.asInstanceOf[Short]
      catch case _ => ps.fail("Short")

  given IntConv: NativeConverter[Int] with
    extension (i: Int) def toNative: js.Any = i.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Int =
      try ps.json.asInstanceOf[Int]
      catch case _ => ps.fail("Int")

  /** Infinity and NaN are not supported, since JSON does not support serializing those values.
    */
  given FloatConv: NativeConverter[Float] with
    extension (f: Float) def toNative: js.Any = f.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Float =
      try
        ps.json.asInstanceOf[Any] match
          case f: Float  => f
          case d: Double => d.toFloat
          case s: String => s.toFloat
      catch case _ => ps.fail("Float")

  /** Infinity and NaN are not supported, since JSON does not support serializing those values.
    */
  given DoubleConv: NativeConverter[Double] with
    extension (d: Double) def toNative: js.Any = d.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Double =
      try
        ps.json.asInstanceOf[Any] match
          case d: Double => d
          case f: Float  => f.toDouble
          case s: String => s.toDouble
      catch case _ => ps.fail("Double")

  given NullConv: NativeConverter[Null] with
    extension (n: Null) def toNative: js.Any = n.asInstanceOf[js.Any]
    def fromNative(ps: ParseState): Null =
      try ps.json.asInstanceOf[Null]
      catch case _ => ps.fail("null")

  given JSDateConv: NativeConverter[js.Date] with
    extension (d: js.Date)
      def toNative: js.Any = d
      override def toJson: String = d.toISOString
    def fromNative(ps: ParseState): js.Date =
      try ps.json.asInstanceOf[js.Date]
      catch case _ => ps.fail("js.Date")
    override def fromJson(json: String): js.Date =
      new js.Date(json)

  given JSAnyConv: NativeConverter[js.Any] with
    extension (a: js.Any) def toNative: js.Any = a
    def fromNative(ps: ParseState): js.Any = ps.json

  /*
  Char Long, etc, don't precisely map to JS, so the conversion is debatable.
  Good thing is that they can be easily overriden.
   */

  given CharConv: NativeConverter[Char] with
    extension (c: Char) def toNative: js.Any = c.toString
    def fromNative(ps: ParseState): Char =
      ps.json.asInstanceOf[Any] match
        case s: String if s.nonEmpty => s.charAt(0)
        case _                       => ps.fail("Char")

  given LongConv: NativeConverter[Long] with
    extension (l: Long) def toNative: js.Any = l.toString
    def fromNative(ps: ParseState): Long =
      ps.json.asInstanceOf[Any] match
        case i: Int    => i
        case s: String => s.toLongOption.getOrElse(ps.fail("Long"))
        case _         => ps.fail("Long")

  given UUIDConv: NativeConverter[UUID] with
    extension (uuid: UUID) def toNative: js.Any = uuid.toString

    def fromNative(ps: ParseState): UUID =
      ps.json.asInstanceOf[Any] match
        case uuid: UUID => uuid
        case s: String =>
          try UUID.fromString(s)
          catch case t => ps.fail("UUID")
        case ab: Array[Byte] =>
          try UUID.nameUUIDFromBytes(ab)
          catch case t => ps.fail("UUID")
        case _ => ps.fail("UUID")

  /*
  Functions are converted with Scala.js's helper methods in js.Any
   */

  given F0Conv[A]: NativeConverter[Function0[A]] with
    extension (f: Function0[A])
      def toNative: js.Any =
        js.Any.fromFunction0(f)
    def fromNative(ps: ParseState): Function0[A] =
      js.Any.toFunction0(ps.json.asInstanceOf[js.Function0[A]])

  given F1Conv[A, B]: NativeConverter[Function1[A, B]] with
    extension (f: Function1[A, B])
      def toNative: js.Any =
        js.Any.fromFunction1(f)
    def fromNative(ps: ParseState): Function1[A, B] =
      js.Any.toFunction1(ps.json.asInstanceOf[js.Function1[A, B]])

  given F2Conv[A, B, C]: NativeConverter[Function2[A, B, C]] with
    extension (f: Function2[A, B, C])
      def toNative: js.Any =
        js.Any.fromFunction2(f)
    def fromNative(ps: ParseState): Function2[A, B, C] =
      js.Any.toFunction2(ps.json.asInstanceOf[js.Function2[A, B, C]])

  given F3Conv[A, B, C, D]: NativeConverter[Function3[A, B, C, D]] with
    extension (f: Function3[A, B, C, D])
      def toNative: js.Any =
        js.Any.fromFunction3(f)
    def fromNative(ps: ParseState): Function3[A, B, C, D] =
      js.Any.toFunction3(ps.json.asInstanceOf[js.Function3[A, B, C, D]])

  given F4Conv[A, B, C, D, E]: NativeConverter[Function4[A, B, C, D, E]] with
    extension (f: Function4[A, B, C, D, E])
      def toNative: js.Any =
        js.Any.fromFunction4(f)
    def fromNative(ps: ParseState): Function4[A, B, C, D, E] =
      js.Any.toFunction4(ps.json.asInstanceOf[js.Function4[A, B, C, D, E]])

  given F5Conv[A, B, C, D, E, F]: NativeConverter[Function5[A, B, C, D, E, F]] with
    extension (f: Function5[A, B, C, D, E, F])
      def toNative: js.Any =
        js.Any.fromFunction5(f)
    def fromNative(ps: ParseState): Function5[A, B, C, D, E, F] =
      js.Any.toFunction5(ps.json.asInstanceOf[js.Function5[A, B, C, D, E, F]])

  given F6Conv[A, B, C, D, E, F, G]: NativeConverter[Function6[A, B, C, D, E, F, G]] with
    extension (f: Function6[A, B, C, D, E, F, G])
      def toNative: js.Any =
        js.Any.fromFunction6(f)
    def fromNative(ps: ParseState): Function6[A, B, C, D, E, F, G] =
      js.Any.toFunction6(ps.json.asInstanceOf[js.Function6[A, B, C, D, E, F, G]])

  given F7Conv[A, B, C, D, E, F, G, H]: NativeConverter[Function7[A, B, C, D, E, F, G, H]] with
    extension (f: Function7[A, B, C, D, E, F, G, H])
      def toNative: js.Any =
        js.Any.fromFunction7(f)
    def fromNative(ps: ParseState): Function7[A, B, C, D, E, F, G, H] =
      js.Any.toFunction7(
        ps.json.asInstanceOf[js.Function7[A, B, C, D, E, F, G, H]]
      )

  given F8Conv[A, B, C, D, E, F, G, H, I]: NativeConverter[Function8[A, B, C, D, E, F, G, H, I]] with
    extension (f: Function8[A, B, C, D, E, F, G, H, I])
      def toNative: js.Any =
        js.Any.fromFunction8(f)
    def fromNative(ps: ParseState): Function8[A, B, C, D, E, F, G, H, I] =
      js.Any.toFunction8(
        ps.json.asInstanceOf[js.Function8[A, B, C, D, E, F, G, H, I]]
      )

  given F9Conv[A, B, C, D, E, F, G, H, I, J]: NativeConverter[Function9[A, B, C, D, E, F, G, H, I, J]] with
    extension (f: Function9[A, B, C, D, E, F, G, H, I, J])
      def toNative: js.Any =
        js.Any.fromFunction9(f)
    def fromNative(ps: ParseState): Function9[A, B, C, D, E, F, G, H, I, J] =
      js.Any.toFunction9(
        ps.json.asInstanceOf[js.Function9[A, B, C, D, E, F, G, H, I, J]]
      )

  given F10Conv[A, B, C, D, E, F, G, H, I, J, K]: NativeConverter[Function10[A, B, C, D, E, F, G, H, I, J, K]] with
    extension (f: Function10[A, B, C, D, E, F, G, H, I, J, K])
      def toNative: js.Any =
        js.Any.fromFunction10(f)
    def fromNative(
        ps: ParseState
    ): Function10[A, B, C, D, E, F, G, H, I, J, K] =
      js.Any.toFunction10(
        ps.json.asInstanceOf[js.Function10[A, B, C, D, E, F, G, H, I, J, K]]
      )

  given F11Conv[A, B, C, D, E, F, G, H, I, J, K, L]: NativeConverter[Function11[A, B, C, D, E, F, G, H, I, J, K, L]] with
    extension (f: Function11[A, B, C, D, E, F, G, H, I, J, K, L])
      def toNative: js.Any =
        js.Any.fromFunction11(f)
    def fromNative(
        ps: ParseState
    ): Function11[A, B, C, D, E, F, G, H, I, J, K, L] =
      js.Any.toFunction11(
        ps.json.asInstanceOf[js.Function11[A, B, C, D, E, F, G, H, I, J, K, L]]
      )

  given F12Conv[A, B, C, D, E, F, G, H, I, J, K, L, M]: NativeConverter[Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]] with
    extension (f: Function12[A, B, C, D, E, F, G, H, I, J, K, L, M])
      def toNative: js.Any =
        js.Any.fromFunction12(f)
    def fromNative(
        ps: ParseState
    ): Function12[A, B, C, D, E, F, G, H, I, J, K, L, M] =
      js.Any.toFunction12(
        ps.json
          .asInstanceOf[js.Function12[A, B, C, D, E, F, G, H, I, J, K, L, M]]
      )

  given F13Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N]: NativeConverter[Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]] with
    extension (f: Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N])
      def toNative: js.Any =
        js.Any.fromFunction13(f)
    def fromNative(
        ps: ParseState
    ): Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N] =
      js.Any.toFunction13(
        ps.json
          .asInstanceOf[js.Function13[A, B, C, D, E, F, G, H, I, J, K, L, M, N]]
      )

  given F14Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]: NativeConverter[Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]] with
    extension (f: Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O])
      def toNative: js.Any =
        js.Any.fromFunction14(f)
    def fromNative(
        ps: ParseState
    ): Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O] =
      js.Any.toFunction14(
        ps.json.asInstanceOf[
          js.Function14[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]
        ]
      )

  given F15Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]: NativeConverter[
    Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]
  ] with
    extension (f: Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P])
      def toNative: js.Any =
        js.Any.fromFunction15(f)
    def fromNative(
        ps: ParseState
    ): Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P] =
      js.Any.toFunction15(
        ps.json.asInstanceOf[
          js.Function15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]
        ]
      )

  given F16Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]: NativeConverter[
    Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]
  ] with
    extension (f: Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q])
      def toNative: js.Any =
        js.Any.fromFunction16(f)
    def fromNative(
        ps: ParseState
    ): Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q] =
      js.Any.toFunction16(
        ps.json.asInstanceOf[
          js.Function16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q]
        ]
      )

  given F17Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]: NativeConverter[
    Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]
  ] with
    extension (
        f: Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]
    )
      def toNative: js.Any =
        js.Any.fromFunction17(f)
    def fromNative(
        ps: ParseState
    ): Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R] =
      js.Any.toFunction17(
        ps.json.asInstanceOf[
          js.Function17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R]
        ]
      )

  given F18Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]: NativeConverter[
    Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]
  ] with
    extension (
        f: Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]
    )
      def toNative: js.Any =
        js.Any.fromFunction18(f)
    def fromNative(
        ps: ParseState
    ): Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S] =
      js.Any.toFunction18(
        ps.json.asInstanceOf[
          js.Function18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S]
        ]
      )

  given F19Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]: NativeConverter[
    Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]
  ] with
    extension (f: Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]) def toNative: js.Any = js.Any.fromFunction19(f)
    def fromNative(ps: ParseState): Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T] =
      js.Any.toFunction19(ps.json.asInstanceOf[js.Function19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T]])

  given F20Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]: NativeConverter[Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]] with
    extension (f: Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]) def toNative: js.Any = js.Any.fromFunction20(f)
    def fromNative(ps: ParseState): Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U] =
      js.Any.toFunction20(ps.json.asInstanceOf[js.Function20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U]])

  given F21Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]: NativeConverter[Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]] with
    extension (f: Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V])
      def toNative: js.Any =
        js.Any.fromFunction21(f)
    def fromNative(ps: ParseState): Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V] = js.Any.toFunction21(ps.json.asInstanceOf[js.Function21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V]])

  given F22Conv[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]: NativeConverter[Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]] with
    extension (f: Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W])
      def toNative: js.Any =
        js.Any.fromFunction22(f)
    def fromNative(ps: ParseState): Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W] =
      js.Any.toFunction22(ps.json.asInstanceOf[js.Function22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W]])

  private def makeNativeArray[A: NativeConverter](
      it: Iterable[A]
  ): js.Array[js.Any] =
    val res = js.Array[js.Any]()
    for t <- it do res.push(t.toNative)
    res

  private def asJSArray(ps: ParseState): js.Array[js.Any] =
    ps.json match
      case a: js.Array[?] => a.asInstanceOf[js.Array[js.Any]]
      case _              => ps.fail("js.Array")

  private def jsonArrayToCollection[A, C[_]](
      ps: ParseState,
      arr: js.Array[js.Any],
      resBuilder: Builder[A, C[A]]
  )(using nc: NativeConverter[A]): C[A] =
    resBuilder.sizeHint(arr.length)

    var i = 0
    val it = arr.iterator
    while it.hasNext do
      resBuilder += nc.fromNative(ps.atIndex(i, it.next()))
      i += 1

    resBuilder.result()

  private def jsToCollection[A: NativeConverter, C[_]](
      ps: ParseState,
      builder: Builder[A, C[A]]
  ): C[A] =
    val jsArr = asJSArray(ps)
    jsonArrayToCollection(ps, jsArr, builder)

  given ArrayConv[A: ClassTag: NativeConverter]: NativeConverter[Array[A]] with
    extension (a: Array[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNative(ps: ParseState): Array[A] =
      jsToCollection(ps, Array.newBuilder)

  given IArrayConv[A: ClassTag: NativeConverter]: NativeConverter[IArray[A]] with
    extension (a: IArray[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNative(ps: ParseState): IArray[A] =
      jsToCollection(ps, IArray.newBuilder)

  given IterableConv[A: NativeConverter]: NativeConverter[Iterable[A]] with
    extension (a: Iterable[A]) def toNative: js.Any = makeNativeArray(a)
    def fromNative(ps: ParseState): Iterable[A] =
      jsToCollection(ps, ArrayBuffer.newBuilder)

  given SeqConv[A: NativeConverter]: NativeConverter[Seq[A]] with
    extension (s: Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(ps: ParseState): Seq[A] =
      jsToCollection(ps, ArrayBuffer.newBuilder)

  given ImmutableSeqConv[A: NativeConverter]: NativeConverter[immutable.Seq[A]] with
    extension (s: immutable.Seq[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(ps: ParseState): immutable.Seq[A] =
      jsToCollection(ps, immutable.Seq.newBuilder)

  given SetCodec[A: NativeConverter]: NativeConverter[Set[A]] with
    extension (s: Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(ps: ParseState): Set[A] =
      jsToCollection(ps, HashSet.newBuilder)

  given ImmutableSetCodec[A: NativeConverter]: NativeConverter[immutable.Set[A]] with
    extension (s: immutable.Set[A]) def toNative: js.Any = makeNativeArray(s)
    def fromNative(ps: ParseState): immutable.Set[A] =
      jsToCollection(ps, immutable.Set.newBuilder)

  given ListConv[A: NativeConverter]: NativeConverter[List[A]] with
    extension (l: List[A]) def toNative: js.Any = makeNativeArray(l)
    def fromNative(ps: ParseState): List[A] =
      jsToCollection(ps, List.newBuilder)

  given VectorConv[A: NativeConverter]: NativeConverter[Vector[A]] with
    extension (v: Vector[A]) def toNative: js.Any = makeNativeArray(v)
    def fromNative(ps: ParseState): Vector[A] =
      jsToCollection(ps, Vector.newBuilder)

  given BufferConv[A: NativeConverter]: NativeConverter[Buffer[A]] with
    extension (b: Buffer[A]) def toNative: js.Any = makeNativeArray(b)
    def fromNative(ps: ParseState): mutable.Buffer[A] =
      jsToCollection(ps, ArrayBuffer.newBuilder)

  private def asJSDict(ps: ParseState): js.Dictionary[js.Any] =
    ps.json match
      case o: js.Object => o.asInstanceOf[js.Dictionary[js.Any]]
      case _            => ps.fail("js.Object")

  private def jsToCollection[A, C[_, _]](
      ps: ParseState,
      dict: js.Dictionary[js.Any],
      resBuilder: Builder[(String, A), C[String, A]]
  )(using nc: NativeConverter[A]): C[String, A] =
    resBuilder.sizeHint(dict.size)
    val it = dict.iterator

    while it.hasNext do
      val (k, v) = it.next()

      val key: String = k.asInstanceOf[Any] match
        case s: String => s
        case _         => return ps.fail("all String keys")

      resBuilder += k -> nc.fromNative(ps.atKey(key, v))

    resBuilder.result()

  given MapConv[A: NativeConverter]: NativeConverter[Map[String, A]] with
    extension (m: Map[String, A])
      def toNative: js.Any =
        val res = js.Object().asInstanceOf[js.Dynamic]
        for (k, v) <- m do res.updateDynamic(k)(v.toNative)
        res

    def fromNative(ps: ParseState): Map[String, A] =
      val jsDict = asJSDict(ps)
      jsToCollection(ps, jsDict, HashMap.newBuilder)

  given ImmutableMapConv[A: NativeConverter]: NativeConverter[immutable.Map[String, A]] with
    extension (m: immutable.Map[String, A])
      def toNative: js.Any =
        val res = js.Object().asInstanceOf[js.Dynamic]
        for (k, v) <- m do res.updateDynamic(k)(v.toNative)
        res

    def fromNative(ps: ParseState): Predef.Map[String, A] =
      val jsDict = asJSDict(ps)
      jsToCollection(ps, jsDict, immutable.Map.newBuilder)

  given OptionCodec[A: NativeConverter]: NativeConverter[Option[A]] with
    extension (o: Option[A])
      def toNative: js.Any =
        o.map(_.toNative).getOrElse(null.asInstanceOf[js.Any])

    def fromNative(ps: ParseState): Option[A] =
      ps.json match
        case null => None
        case _    => Some(NativeConverter[A].fromNative(ps))

  /** Derive a NativeConverter for type T. This method is called by the compiler automatically when adding `derives NativeConverter` on a class. You can also use it to derive given instances anywhere, which is useful if Cross-Building a Scala.js project: <br> `given NativeConverter[User] = NativeConverter.derived` <br> Only Sum and Product types are supported
    */
  inline given derived[A](using m: Mirror.Of[A]): NativeConverter[A] =
    type Mets = m.MirroredElemTypes
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    inline m match
      case p: Mirror.ProductOf[A] =>
        new NativeConverter[A]:
          extension (a: A)
            def toNative: js.Any =
              productToNative[Mets, Mels](a.asInstanceOf[Product])
          def fromNative(ps: ParseState): A =
            val jsDict = asJSDict(ps)
            val resArr = Array.ofDim[Any](constValue[Tuple.Size[Mets]])
            nativeToProduct[A, Mets, Mels](p, resArr, ps, jsDict)

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
        res.updateDynamic(constValue[mel & String])(
          p.productElement(i).asInstanceOf[js.Any]
        )
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
  ): A =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        mirror.fromProduct(ArrayProduct(resArr))

      case _: (met *: metsTail, mel *: melsTail) =>
        val nc = summonInline[NativeConverter[met]]
        val key = constValue[mel & String]
        val elementJs = jsDict.getOrElse(key, null)

        resArr(i) = nc.fromNative(ps.atKey(key, elementJs))
        nativeToProduct[A, metsTail, melsTail](mirror, resArr, ps, jsDict, i + 1)

  private inline def sumConverter[A, Mets](
      m: Mirror.SumOf[A]
  ): NativeConverter[A] =
    inline erasedValue[Mets] match
      case _: (met *: metsTail) =>
        inline if isSingleton[met] then sumConverter[A, metsTail](m)
        else adtSumConverter(m)

      case _: EmptyTuple => simpleSumConverter(m)

  /** A singleton is a Product with no parameter elements
    */
  private inline def isSingleton[T]: Boolean = summonFrom[T] {
    case product: Mirror.ProductOf[T] =>
      inline erasedValue[product.MirroredElemTypes] match
        case _: EmptyTuple => true
        case _             => false
    case _ => false
  }

  private inline def simpleSumConverter[A](
      m: Mirror.SumOf[A]
  ): NativeConverter[A] =
    type Mets = m.MirroredElemTypes
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any = simpleSumToNative[Mels](m.ordinal(a))
      def fromNative(ps: ParseState): A =
        val name = ps.json.asInstanceOf[Any] match
          case s: String => s
          case _         => ps.fail("String")
        simpleSumFromNative[A, Label, Mets, Mels](ps, name)

  private inline def simpleSumToNative[Mels](n: Int, i: Int = 0): js.Any =
    inline erasedValue[Mels] match
      case _: EmptyTuple => // can never reach
      case _: (mel *: melsTail) =>
        if i == n then constValue[mel].asInstanceOf[js.Any]
        else simpleSumToNative[melsTail](n, i + 1)

  private inline def simpleSumFromNative[A, Label, Mets, Mels](
      ps: ParseState,
      name: String
  ): A =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        ps.fail(s"a member of Sum type ${constValue[Label]}")
      case _: (met *: metsTail, mel *: melsTail) =>
        if constValue[mel] == name then summonInline[Mirror.ProductOf[met & A]].fromProduct(EmptyTuple)
        else simpleSumFromNative[A, Label, metsTail, melsTail](ps, name)

  private inline def adtSumConverter[A](
      m: Mirror.SumOf[A]
  ): NativeConverter[A] =
    type Mets = m.MirroredElemTypes
    type Mels = m.MirroredElemLabels
    type Label = m.MirroredLabel

    new NativeConverter[A]:
      extension (a: A) def toNative: js.Any = adtSumToNative[A, Mets, Mels](a, m.ordinal(a))
      def fromNative(ps: ParseState): A =
        val jsDict = asJSDict(ps)
        val typeName = jsDict.getOrElse("@type", null).asInstanceOf[Any] match
          case s: String => s
          case _         => ps.fail("js.Object with existing '@type' key/value")
        adtSumFromNative[A, Label, Mets, Mels](ps, typeName)

  private inline def adtSumToNative[A, Mets, Mels](
      a: A,
      ordinal: Int,
      i: Int = 0
  ): js.Any =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) => // can never reach
      case _: (met *: metsTail, mel *: melsTail) =>
        if i == ordinal then
          val res = summonInline[NativeConverter[met]]
            .asInstanceOf[NativeConverter[A]]
            .toNative(a)
          res.asInstanceOf[js.Dynamic].`@type` = constValue[mel].asInstanceOf[js.Any]
          res
        else adtSumToNative[A, metsTail, melsTail](a, ordinal, i + 1)

  private inline def adtSumFromNative[A, Label, Mets, Mels](
      ps: ParseState,
      typeName: String
  ): A =
    inline (erasedValue[Mets], erasedValue[Mels]) match
      case _: (EmptyTuple, EmptyTuple) =>
        ps.fail(s"a valid '@type' for ${constValue[Label]}")
      case _: (met *: metsTail, mel *: melsTail) =>
        if constValue[mel] == typeName then
          summonInline[NativeConverter[met]]
            .asInstanceOf[NativeConverter[A]]
            .fromNative(ps)
        else adtSumFromNative[A, Label, metsTail, melsTail](ps, typeName)
