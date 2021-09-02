package org.getshaka.nativeconverter

import scala.scalajs.js

extension (nativeJs: js.Any)
  def fromNative[A](using nc: NativeConverter[A]): A = nc.fromNative(nativeJs)

extension (json: String)
  def fromJson[A](using nc: NativeConverter[A]): A = nc.fromJson(json)
