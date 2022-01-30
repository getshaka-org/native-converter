package org.getshaka.nativeconverter

import scala.scalajs.js
import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.scalajs.js.{JSON, WrappedMap}

/**
 * Converters for native JS types not supported by JSON.
 */
object EsConverters:
  
  given ESMapConv[K: NativeConverter, V: NativeConverter]: NativeConverter[Map[K, V]] with
    extension (t: Map[K, V]) def toNative: js.Any =
      val res = js.Map[js.Any, js.Any]()
      for (k, v) <- t do
        res(k.toNative) = v.toNative
      res

    def fromNativeE(ps: ParseState): Either[String, Map[K, V]] =
      for
        jsMap <-
          ps.json match
            case m: js.Map[?, ?] => Right(m.asInstanceOf[js.Map[js.Any, js.Any]])
            case _ => ps.left("js.Map")
        m <- jsMapToMap[K, V](ps, jsMap)
      yield m

  private def jsMapToMap[K: NativeConverter, V: NativeConverter](
    ps: ParseState,
    jsMap: js.Map[js.Any, js.Any]
  ): Either[String, Map[K, V]] =
    val res = HashMap.empty[K, V]
    res.sizeHint(jsMap.size)
    val it = jsMap.iterator

    while it.hasNext do
      val (k, v) = it.next()
      val kString = JSON.stringify(k)

      val key: K = NativeConverter[K].fromNativeE(ps.atKey(kString, k)) match
        case Right(x) => x
        case l: Left[?, ?] => return l.asInstanceOf[Left[String, Map[K, V]]]

      val value: V = NativeConverter[V].fromNativeE(ps.atKey(kString, v)) match
        case Right(x) => x
        case l: Left[?, ?] => return l.asInstanceOf[Left[String, Map[K, V]]]

      res(key) = value
    Right(res)


// todo more

  
