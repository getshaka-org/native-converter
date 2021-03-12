package org.getshaka.nativeconverter

import scala.scalajs.js
import scala.collection.Map
import scala.collection.mutable.HashMap
import scala.scalajs.js.WrappedMap

/**
 * Converters for native JS types not supported by JSON.
 */
object EsConverters:
  
  given [K, V](using kConv: NativeConverter[K], vConv: NativeConverter[V]): NativeConverter[Map[K, V]] with
    extension (t: Map[K, V]) def toNative: js.Any =
      val res = js.Map[js.Any, js.Any]()
      for (k, v) <- t do
        res(kConv.toNative(k)) = vConv.toNative(v)
      res
  
    def fromNative(nativeJs: js.Any): Map[K, V] =
      val jsMap = nativeJs.asInstanceOf[js.Map[js.Any, js.Any]]
      val res = HashMap[K, V]()
      for (k, v) <- jsMap do
        res(kConv.fromNative(k)) = vConv.fromNative(v)
      res

  // todo more

  
