package org.getshaka.nativeconverter

import scala.scalajs.js
import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.scalajs.js.{JSON, WrappedMap}

/** Converters for native JS types not supported by JSON.
  */
object EsConverters:

  given ESMapConv[K: NativeConverter, V: NativeConverter]: NativeConverter[Map[K, V]] with
    extension (t: Map[K, V])
      def toNative: js.Any =
        val res = js.Map[js.Any, js.Any]()
        for (k, v) <- t do res(k.toNative) = v.toNative
        res

    def fromNative(ps: ParseState): Map[K, V] =
      val jsMap = ps.json match
        case m: js.Map[?, ?] => m.asInstanceOf[js.Map[js.Any, js.Any]]
        case _               => ps.fail("js.Map")
      jsMapToMap[K, V](ps, jsMap)

  private def jsMapToMap[K: NativeConverter, V: NativeConverter](
      ps: ParseState,
      jsMap: js.Map[js.Any, js.Any]
  ): Map[K, V] =
    val res = HashMap.empty[K, V]
    res.sizeHint(jsMap.size)
    val it = jsMap.iterator

    while it.hasNext do
      val (k, v) = it.next()
      val kString = JSON.stringify(k)

      val key: K = NativeConverter[K].fromNative(ps.atKey(kString, k))
      val value: V = NativeConverter[V].fromNative(ps.atKey(kString, v))
      res(key) = value

    res

// todo more
